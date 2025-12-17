package net.myitian.blockyfile;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.myitian.blockyfile.command.BlockPosArgumentEx;
import net.myitian.blockyfile.command.CustomArgument;
import net.myitian.blockyfile.config.Config;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public record CommandBuilder<S>(
    Literal<S> literal,
    Argument<S> argument,
    Function<S, Level> getWorld,
    Function<S, CommandFeedback> getFeedbackWrapper) {
    private static final Object2IntMap<Block> nonNull = new Object2IntMap<>() {
        @Override
        public int size() {
            return 256;
        }

        @Override
        public void defaultReturnValue(int rv) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int defaultReturnValue() {
            return 0;
        }

        @Override
        public ObjectSet<Entry<Block>> object2IntEntrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ObjectSet<Block> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull IntCollection values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            return key != null;
        }

        @Override
        public boolean containsValue(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInt(Object key) {
            return containsKey(key) ? 1 : 0;
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(@NotNull Map<? extends Block, ? extends Integer> m) {
            throw new UnsupportedOperationException();
        }
    };

    private static void paletteImportSucceed(CommandFeedback feedback) {
        List<String> palette = Config.getPalette();
        palette.clear();
        BlockyFile.index2block.stream()
            .map(BlockState::getBlock)
            .map(BlockyFile.block2id::get)
            .forEachOrdered(palette::add);
        feedback.sendFeedback(BlockyFile.translatable_PALETTE_IMPORT_SUCCEED());
    }

    public <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
        return argument.argument(name, type);
    }

    public LiteralArgumentBuilder<S> literal(String name) {
        return literal.literal(name);
    }

    public LiteralArgumentBuilder<S> build() {
        return literal("blockyfile")
            .then(literal("file")
                .then(argument("mode", new CustomArgument<>(FileOperationMode::parse, FileOperationMode.VALUES))
                    .then(argument("pos1", BlockPosArgumentEx.blockPos())
                        .then(argument("pos2", BlockPosArgumentEx.blockPos())
                            .then(argument("axisOrder", new CustomArgument<>(AxisOrder::parse, AxisOrder.VALUES))
                                .then(argument("filename", StringArgumentType.greedyString())
                                    .executes(this::subCommandFile)))))))
            .then(literal("config")
                .then(literal("reload")
                    .executes(this::subCommandConfigReload)))
            .then(literal("palette")
                .then(argument("mode", new CustomArgument<>(PaletteOperationMode::parse, PaletteOperationMode.VALUES))
                    .then(literal("clipboard")
                        .executes(this::subCommandPaletteClipboard))
                    .then(literal("file")
                        .then(argument("filename", StringArgumentType.greedyString())
                            .executes(this::subCommandPaletteFile)))
                    .then(literal("world")
                        .then(argument("pos1", BlockPosArgumentEx.blockPos())
                            .then(argument("pos2", BlockPosArgumentEx.blockPos())
                                .then(argument("axisOrder", new CustomArgument<>(AxisOrder::parse, AxisOrder.VALUES))
                                    .executes(this::subCommandPaletteWorld)))))));
    }

    private int subCommandFile(CommandContext<S> context) throws CommandSyntaxException {
        S source = context.getSource();
        Level world = getWorld.apply(source);
        BlockPos pos1 = BlockPosArgumentEx.getInWorldBlockPos(context, world, "pos1");
        BlockPos pos2 = BlockPosArgumentEx.getInWorldBlockPos(context, world, "pos2");
        AxisOrder axisOrder = context.getArgument("axisOrder", AxisOrder.class);
        String file = StringArgumentType.getString(context, "filename");
        CommandFeedback feedback = getFeedbackWrapper.apply(source);
        LocalPlayer player = Minecraft.getInstance().player;
        switch (context.getArgument("mode", FileOperationMode.class)) {
            case STORE -> {
                BlockyFileWriter<BlockState> writer = BlockyFile.tryCreateWriter();
                if (writer == null) {
                    throw new SimpleCommandExceptionType(BlockyFile.getLastError()).create();
                }
                BlockyFile.storeFile(pos1, pos2, axisOrder, file, feedback, player, writer, null, null);
            }
            case LOAD -> {
                BlockyFileReader<Block> reader = BlockyFile.tryCreateReader();
                if (reader == null) {
                    throw new SimpleCommandExceptionType(BlockyFile.getLastError()).create();
                }
                BlockyFile.loadFile(pos1, pos2, axisOrder, file, feedback, player, reader, null, null);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int subCommandConfigReload(CommandContext<S> context) throws CommandSyntaxException {
        BlockyFile.loadConfig();
        Component lastError = BlockyFile.getLastError();
        CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
        if (lastError != null) {
            throw new SimpleCommandExceptionType(lastError).create();
        }
        feedback.sendFeedback(BlockyFile.CONFIG_RELOAD_SUCCEED);
        return Command.SINGLE_SUCCESS;
    }

    private int subCommandPaletteClipboard(CommandContext<S> context) throws CommandSyntaxException {
        CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
        switch (context.getArgument("mode", PaletteOperationMode.class)) {
            case IMPORT -> {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                BlockyFile.loadPalette(BlockyFile.NEWLINE_PATTERN.splitAsStream(clipboard)::iterator);
                Component lastError = BlockyFile.getLastError();
                if (lastError != null) {
                    throw new SimpleCommandExceptionType(lastError).create();
                }
                paletteImportSucceed(feedback);
            }
            case EXPORT -> {
                String clipboard = String.join(System.lineSeparator(), Config.getPalette());
                Minecraft.getInstance().keyboardHandler.setClipboard(clipboard);
                feedback.sendFeedback(BlockyFile.translatable_PALETTE_EXPORT_SUCCEED());
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int subCommandPaletteFile(CommandContext<S> context) throws CommandSyntaxException {
        CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
        String file = StringArgumentType.getString(context, "filename");
        Path p = Path.of(file);
        switch (context.getArgument("mode", PaletteOperationMode.class)) {
            case IMPORT -> {
                if (!(Files.exists(p) && Files.isRegularFile(p))) {
                    throw BlockyFile.FILE_NOT_EXISTS_EXCEPTION.create(file);
                }
                try (Stream<String> lines = Files.lines(p)) {
                    BlockyFile.loadPalette(lines::iterator);
                } catch (IOException e) {
                    throw new SimpleCommandExceptionType(BlockyFile.exceptionAsComponent(e)).create();
                }
                Component lastError = BlockyFile.getLastError();
                if (lastError != null) {
                    throw new SimpleCommandExceptionType(lastError).create();
                }
                paletteImportSucceed(feedback);
            }
            case EXPORT -> {
                if (Files.exists(p)) {
                    throw BlockyFile.FILE_EXISTS_EXCEPTION.create(file);
                }
                try {
                    Files.write(p, Config.getPalette());
                } catch (IOException e) {
                    throw new SimpleCommandExceptionType(BlockyFile.exceptionAsComponent(e)).create();
                }
                feedback.sendFeedback(BlockyFile.translatable_PALETTE_EXPORT_SUCCEED());
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int subCommandPaletteWorld(CommandContext<S> context) throws CommandSyntaxException {
        S source = context.getSource();
        Level world = getWorld.apply(source);
        BlockPos pos1 = BlockPosArgumentEx.getInWorldBlockPos(context, world, "pos1");
        BlockPos pos2 = BlockPosArgumentEx.getInWorldBlockPos(context, world, "pos2");
        AxisOrder axisOrder = context.getArgument("axisOrder", AxisOrder.class);
        CommandFeedback feedback = getFeedbackWrapper.apply(source);
        LocalPlayer player = Minecraft.getInstance().player;
        switch (context.getArgument("mode", PaletteOperationMode.class)) {
            case IMPORT -> {
                // enable 8-bit read mode
                BlockyFileReader<Block> reader = new BlockyFileReader<>(nonNull);
                OutputStream stream = OutputStream.nullOutputStream();
                BlockyFile.loadStream(pos1, pos2, axisOrder, stream, feedback, player, reader,
                    null, PaletteLoader::prepare,
                    (feedback1, error) -> Minecraft.getInstance().execute(() -> {
                        PaletteLoader.complete();
                        if (error != null) {
                            feedback1.sendError(error);
                        }
                        Component lastError = BlockyFile.getLastError();
                        if (lastError != null) {
                            feedback.sendError(lastError);
                        } else if (error == null) {
                            paletteImportSucceed(feedback1);
                        }
                    }),
                    b -> {
                        if (Blocks.AIR.equals(b)) {
                            return true;
                        } else {
                            String s = BuiltInRegistries.BLOCK.getKey(b).toString();
                            return PaletteLoader.append(s);
                        }
                    });
            }
            case EXPORT -> {
                BlockyFileWriter<BlockState> writer;
                ByteArrayInputStream stream;
                synchronized (PaletteLoader.LOCK) {
                    // enable 8-bit write mode
                    BlockState[] temp = BlockyFile.index2block.toArray(new BlockState[256]);
                    writer = new BlockyFileWriter<>(Arrays.asList(temp));
                    // ensure that null values are not accessed
                    stream = new ByteArrayInputStream(BlockyFile.byteValues, 0, BlockyFile.index2block.size());
                }
                BlockyFile.storeStream(pos1, pos2, axisOrder, stream, feedback, player, writer,
                    null, null,
                    (feedback1, error) -> Minecraft.getInstance().execute(() -> {
                        if (error != null) {
                            feedback1.sendError(error);
                        } else {
                            feedback1.sendFeedback(BlockyFile.translatable_PALETTE_EXPORT_SUCCEED());
                        }
                    }));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    @FunctionalInterface
    public interface Argument<S> {
        <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type);
    }

    @FunctionalInterface
    public interface Literal<S> {
        LiteralArgumentBuilder<S> literal(String name);
    }
}
