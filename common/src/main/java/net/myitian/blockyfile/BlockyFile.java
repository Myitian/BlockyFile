package net.myitian.blockyfile;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.myitian.blockyfile.command.BlockPosArgumentEx;
import net.myitian.blockyfile.command.CustomArgument;
import net.myitian.blockyfile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class BlockyFile {
    public static final String MOD_ID = "blockyfile";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path CONFIG_PATH = PlatformUtil.getConfigDirectory().resolve(MOD_ID + ".json");
    public static final boolean CLOTH_CONFIG_EXISTED = isClothConfigExisted();
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("[\\r\\n]");
    public static final Component CONFIG_RELOAD_SUCCEED = Component.translatable("commands.blockyfile.config.reload.succeed");
    public static final Component PALETTE_IMPORT_SUCCEED = Component.translatable("commands.blockyfile.palette.import.succeed");
    public static final Component PALETTE_EXPORT_SUCCEED = Component.translatable("commands.blockyfile.palette.export.succeed");
    public static final DynamicCommandExceptionType INVALID_PALETTE_SIZE_EXCEPTION = new DynamicCommandExceptionType(
        size -> Component.translatable("commands.blockyfile.invalid_palette_size", size));
    public static final DynamicCommandExceptionType FILE_NOT_EXISTS_EXCEPTION = new DynamicCommandExceptionType(
        path -> Component.translatable("argument.blockyfile.file.not_exists", path));
    public static final DynamicCommandExceptionType FILE_EXISTS_EXCEPTION = new DynamicCommandExceptionType(
        path -> Component.translatable("argument.blockyfile.file.exists", path));
    public static final Dynamic2CommandExceptionType FILE_TOO_LARGE_EXCEPTION = new Dynamic2CommandExceptionType(
        (realSize, maxSize) -> Component.translatable("argument.blockyfile.file.too_large", realSize, maxSize));

    private static final ArrayList<BlockState> index2block = new ArrayList<>();
    private static final Object2IntOpenHashMap<Block> block2index = new Object2IntOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<Block, String> block2id = new Object2ObjectOpenHashMap<>();
    private static Component lastError = Component.literal("not loaded");

    public static void init() {
        block2index.defaultReturnValue(-1);
        File configFile = CONFIG_PATH.toFile();
        if (!Config.load(configFile)) {
            Config.save(configFile);
        }
        loadPalette();
    }

    public static Component getLastError() {
        return lastError;
    }

    public static synchronized void loadPalette() {
        if ((lastError = loadPalettePrivate()) != null) {
            index2block.clear();
            block2index.clear();
            block2id.clear();
        }
    }

    public static Component translatable_INVALID_PALETTE_SIZE(int size) {
        return Component.translatable("commands.blockyfile.invalid_palette_size", size);
    }

    public static Component translatable_INVALID_IDENTIFIER(String id) {
        return Component.translatable("commands.blockyfile.invalid_identifier", id);
    }

    public static Component translatable_DUPLICATE_IDENTIFIER(ResourceLocation id) {
        return Component.translatable("commands.blockyfile.duplicate_identifier", id);
    }

    public static Component translatable_BLOCK_IS_AIR_OR_NOT_EXIST(ResourceLocation id) {
        return Component.translatable("commands.blockyfile.block_is_air_or_not_exist", id);
    }

    private static Component loadPalettePrivate() {
        List<String> palette = Config.getPalette();
        index2block.clear();
        block2index.clear();
        block2id.clear();
        int size = palette.size();
        if (!validatePaletteSize(size)) {
            return translatable_INVALID_PALETTE_SIZE(size);
        }
        HashSet<ResourceLocation> ids = new HashSet<>(size);
        index2block.ensureCapacity(size);
        block2index.ensureCapacity(size);
        block2id.ensureCapacity(size);
        for (String s : palette) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id == null) {
                return translatable_INVALID_IDENTIFIER(s);
            } else if (!ids.add(id)) {
                return translatable_DUPLICATE_IDENTIFIER(id);
            }
            Block b = BuiltInRegistries.BLOCK.getValue(id);
            if (b == Blocks.AIR) {
                return translatable_BLOCK_IS_AIR_OR_NOT_EXIST(id);
            }
            block2index.put(b, index2block.size());
            block2id.put(b, id.toString());
            index2block.add(b.defaultBlockState());
        }
        return null;
    }

    public static boolean validatePaletteSize(int n) {
        return n >= 2 && n <= 256 && (n & (n - 1)) == 0;
    }

    public static int getPaletteBitCount(int n) {
        return validatePaletteSize(n) ? Integer.numberOfTrailingZeros(n) : 0;
    }

    public static boolean isClothConfigExisted() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static BlockyFileWriter tryCreateWriter() {
        return getLastError() != null ? null : new BlockyFileWriter(index2block);
    }

    public static BlockyFileReader tryCreateReader() {
        return getLastError() != null ? null : new BlockyFileReader(block2index);
    }

    public static <S> LiteralArgumentBuilder<S> buildCommandArguments(
        Literal<S> literal,
        Argument<S> argument,
        Function<S, Level> getWorld,
        Function<S, CommandFeedback> getFeedbackWrapper) {
        return literal.literal("blockyfile")
            .then(literal.literal("file")
                .then(argument.argument("mode", new CustomArgument<>(FileOperationMode::parse, FileOperationMode.VALUES))
                    .then(argument.argument("pos1", BlockPosArgumentEx.blockPos())
                        .then(argument.argument("pos2", BlockPosArgumentEx.blockPos())
                            .then(argument.argument("axisOrder", new CustomArgument<>(AxisOrder::parse, AxisOrder.VALUES))
                                .then(argument.argument("filename", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        S source = context.getSource();
                                        Level world = getWorld.apply(source);
                                        return executeCommand(
                                            context.getArgument("mode", FileOperationMode.class),
                                            BlockPosArgumentEx.getInWorldBlockPos(context, world, "pos1"),
                                            BlockPosArgumentEx.getInWorldBlockPos(context, world, "pos2"),
                                            context.getArgument("axisOrder", AxisOrder.class),
                                            StringArgumentType.getString(context, "filename"),
                                            getFeedbackWrapper.apply(source),
                                            Minecraft.getInstance().player);
                                    })))))))
            .then(literal.literal("config")
                .then(literal.literal("reload")
                    .executes(context -> {
                        File configFile = CONFIG_PATH.toFile();
                        Config.load(configFile);
                        loadPalette();
                        Component lastError = getLastError();
                        CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
                        if (lastError != null)
                            throw new SimpleCommandExceptionType(lastError).create();
                        feedback.sendFeedback(CONFIG_RELOAD_SUCCEED);
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(literal.literal("palette")
                .then(argument.argument("mode", new CustomArgument<>(PaletteOperationMode::parse, PaletteOperationMode.VALUES))
                    .then(literal.literal("clipboard")
                        .executes(context -> {
                            CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
                            switch (context.getArgument("mode", PaletteOperationMode.class)) {
                                case IMPORT -> {
                                    String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                                    importPalette(NEWLINE_PATTERN.splitAsStream(clipboard));
                                    Component lastError = getLastError();
                                    if (lastError != null)
                                        throw new SimpleCommandExceptionType(lastError).create();
                                    feedback.sendFeedback(PALETTE_IMPORT_SUCCEED);
                                }
                                case EXPORT -> {
                                    String clipboard = String.join(System.lineSeparator(), Config.getPalette());
                                    Minecraft.getInstance().keyboardHandler.setClipboard(clipboard);
                                    feedback.sendFeedback(PALETTE_EXPORT_SUCCEED);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                    .then(literal.literal("file")
                        .then(argument.argument("filename", StringArgumentType.greedyString())
                            .executes(context -> {
                                CommandFeedback feedback = getFeedbackWrapper.apply(context.getSource());
                                String file = StringArgumentType.getString(context, "filename");
                                Path p = Path.of(file);
                                switch (context.getArgument("mode", PaletteOperationMode.class)) {
                                    case IMPORT -> {
                                        if (!(Files.exists(p) && Files.isRegularFile(p)))
                                            throw FILE_NOT_EXISTS_EXCEPTION.create(file);
                                        try {
                                            importPalette(Files.lines(p));
                                        } catch (IOException e) {
                                            throw new SimpleCommandExceptionType(exceptionAsComponent(e)).create();
                                        }
                                        Component lastError = getLastError();
                                        if (lastError != null)
                                            throw new SimpleCommandExceptionType(lastError).create();
                                        feedback.sendFeedback(PALETTE_IMPORT_SUCCEED);
                                    }
                                    case EXPORT -> {
                                        if (Files.exists(p))
                                            throw FILE_EXISTS_EXCEPTION.create(file);
                                        try {
                                            Files.write(p, Config.getPalette());
                                        } catch (IOException e) {
                                            throw new SimpleCommandExceptionType(exceptionAsComponent(e)).create();
                                        }
                                        feedback.sendFeedback(PALETTE_EXPORT_SUCCEED);
                                    }
                                }
                                return Command.SINGLE_SUCCESS;
                            })))));
    }

    public static void importPalette(Stream<String> stream) {
        List<String> palette = Config.getPalette();
        palette.clear();
        stream.map(String::trim)
            .filter(it -> !it.isEmpty())
            .forEachOrdered(palette::add);
        stream.close();
        File configFile = CONFIG_PATH.toFile();
        Config.save(configFile);
        loadPalette();
    }

    public static int executeCommand(
        FileOperationMode mode,
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        String file,
        CommandFeedback feedback,
        LocalPlayer player) throws CommandSyntaxException {
        return switch (mode) {
            case STORE -> storeFile(pos1, pos2, axisOrder, file, feedback, player);
            case LOAD -> loadFile(pos1, pos2, axisOrder, file, feedback, player);
        };
    }

    public static int loadFile(BlockPos pos1, BlockPos pos2, AxisOrder axisOrder, String file, CommandFeedback feedback, LocalPlayer player) throws CommandSyntaxException {
        BlockyFileReader reader = tryCreateReader();
        if (reader == null)
            throw new SimpleCommandExceptionType(getLastError()).create();
        reader.validate(file);
        IntegratedServer localServer = Minecraft.getInstance().getSingleplayerServer();
        Level level = getLevel(player, localServer);
        BlockyFileRunnable<BlockyFileReader.Supplier> runnable = new BlockyFileRunnable<>(
            reader, file, pos1, pos2, axisOrder, feedback,
            "commands.blockyfile.load.succeed",
            (x, y, z) -> {
                BlockPos pos = new BlockPos(x, y, z);
                return level.getBlockState(pos).getBlock();
            });
        if (localServer == null) {
            LOGGER.info("Load file from ClientLevel...");
            CompletableFuture.runAsync(runnable);
        } else {
            LOGGER.info("Load file from ServerLevel...");
            localServer.execute(runnable);
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int storeFile(BlockPos pos1, BlockPos pos2, AxisOrder axisOrder, String file, CommandFeedback feedback, LocalPlayer player) throws CommandSyntaxException {
        BlockyFileWriter writer = tryCreateWriter();
        if (writer == null)
            throw new SimpleCommandExceptionType(getLastError()).create();
        writer.validate(file, pos1, pos2);
        IntegratedServer localServer = Minecraft.getInstance().getSingleplayerServer();
        if (localServer == null || Config.isForceCommand()) {
            ClientPacketListener connection = player.connection;
            LOGGER.info("Save file by command...");
            CompletableFuture.runAsync(new BlockyFileRunnable<>(
                writer, file, pos1, pos2, axisOrder, feedback,
                "commands.blockyfile.store.succeed",
                (b, x, y, z) -> {
                    try {
                        connection.sendCommand(Config.getCommand()
                            .formatted(x, y, z, block2id.get(b.getBlock())));
                        Thread.sleep(Config.getCommandInterval());
                        return false;
                    } catch (InterruptedException ex) {
                        return true;
                    }
                }));
        } else {
            ServerLevel level = localServer.getLevel(player.level().dimension());
            LOGGER.info("Save file by accessing local ServerLevel...");
            if (level != null) {
                localServer.execute(new BlockyFileRunnable<>(
                    writer, file, pos1, pos2, axisOrder, feedback,
                    "commands.blockyfile.store.succeed",
                    (b, x, y, z) -> {
                        BlockPos pos = new BlockPos(x, y, z);
                        level.setBlock(pos, b, 2, 0);
                        return false;
                    }));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static Level getLevel(LocalPlayer player, IntegratedServer localServer) {
        if (localServer != null) {
            Level level = localServer.getLevel(player.level().dimension());
            if (level != null)
                return level;
        }
        return player.level();
    }

    public static Component exceptionAsComponent(Exception e) {
        return Component
            .literal(e.getClass().getName())
            .append(": ")
            .append(e.getLocalizedMessage());
    }

    @FunctionalInterface
    public interface Argument<S> {
        <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type);
    }

    @FunctionalInterface
    public interface Literal<S> {
        LiteralArgumentBuilder<S> literal(String name);
    }

    private record BlockyFileRunnable<ARG>(
        BlockyFileHandler<?, ARG> reader,
        String file,
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        CommandFeedback feedback,
        String succeedText,
        ARG arg) implements Runnable {

        @Override
        public void run() {
            try {
                Component error = reader.execute(file, pos1, pos2, axisOrder, arg);
                if (error != null) {
                    feedback.sendError(error);
                } else {
                    Minecraft.getInstance().execute(this::succeed);
                }
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> feedback.sendError(exceptionAsComponent(e)));
            }
        }

        private void succeed() {
            long blocks = reader.getBlockCounter();
            long bytes = reader.getByteCounter();
            feedback.sendFeedback(Component.translatable(succeedText, bytes, blocks));
        }
    }
}