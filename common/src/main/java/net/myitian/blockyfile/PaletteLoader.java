package net.myitian.blockyfile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.myitian.blockyfile.config.Config;

import java.util.HashSet;

public final class PaletteLoader {
    public static final Object LOCK = new Object();
    private static final Component NOT_COMPLETE_ADDING = Component.translatable("commands.blockyfile.not_complete_adding");
    private static final HashSet<ResourceLocation> ids = new HashSet<>(256);
    private static long size = 0;
    private static Component lastError = Component.literal("not loaded");

    public static Component getLastError() {
        return lastError;
    }

    public static void prepare() {
        lastError = NOT_COMPLETE_ADDING;
        size = 0;
        ids.clear();
        BlockyFile.index2block.clear();
        BlockyFile.block2index.clear();
        BlockyFile.block2id.clear();
        if (Config.isDebug())
            BlockyFile.LOGGER.info("[L] prepare");
    }

    public static boolean append(String s) {
        if (lastError != NOT_COMPLETE_ADDING) {
            return true;
        } else if (size >= 256) {
            size++;
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(s);
        if (id == null) {
            lastError = BlockyFile.translatable_INVALID_IDENTIFIER(s);
            return true;
        } else if (!ids.add(id)) {
            lastError = BlockyFile.translatable_DUPLICATE_IDENTIFIER(id);
            return true;
        }
        Block b = BuiltInRegistries.BLOCK.getValue(id);
        if (b == Blocks.AIR) {
            lastError = BlockyFile.translatable_BLOCK_IS_AIR_OR_NOT_EXIST(id);
            return true;
        }
        String sid = id.toString();
        BlockyFile.block2index.put(b, BlockyFile.index2block.size());
        BlockyFile.block2id.put(b, sid);
        BlockyFile.index2block.add(b.defaultBlockState());
        size++;
        return false;
    }

    public static void complete() {
        if (lastError == NOT_COMPLETE_ADDING) {
            if (BlockyFile.validatePaletteSize(size)) {
                lastError = null;
            } else {
                lastError = BlockyFile.translatable_INVALID_PALETTE_SIZE(size);
            }
        }

        if (lastError != null) {
            BlockyFile.index2block.clear();
            BlockyFile.block2index.clear();
            BlockyFile.block2id.clear();
            if (Config.isDebug()) {
                BlockyFile.LOGGER.info("[L] complete (failed: {})", lastError);
            }
        } else if (Config.isDebug()) {
            BlockyFile.LOGGER.info("[L] complete (succeed)");
        }
    }
}
