package net.myitian.blockyfile;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BlockyFileWriter<T> extends BlockyFileHandler<InputStream, BlockyFileWriter.Consumer<T>> {
    private final List<T> index2unit;
    private boolean eof = false;

    public BlockyFileWriter(List<T> index2unit) {
        super(BlockyFile.getPaletteBitCount(index2unit.size()));
        this.index2unit = index2unit;
        if (debug) {
            BlockyFile.LOGGER.info("[W] <init>, size = {}, bitPerUnit = {}",
                index2unit.size(), bitPerUnit);
        }
    }

    public void validate(String path, BlockPos pos1, BlockPos pos2) throws CommandSyntaxException {
        if (bitPerUnit == 0)
            throw BlockyFile.INVALID_PALETTE_SIZE_EXCEPTION.create(index2unit.size());
        Path p = Path.of(path);
        if (!(Files.exists(p) && Files.isRegularFile(p)))
            throw BlockyFile.FILE_NOT_EXISTS_EXCEPTION.create(path);
        long xDiff = Math.abs((long) pos1.getX() - (long) pos2.getX()) + 1;
        long yDiff = Math.abs((long) pos1.getY() - (long) pos2.getY()) + 1;
        long zDiff = Math.abs((long) pos1.getZ() - (long) pos2.getZ()) + 1;
        long maxSize = xDiff * yDiff * zDiff * bitPerUnit / 8;
        try {
            long realSize = Files.size(p);
            if (realSize > maxSize) {
                throw BlockyFile.FILE_TOO_LARGE_EXCEPTION.create(realSize, maxSize);
            }
        } catch (IOException e) {
            throw new SimpleCommandExceptionType(BlockyFile.exceptionAsComponent(e)).create();
        }
    }

    /**
     * @return true if reached the end of stream or an error occurred
     */
    @Override
    protected boolean next(
        InputStream stream,
        int x, int y, int z,
        Consumer<T> consumer) throws IOException {
        if (bitPerUnit == 8) {
            int read = stream.read();
            if (read < 0) {
                return true;
            }
            byteCount++;
            T unit = index2unit.get(read);
            if (consumer.apply(unit, x, y, z)) {
                return true;
            }
            unitCount++;
        } else {
            if (eof) { // Already EOF
                if (length <= 0) {
                    return true; // No more data
                }
            } else if (length < bitPerUnit) {
                int read = stream.read();
                if (read >= 0) { // Normal
                    buffer |= read << (8 - length);
                    length += 8;
                    byteCount++;
                } else { // Reached EOF
                    eof = true;
                    if (length <= 0) {
                        return true; // No more data
                    }
                }
            }
            int offset = 16 - bitPerUnit;
            int mask = ((1 << bitPerUnit) - 1) << offset;
            T unit = index2unit.get((buffer & mask) >> offset);
            if (consumer.apply(unit, x, y, z)) {
                return true; // Failed to execute
            }
            if (debug) {
                int masked = buffer & mask;
                int value = masked >> offset;
                BlockyFile.LOGGER.info("[W] pos = ({}, {}, {}), buffer = 0b_{}, masked = 0b_{}, value = 0b_{}",
                    x, y, z,
                    toGroupedBinaryString(buffer),
                    toGroupedBinaryString(masked),
                    toGroupedBinaryString(value));
            }
            unitCount++;
            buffer <<= bitPerUnit;
            length -= bitPerUnit;
        }
        return false;
    }

    @FunctionalInterface
    public interface Consumer<T> {
        /**
         * @return true if failed
         */
        boolean apply(T unit, int x, int y, int z);
    }
}
