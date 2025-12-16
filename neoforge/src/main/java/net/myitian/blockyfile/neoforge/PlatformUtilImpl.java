package net.myitian.blockyfile.neoforge;

import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class PlatformUtilImpl {
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static <S> BlockPos getBlockPos(Coordinates coordinates, S source) {
        return null;
    }
}