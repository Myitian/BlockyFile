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
    private static final byte[] byteValues = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
        0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,
        0xFFFFFF80, 0xFFFFFF81, 0xFFFFFF82, 0xFFFFFF83, 0xFFFFFF84, 0xFFFFFF85, 0xFFFFFF86, 0xFFFFFF87, 0xFFFFFF88, 0xFFFFFF89, 0xFFFFFF8A, 0xFFFFFF8B, 0xFFFFFF8C, 0xFFFFFF8D, 0xFFFFFF8E, 0xFFFFFF8F,
        0xFFFFFF90, 0xFFFFFF91, 0xFFFFFF92, 0xFFFFFF93, 0xFFFFFF94, 0xFFFFFF95, 0xFFFFFF96, 0xFFFFFF97, 0xFFFFFF98, 0xFFFFFF99, 0xFFFFFF9A, 0xFFFFFF9B, 0xFFFFFF9C, 0xFFFFFF9D, 0xFFFFFF9E, 0xFFFFFF9F,
        0xFFFFFFA0, 0xFFFFFFA1, 0xFFFFFFA2, 0xFFFFFFA3, 0xFFFFFFA4, 0xFFFFFFA5, 0xFFFFFFA6, 0xFFFFFFA7, 0xFFFFFFA8, 0xFFFFFFA9, 0xFFFFFFAA, 0xFFFFFFAB, 0xFFFFFFAC, 0xFFFFFFAD, 0xFFFFFFAE, 0xFFFFFFAF,
        0xFFFFFFB0, 0xFFFFFFB1, 0xFFFFFFB2, 0xFFFFFFB3, 0xFFFFFFB4, 0xFFFFFFB5, 0xFFFFFFB6, 0xFFFFFFB7, 0xFFFFFFB8, 0xFFFFFFB9, 0xFFFFFFBA, 0xFFFFFFBB, 0xFFFFFFBC, 0xFFFFFFBD, 0xFFFFFFBE, 0xFFFFFFBF,
        0xFFFFFFC0, 0xFFFFFFC1, 0xFFFFFFC2, 0xFFFFFFC3, 0xFFFFFFC4, 0xFFFFFFC5, 0xFFFFFFC6, 0xFFFFFFC7, 0xFFFFFFC8, 0xFFFFFFC9, 0xFFFFFFCA, 0xFFFFFFCB, 0xFFFFFFCC, 0xFFFFFFCD, 0xFFFFFFCE, 0xFFFFFFCF,
        0xFFFFFFD0, 0xFFFFFFD1, 0xFFFFFFD2, 0xFFFFFFD3, 0xFFFFFFD4, 0xFFFFFFD5, 0xFFFFFFD6, 0xFFFFFFD7, 0xFFFFFFD8, 0xFFFFFFD9, 0xFFFFFFDA, 0xFFFFFFDB, 0xFFFFFFDC, 0xFFFFFFDD, 0xFFFFFFDE, 0xFFFFFFDF,
        0xFFFFFFE0, 0xFFFFFFE1, 0xFFFFFFE2, 0xFFFFFFE3, 0xFFFFFFE4, 0xFFFFFFE5, 0xFFFFFFE6, 0xFFFFFFE7, 0xFFFFFFE8, 0xFFFFFFE9, 0xFFFFFFEA, 0xFFFFFFEB, 0xFFFFFFEC, 0xFFFFFFED, 0xFFFFFFEE, 0xFFFFFFEF,
        0xFFFFFFF0, 0xFFFFFFF1, 0xFFFFFFF2, 0xFFFFFFF3, 0xFFFFFFF4, 0xFFFFFFF5, 0xFFFFFFF6, 0xFFFFFFF7, 0xFFFFFFF8, 0xFFFFFFF9, 0xFFFFFFFA, 0xFFFFFFFB, 0xFFFFFFFC, 0xFFFFFFFD, 0xFFFFFFFE, 0xFFFFFFFF
    };
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
        BlockyFile.saveConfig();
        feedback.sendFeedback(BlockyFile.translatable_PALETTE_IMPORT_SUCCEED());
    }

    public static void throwIfError() throws CommandSyntaxException {
        Component lastError = PaletteLoader.getLastError();
        if (lastError != null) {
            throw new SimpleCommandExceptionType(lastError).create();
        }
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
                throwIfError();
                BlockyFileWriter<BlockState> writer = BlockyFile.createWriter();
                BlockyFile.storeFile(pos1, pos2, axisOrder, file, feedback, player, writer, null, null);
            }
            case LOAD -> {
                throwIfError();
                BlockyFileReader<Block> reader = BlockyFile.createReader();
                BlockyFile.loadFile(pos1, pos2, axisOrder, file, feedback, player, reader, null, null);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int subCommandConfigReload(CommandContext<S> context) throws CommandSyntaxException {
        BlockyFile.loadConfig();
        CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
        throwIfError();
        feedback.sendFeedback(BlockyFile.CONFIG_RELOAD_SUCCEED);
        return Command.SINGLE_SUCCESS;
    }

    private int subCommandPaletteClipboard(CommandContext<S> context) throws CommandSyntaxException {
        CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
        switch (context.getArgument("mode", PaletteOperationMode.class)) {
            case IMPORT -> {
                String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                BlockyFile.loadPalette(BlockyFile.NEWLINE_PATTERN.splitAsStream(clipboard)::iterator);
                throwIfError();
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
                throwIfError();
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
                        Component lastError = PaletteLoader.getLastError();
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
                    stream = new ByteArrayInputStream(byteValues, 0, BlockyFile.index2block.size());
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
