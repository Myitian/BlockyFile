package net.myitian.blockyfile;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.myitian.blockyfile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class BlockyFile {
    public static final String MOD_ID = "blockyfile";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path CONFIG_PATH = PlatformUtil.getConfigDirectory().resolve(MOD_ID + ".json");
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("[\\r\\n]");
    public static final Component CONFIG_RELOAD_SUCCEED = Component.translatable("commands.blockyfile.config.reload.succeed");
    public static final DynamicCommandExceptionType INVALID_PALETTE_SIZE_EXCEPTION = new DynamicCommandExceptionType(
        size -> Component.translatable("commands.blockyfile.invalid_palette_size", size));
    public static final DynamicCommandExceptionType FILE_NOT_EXISTS_EXCEPTION = new DynamicCommandExceptionType(
        path -> Component.translatable("argument.blockyfile.file.not_exists", path));
    public static final DynamicCommandExceptionType FILE_EXISTS_EXCEPTION = new DynamicCommandExceptionType(
        path -> Component.translatable("argument.blockyfile.file.exists", path));
    public static final Dynamic2CommandExceptionType FILE_TOO_LARGE_EXCEPTION = new Dynamic2CommandExceptionType(
        (realSize, maxSize) -> Component.translatable("argument.blockyfile.file.too_large", realSize, maxSize));
    public static final boolean CLOTH_CONFIG_EXISTED = isClothConfigExisted();

    static final ArrayList<BlockState> index2block = new ArrayList<>();
    static final Object2IntOpenHashMap<Block> block2index = new Object2IntOpenHashMap<>();
    static final Object2ObjectOpenHashMap<Block, String> block2id = new Object2ObjectOpenHashMap<>();

    static {
        block2index.defaultReturnValue(-1);
    }

    public static void init() {
        LOGGER.info("BlockyFile init");
        loadConfig();
    }

    public static void saveConfig() {
        File configFile = CONFIG_PATH.toFile();
        Config.save(configFile);
        loadPalette(Config.getPalette());
    }

    public static void loadConfig() {
        File configFile = CONFIG_PATH.toFile();
        if (!Config.load(configFile)) {
            Config.save(configFile);
        }
        loadPalette(Config.getPalette());
    }

    public static void loadPalette(Iterable<String> palette) {
        synchronized (PaletteLoader.LOCK) {
            try {
                PaletteLoader.prepare();
                for (String s : palette) {
                    if (PaletteLoader.append(s)) {
                        break;
                    }
                }
            } finally {
                PaletteLoader.complete();
            }
        }
    }

    public static Component translatable_PALETTE_IMPORT_SUCCEED() {
        return Component.translatable("commands.blockyfile.palette.import.succeed", Config.getPalette().size());
    }

    public static Component translatable_PALETTE_EXPORT_SUCCEED() {
        return Component.translatable("commands.blockyfile.palette.export.succeed", Config.getPalette().size());
    }

    public static Component translatable_INVALID_PALETTE_SIZE(long size) {
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

    public static boolean validatePaletteSize(long n) {
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

    public static BlockyFileWriter<BlockState> createWriter() {
        return new BlockyFileWriter<>(index2block);
    }

    public static BlockyFileReader<Block> createReader() {
        return new BlockyFileReader<>(block2index);
    }

    public static void storeFile(
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        String file,
        CommandFeedback feedback,
        LocalPlayer player,
        BlockyFileWriter<BlockState> writer,
        Runnable prepare,
        BiConsumer<CommandFeedback, Component> complete) throws CommandSyntaxException {
        writer.validate(file, pos1, pos2);
        try {
            FileInputStream stream = new FileInputStream(file);
            storeStream(pos1, pos2, axisOrder, stream, feedback, player, writer,
                "commands.blockyfile.store.succeed", prepare, complete);
        } catch (FileNotFoundException e) {
            throw FILE_NOT_EXISTS_EXCEPTION.create(file);
        }
    }

    public static void storeStream(
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        InputStream stream,
        CommandFeedback feedback,
        LocalPlayer player,
        BlockyFileWriter<BlockState> writer,
        String succeedText,
        Runnable prepare,
        BiConsumer<CommandFeedback, Component> complete) {
        IntegratedServer localServer = Minecraft.getInstance().getSingleplayerServer();
        if (localServer == null || Config.isForceCommand()) {
            ClientPacketListener connection = player.connection;
            LOGGER.info("Store file by command...");
            CompletableFuture.runAsync(new BlockyFileRunnable<>(
                writer, stream, pos1, pos2, axisOrder, feedback, succeedText,
                (b, x, y, z) -> {
                    try {
                        connection.sendCommand(Config.getCommand()
                            .formatted(x, y, z, block2id.get(b.getBlock())));
                        Thread.sleep(Config.getCommandInterval());
                        return false;
                    } catch (InterruptedException ex) {
                        return true;
                    }
                }, prepare, complete));
        } else {
            ServerLevel level = localServer.getLevel(player.level().dimension());
            LOGGER.info("Store file by accessing local ServerLevel...");
            if (level != null) {
                localServer.execute(new BlockyFileRunnable<>(
                    writer, stream, pos1, pos2, axisOrder, feedback, succeedText,
                    (b, x, y, z) -> {
                        BlockPos pos = new BlockPos(x, y, z);
                        level.setBlock(pos, b, 2, 0);
                        return false;
                    }, prepare, complete));
            }
        }
    }

    public static void loadFile(
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        String file,
        CommandFeedback feedback,
        LocalPlayer player,
        BlockyFileReader<Block> reader,
        Runnable prepare,
        BiConsumer<CommandFeedback, Component> complete) throws CommandSyntaxException {
        reader.validate(file);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            loadStream(pos1, pos2, axisOrder, stream, feedback, player, reader,
                "commands.blockyfile.load.succeed", prepare, complete, b -> false);
        } catch (FileNotFoundException e) {
            throw FILE_NOT_EXISTS_EXCEPTION.create(file);
        }
    }

    public static void loadStream(
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        OutputStream stream,
        CommandFeedback feedback,
        LocalPlayer player,
        BlockyFileReader<Block> reader,
        String succeedText,
        Runnable prepare,
        BiConsumer<CommandFeedback, Component> complete,
        Predicate<Block> predicate) {
        IntegratedServer localServer = Minecraft.getInstance().getSingleplayerServer();
        Level level = getLevel(player, localServer);
        BlockyFileRunnable<OutputStream, BlockyFileReader.Supplier<Block>> runnable = new BlockyFileRunnable<>(
            reader, stream, pos1, pos2, axisOrder, feedback, succeedText,
            (x, y, z) -> {
                BlockPos pos = new BlockPos(x, y, z);
                Block block = level.getBlockState(pos).getBlock();
                return predicate.test(block) ? null : block;
            }, prepare, complete);
        if (localServer == null) {
            LOGGER.info("Load file from ClientLevel...");
            CompletableFuture.runAsync(runnable);
        } else {
            LOGGER.info("Load file from ServerLevel...");
            localServer.execute(runnable);
        }
    }

    private static Level getLevel(LocalPlayer player, IntegratedServer localServer) {
        Level cLevel = player.level();
        if (localServer != null) {
            Level sLevel = localServer.getLevel(cLevel.dimension());
            if (sLevel != null) {
                return sLevel;
            }
        }
        return cLevel;
    }

    public static Component exceptionAsComponent(Throwable e) {
        return Component
            .literal(e.getClass().getName())
            .append(": ")
            .append(e.getLocalizedMessage());
    }
}