package net.myitian.blockyfile.fabric;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;

public final class PlatformUtilImpl {
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static <S> BlockPos getBlockPos(Coordinates coordinates, S source) {
        if (source instanceof FabricClientCommandSource s) {
            if (coordinates instanceof WorldCoordinates wc) {
                Vec3 vec3 = s.getPosition();
                return BlockPos.containing(wc.x.get(vec3.x), wc.y.get(vec3.y), wc.z.get(vec3.z));
            } else if (coordinates instanceof LocalCoordinates lc) {
                Vec2 vec2 = s.getRotation();
                Vec3 vec3 = EntityAnchorArgument.Anchor.FEET.apply(s.getEntity());
                float a = Mth.cos((vec2.y + 90) * Mth.DEG_TO_RAD);
                float b = Mth.sin((vec2.y + 90) * Mth.DEG_TO_RAD);
                float c = Mth.cos(-vec2.x * Mth.DEG_TO_RAD);
                float d = Mth.sin(-vec2.x * Mth.DEG_TO_RAD);
                float e = Mth.cos((-vec2.x + 90) * Mth.DEG_TO_RAD);
                float f = Mth.sin((-vec2.x + 90) * Mth.DEG_TO_RAD);
                Vec3 vec3a = new Vec3(a * c, d, b * c);
                Vec3 vec3b = new Vec3(a * e, f, b * e);
                Vec3 vec3c = vec3a.cross(vec3b).scale(-1.0F);
                double xo = vec3a.x * lc.forwards + vec3b.x * lc.up + vec3c.x * lc.left;
                double yo = vec3a.y * lc.forwards + vec3b.y * lc.up + vec3c.y * lc.left;
                double zo = vec3a.z * lc.forwards + vec3b.z * lc.up + vec3c.z * lc.left;
                return BlockPos.containing(vec3.x + xo, vec3.y + yo, vec3.z + zo);
            }
        }
        return null;
    }
}