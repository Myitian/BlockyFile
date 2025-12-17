package net.myitian.blockyfile;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BlockyFileReader extends BlockyFileHandler<OutputStream, BlockyFileReader.Supplier> {
    private final AbstractObject2IntMap<Block> block2index;

    public BlockyFileReader(AbstractObject2IntMap<Block> block2index) {
        super(BlockyFile.getPaletteBitCount(block2index.size()));
        this.block2index = block2index;
    }

    public void validate(String path) throws CommandSyntaxException {
        if (bitPerBlock == 0)
            throw BlockyFile.INVALID_PALETTE_SIZE_EXCEPTION.create(block2index.size());
        Path p = Path.of(path);
        if (Files.exists(p))
            throw BlockyFile.FILE_EXISTS_EXCEPTION.create(path);
    }

    @Override
    public OutputStream createStream(String path) throws Exception {
        return new FileOutputStream(path);
    }

    /**
     * @return true if found an invalid block
     */
    @Override
    protected boolean next(
        OutputStream stream,
        int x, int y, int z,
        Supplier supplier) throws IOException {
        Block block = supplier.apply(x, y, z);
        int read = block2index.getInt(block);
        if (read == -1)
            return true;
        if (bitPerBlock == 8) {
            stream.write(read);
            byteCounter++;
        } else {
            int mask = (1 << bitPerBlock) - 1;
            buffer |= (read & mask) << (16 - length - bitPerBlock);
            length += bitPerBlock;
            if (length >= 8) {
                stream.write((buffer & 0xFF00) >> 8);
                if (debug) {
                    int masked = buffer & 0xFF00;
                    int value = masked >> 8;
                    BlockyFile.LOGGER.info("[R] pos = ({}, {}, {}), buffer = 0b{}, masked = 0b{}, value = 0b{}",
                        x, y, z,
                        StringUtils.leftPad(Integer.toBinaryString(buffer), 32, '0'),
                        StringUtils.leftPad(Integer.toBinaryString(masked), 32, '0'),
                        StringUtils.leftPad(Integer.toBinaryString(value), 32, '0'));
                }
                byteCounter++;
                buffer <<= 8;
                length -= 8;
            }
        }
        blockCounter++;
        return false;
    }

    @FunctionalInterface
    public interface Supplier {
        Block apply(int x, int y, int z);
    }
}
