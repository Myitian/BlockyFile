package net.myitian.blockyfile;

import com.mojang.brigadier.Command;
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
import net.myitian.blockyfile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BlockyFile {
    public static final String MOD_ID = "blockyfile";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Path CONFIG_PATH = PlatformUtil.getConfigDirectory().resolve(MOD_ID + ".json");
    public static final boolean CLOTH_CONFIG_EXISTED = isClothConfigExisted();
    public static final DynamicCommandExceptionType INVALID_PALETTE_SIZE_EXCEPTION = new DynamicCommandExceptionType(size -> Component.translatable("commands.blockyfile.invalid_palette_size", size));
    public static final DynamicCommandExceptionType FILE_NOT_EXISTS_EXCEPTION = new DynamicCommandExceptionType(path -> Component.translatableEscape("argument.blockyfile.file.not_exists", path));
    public static final DynamicCommandExceptionType FILE_EXISTS_EXCEPTION = new DynamicCommandExceptionType(path -> Component.translatableEscape("argument.blockyfile.file.exists", path));
    public static final Dynamic2CommandExceptionType FILE_TOO_LARGE_EXCEPTION = new Dynamic2CommandExceptionType((realSize, maxSize) -> Component.translatableEscape("argument.blockyfile.file.not_exists", realSize, maxSize));

    private static final ArrayList<BlockState> index2block = new ArrayList<>();
    private static final Object2IntOpenHashMap<Block> block2index = new Object2IntOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<Block, String> block2id = new Object2ObjectOpenHashMap<>();
    private static Component lastError = Component.literal("unloaded");

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

    public static Component translatable_DUPLICATED_IDENTIFIER(ResourceLocation id) {
        return Component.translatable("commands.blockyfile.duplicated_identifier", id);
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
                return translatable_DUPLICATED_IDENTIFIER(id);
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

    public static int executeCommand(
        OperationMode mode,
        BlockPos pos1,
        BlockPos pos2,
        AxisOrder axisOrder,
        String file,
        CommandFeedback feedback,
        LocalPlayer player) throws CommandSyntaxException {
        return switch (mode) {
            case store -> storeFile(pos1, pos2, axisOrder, file, feedback, player);
            case load -> loadFile(pos1, pos2, axisOrder, file, feedback, player);
        };
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
                        Thread.sleep(Config.getCommandDelay());
                        return true;
                    } catch (InterruptedException ex) {
                        return false;
                    }
                }));
        } else {
            ServerLevel level = localServer.getLevel(player.clientLevel.dimension());
            LOGGER.info("Save file by accessing local ServerLevel...");
            if (level != null) {
                localServer.execute(new BlockyFileRunnable<>(
                    writer, file, pos1, pos2, axisOrder, feedback,
                    "commands.blockyfile.store.succeed",
                    (b, x, y, z) -> {
                        BlockPos pos = new BlockPos(x, y, z);
                        level.setBlock(pos, b, 2, 0);
                        return true;
                    }));
            }
        }
        return Command.SINGLE_SUCCESS;
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

    private static Level getLevel(LocalPlayer player, IntegratedServer localServer) {
        if (localServer != null) {
            Level level = localServer.getLevel(player.clientLevel.dimension());
            if (level != null)
                return level;
        }
        return player.clientLevel;
    }

    public static Component exceptionAsComponent(Exception e) {
        return Component
            .literal(e.getClass().getName())
            .append(": ")
            .append(e.getLocalizedMessage());
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
                    long blocks = reader.getBlockCounter();
                    long bytes = reader.getByteCounter();
                    feedback.sendFeedback(Component.translatable(succeedText, bytes, blocks));
                }
            } catch (Exception e) {
                feedback.sendError(exceptionAsComponent(e));
            }
        }
    }
}