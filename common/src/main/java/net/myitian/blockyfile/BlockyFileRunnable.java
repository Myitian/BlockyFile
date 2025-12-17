package net.myitian.blockyfile;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public record BlockyFileRunnable<STREAM extends AutoCloseable, ARG>(
    BlockyFileHandler<STREAM, ARG> reader,
    STREAM stream,
    BlockPos pos1,
    BlockPos pos2,
    AxisOrder axisOrder,
    CommandFeedback feedback,
    String succeedText,
    ARG arg,
    Runnable prepare,
    BiConsumer<CommandFeedback, Component> complete) implements Runnable {
    public BlockyFileRunnable(
        BlockyFileHandler<STREAM, ARG> reader,
        STREAM stream,
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        CommandFeedback feedback,
        String succeedText,
        ARG arg,
        Runnable prepare,
        BiConsumer<CommandFeedback, Component> complete) {
        this.reader = reader;
        this.stream = stream;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.axisOrder = axisOrder;
        this.feedback = feedback;
        this.succeedText = succeedText;
        this.arg = arg;
        this.prepare = prepare;
        this.complete = complete == null ? BlockyFileRunnable::defaultComplete : complete;
    }

    public static void defaultComplete(CommandFeedback feedback, Component error) {
        if (error != null) {
            Minecraft.getInstance().execute(() -> feedback.sendError(error));
        }
    }

    @Override
    public void run() {
        synchronized (PaletteLoader.LOCK) {
            Component error = null;
            try {
                if (prepare != null)
                    prepare.run();
                error = reader.execute(stream, pos1, pos2, axisOrder, arg);
                if (error == null && succeedText != null) {
                    Minecraft.getInstance().execute(() -> {
                        long blocks = reader.getUnitCount();
                        long bytes = reader.getByteCount();
                        feedback.sendFeedback(Component.translatable(succeedText, bytes, blocks));
                    });
                }
            } catch (Exception e) {
                error = BlockyFile.exceptionAsComponent(e);
            } finally {
                try {
                    stream.close();
                } catch (Exception e) {
                    Minecraft.getInstance().execute(() -> feedback.sendError(BlockyFile.exceptionAsComponent(e)));
                }
                complete.accept(feedback, error);
            }
        }
    }

    @Override
    public String toString() {
        return "BlockyFileRunnable[" +
            "reader=" + reader + ", " +
            "stream=" + stream + ", " +
            "pos1=" + pos1 + ", " +
            "pos2=" + pos2 + ", " +
            "axisOrder=" + axisOrder + ", " +
            "feedback=" + feedback + ", " +
            "succeedText=" + succeedText + ", " +
            "arg=" + arg + ", " +
            "prepare=" + prepare + ", " +
            "complete=" + complete + ']';
    }

}
