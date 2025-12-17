package net.myitian.blockyfile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.myitian.blockyfile.config.Config;

import java.io.IOException;

public abstract class BlockyFileHandler<STREAM extends AutoCloseable, ARG> {
    protected final boolean debug = Config.isDebug();
    protected final int bitPerBlock;
    protected long blockCounter = 0;
    protected long byteCounter = 0;
    protected int buffer = 0;
    protected int length = 0;

    protected BlockyFileHandler(int bitPerBlock) {
        this.bitPerBlock = bitPerBlock;
    }

    protected static String toGroupedBinaryString(int value) {
        char[] buffer = {
            (char) (((value >> 0x1F) & 1) + '0'),
            (char) (((value >> 0x1E) & 1) + '0'),
            (char) (((value >> 0x1D) & 1) + '0'),
            (char) (((value >> 0x1C) & 1) + '0'),
            (char) (((value >> 0x1B) & 1) + '0'),
            (char) (((value >> 0x1A) & 1) + '0'),
            (char) (((value >> 0x19) & 1) + '0'),
            (char) (((value >> 0x18) & 1) + '0'),
            '_',
            (char) (((value >> 0x17) & 1) + '0'),
            (char) (((value >> 0x16) & 1) + '0'),
            (char) (((value >> 0x15) & 1) + '0'),
            (char) (((value >> 0x14) & 1) + '0'),
            (char) (((value >> 0x13) & 1) + '0'),
            (char) (((value >> 0x12) & 1) + '0'),
            (char) (((value >> 0x11) & 1) + '0'),
            (char) (((value >> 0x10) & 1) + '0'),
            '_',
            (char) (((value >> 0x0F) & 1) + '0'),
            (char) (((value >> 0x0E) & 1) + '0'),
            (char) (((value >> 0x0D) & 1) + '0'),
            (char) (((value >> 0x0C) & 1) + '0'),
            (char) (((value >> 0x0B) & 1) + '0'),
            (char) (((value >> 0x0A) & 1) + '0'),
            (char) (((value >> 0x09) & 1) + '0'),
            (char) (((value >> 0x08) & 1) + '0'),
            '_',
            (char) (((value >> 0x07) & 1) + '0'),
            (char) (((value >> 0x06) & 1) + '0'),
            (char) (((value >> 0x05) & 1) + '0'),
            (char) (((value >> 0x04) & 1) + '0'),
            (char) (((value >> 0x03) & 1) + '0'),
            (char) (((value >> 0x02) & 1) + '0'),
            (char) (((value >> 0x01) & 1) + '0'),
            (char) ((value & 1) + '0'),
        };
        return new String(buffer);
    }

    public long getBlockCounter() {
        return blockCounter;
    }

    public long getByteCounter() {
        return byteCounter;
    }

    public abstract STREAM createStream(String path) throws Exception;

    public Component execute(
        String path,
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        ARG consumer) {
        try (STREAM stream = createStream(path)) {
            int x1 = pos1.getX();
            int y1 = pos1.getY();
            int z1 = pos1.getZ();
            int x2 = pos2.getX();
            int y2 = pos2.getY();
            int z2 = pos2.getZ();
            int xDirection = x2 > x1 ? 1 : -1;
            int yDirection = y2 > y1 ? 1 : -1;
            int zDirection = z2 > z1 ? 1 : -1;
            x2 += xDirection;
            y2 += yDirection;
            z2 += zDirection;
            LOOP:
            switch (axisOrder) {
                case XYZ -> {
                    for (int z = z1; z != z2; z += zDirection)
                        for (int y = y1; y != y2; y += yDirection)
                            for (int x = x1; x != x2; x += xDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case XZY -> {
                    for (int y = y1; y != y2; y += yDirection)
                        for (int z = z1; z != z2; z += zDirection)
                            for (int x = x1; x != x2; x += xDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case YXZ -> {
                    for (int z = z1; z != z2; z += zDirection)
                        for (int x = x1; x != x2; x += xDirection)
                            for (int y = y1; y != y2; y += yDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case YZX -> {
                    for (int x = x1; x != x2; x += xDirection)
                        for (int z = z1; z != z2; z += zDirection)
                            for (int y = y1; y != y2; y += yDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case ZXY -> {
                    for (int y = y1; y != y2; y += yDirection)
                        for (int x = x1; x != x2; x += xDirection)
                            for (int z = z1; z != z2; z += zDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case ZYX -> {
                    for (int x = x1; x != x2; x += xDirection)
                        for (int y = y1; y != y2; y += yDirection)
                            for (int z = z1; z != z2; z += zDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                default -> throw new AssertionError();
            }
            return null;
        } catch (Exception e) {
            return BlockyFile.exceptionAsComponent(e);
        }
    }

    protected abstract boolean next(
        STREAM stream,
        int x, int y, int z,
        ARG consumer) throws IOException;
}
