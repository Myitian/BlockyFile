package net.myitian.blockyfile;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BlockyFileWriter extends BlockyFileHandler<InputStream, BlockyFileWriter.Consumer> {
    private final List<BlockState> index2block;
    private boolean eof = false;

    public BlockyFileWriter(List<BlockState> index2block) {
        super(BlockyFile.getPaletteBitCount(index2block.size()));
        this.index2block = index2block;
    }

    public void validate(String path, BlockPos pos1, BlockPos pos2) throws CommandSyntaxException {
        if (bitPerBlock == 0)
            throw BlockyFile.INVALID_PALETTE_SIZE_EXCEPTION.create(index2block.size());
        Path p = Path.of(path);
        if (!(Files.exists(p) && Files.isRegularFile(p)))
            throw BlockyFile.FILE_NOT_EXISTS_EXCEPTION.create(path);
        long xDiff = Math.abs((long) pos1.getX() - (long) pos2.getX()) + 1;
        long yDiff = Math.abs((long) pos1.getY() - (long) pos2.getY()) + 1;
        long zDiff = Math.abs((long) pos1.getZ() - (long) pos2.getZ()) + 1;
        long maxSize = xDiff * yDiff * zDiff * bitPerBlock / 8;
        try {
            long realSize = Files.size(p);
            if (realSize > maxSize)
                throw BlockyFile.FILE_TOO_LARGE_EXCEPTION.create(realSize, maxSize);
        } catch (IOException e) {
            throw new SimpleCommandExceptionType(BlockyFile.exceptionAsComponent(e)).create();
        }
    }

    @Override
    public InputStream createStream(String path) throws FileNotFoundException {
        return new FileInputStream(path);
    }

    /**
     * @return true if reached the end of stream or an error occurred
     */
    @Override
    protected boolean next(
        InputStream stream,
        int x, int y, int z,
        Consumer consumer) throws IOException {
        if (bitPerBlock == 8) {
            int read = stream.read();
            if (read < 0)
                return true;
            byteCounter++;
            BlockState blockState = index2block.get(read);
            if (!consumer.apply(blockState, x, y, z))
                return true;
            blockCounter++;
        } else {
            if (eof) { // Already EOF
                if (length <= 0)
                    return true; // No more data
            } else if (length < bitPerBlock) {
                int read = stream.read();
                if (read >= 0) { // Normal
                    buffer |= read << (8 - length);
                    length += 8;
                    byteCounter++;
                } else { // Reached EOF
                    eof = true;
                    if (length <= 0)
                        return true; // No more data
                }
            }
            int offset = 16 - bitPerBlock;
            int mask = ((1 << bitPerBlock) - 1) << offset;
            BlockState blockState = index2block.get((buffer & mask) >> offset);
            if (consumer.apply(blockState, x, y, z))
                return true; // Failed to execute
            if (debug) {
                int masked = buffer & mask;
                int value = masked >> offset;
                BlockyFile.LOGGER.info("[W] pos = ({}, {}, {}), buffer = 0b_{}, masked = 0b_{}, value = 0b_{}",
                    x, y, z,
                    toGroupedBinaryString(buffer),
                    toGroupedBinaryString(masked),
                    toGroupedBinaryString(value));
            }
            blockCounter++;
            buffer <<= bitPerBlock;
            length -= bitPerBlock;
        }
        return false;
    }

    @FunctionalInterface
    public interface Consumer {
        /**
         * @return true if failed
         */
        boolean apply(BlockState blockState, int x, int y, int z);
    }
}
