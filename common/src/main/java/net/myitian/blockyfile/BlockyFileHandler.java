package net.myitian.blockyfile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public abstract class BlockyFileHandler<STREAM extends AutoCloseable, ARG> {
    protected final int bitPerBlock;
    protected long blockCounter = 0;
    protected long byteCounter = 0;
    protected int buffer = 0;
    protected int length = 0;

    protected BlockyFileHandler(int bitPerBlock) {
        this.bitPerBlock = bitPerBlock;
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
                case xyz -> {
                    for (int z = z1; z != z2; z += zDirection)
                        for (int y = y1; y != y2; y += yDirection)
                            for (int x = x1; x != x2; x += xDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case xzy -> {
                    for (int y = y1; y != y2; y += yDirection)
                        for (int z = z1; z != z2; z += zDirection)
                            for (int x = x1; x != x2; x += xDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case yxz -> {
                    for (int z = z1; z != z2; z += zDirection)
                        for (int x = x1; x != x2; x += xDirection)
                            for (int y = y1; y != y2; y += yDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case yzx -> {
                    for (int x = x1; x != x2; x += xDirection)
                        for (int z = z1; z != z2; z += zDirection)
                            for (int y = y1; y != y2; y += yDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case zxy -> {
                    for (int y = y1; y != y2; y += yDirection)
                        for (int x = x1; x != x2; x += xDirection)
                            for (int z = z1; z != z2; z += zDirection)
                                if (next(stream, x, y, z, consumer))
                                    break LOOP;
                }
                case zyx -> {
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
