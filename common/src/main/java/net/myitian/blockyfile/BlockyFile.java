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
    public static final boolean CLOTH_CONFIG_EXISTED = isClothConfigExisted();
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

    static final byte[] byteValues = {
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
    static final ArrayList<BlockState> index2block = new ArrayList<>();
    static final Object2IntOpenHashMap<Block> block2index = new Object2IntOpenHashMap<>();
    static final Object2ObjectOpenHashMap<Block, String> block2id = new Object2ObjectOpenHashMap<>();
    static Component lastError = Component.literal("not loaded");

    public static void init() {
        block2index.defaultReturnValue(-1);
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

    public static Component getLastError() {
        return lastError;
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

    public static BlockyFileWriter<BlockState> tryCreateWriter() {
        return getLastError() != null ? null : new BlockyFileWriter<>(index2block);
    }

    public static BlockyFileReader<Block> tryCreateReader() {
        return getLastError() != null ? null : new BlockyFileReader<>(block2index);
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
            LOGGER.info("Save file by command...");
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
            LOGGER.info("Save file by accessing local ServerLevel...");
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
        if (localServer != null) {
            Level level = localServer.getLevel(player.level().dimension());
            if (level != null) {
                return level;
            }
        }
        return player.level();
    }

    public static Component exceptionAsComponent(Throwable e) {
        return Component
            .literal(e.getClass().getName())
            .append(": ")
            .append(e.getLocalizedMessage());
    }
}