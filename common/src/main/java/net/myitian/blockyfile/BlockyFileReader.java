package net.myitian.blockyfile;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BlockyFileReader<T> extends BlockyFileHandler<OutputStream, BlockyFileReader.Supplier<T>> {
    private final Object2IntMap<T> unit2index;

    public BlockyFileReader(Object2IntMap<T> unit2index) {
        super(BlockyFile.getPaletteBitCount(unit2index.size()));
        this.unit2index = unit2index;
        if (debug) {
            BlockyFile.LOGGER.info("[R] <init>, size = {}, bitPerUnit = {}",
                unit2index.size(), bitPerUnit);
        }
    }

    public void validate(String path) throws CommandSyntaxException {
        if (bitPerUnit == 0) {
            throw BlockyFile.INVALID_PALETTE_SIZE_EXCEPTION.create(unit2index.size());
        }
        Path p = Path.of(path);
        if (Files.exists(p)) {
            throw BlockyFile.FILE_EXISTS_EXCEPTION.create(path);
        }
    }

    /**
     * @return true if found an invalid block
     */
    @Override
    protected boolean next(
        OutputStream stream,
        int x, int y, int z,
        Supplier<T> supplier) throws IOException {
        T unit = supplier.apply(x, y, z);
        int read = unit2index.getInt(unit);
        if (read == unit2index.defaultReturnValue()) {
            return true;
        }
        if (bitPerUnit == 8) {
            stream.write(read);
            byteCount++;
        } else {
            int mask = (1 << bitPerUnit) - 1;
            buffer |= (read & mask) << (16 - length - bitPerUnit);
            length += bitPerUnit;
            if (length >= 8) {
                stream.write((buffer & 0xFF00) >> 8);
                if (debug) {
                    int masked = buffer & 0xFF00;
                    int value = masked >> 8;
                    BlockyFile.LOGGER.info("[R] pos = ({}, {}, {}), buffer = 0b_{}, masked = 0b_{}, value = 0b_{}",
                        x, y, z,
                        toGroupedBinaryString(buffer),
                        toGroupedBinaryString(masked),
                        toGroupedBinaryString(value));
                }
                byteCount++;
                buffer <<= 8;
                length -= 8;
            }
        }
        unitCount++;
        return false;
    }

    @FunctionalInterface
    public interface Supplier<T> {
        T apply(int x, int y, int z);
    }
}
