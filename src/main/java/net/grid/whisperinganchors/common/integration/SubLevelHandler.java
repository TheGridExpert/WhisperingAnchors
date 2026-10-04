package net.grid.whisperinganchors.common.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.joml.Quaternionf;

import java.util.ServiceLoader;

public interface SubLevelHandler {

    SubLevelHandler NONE = new SubLevelHandler() {};
    SubLevelHandler INSTANCE = load();

    default Vec3 toWorldPosition(Level level, Vec3 pos) {
        return pos;
    }

    default Vec3 toLocalPosition(Level level, Vec3 frame, Vec3 pos) {
        return pos;
    }

    default Vec3 toWorldDirection(Level level, Vec3 frame, Vec3 direction) {
        return direction;
    }

    default Vec3 toLocalDirection(Level level, Vec3 frame, Vec3 direction) {
        return direction;
    }

    default Vec3 getAngularVelocity(Level level, Vec3 frame) {
        return Vec3.ZERO;
    }

    default Vec3 getVelocity(Level level, Vec3 pos) {
        return Vec3.ZERO;
    }

    default boolean isInSameSubLevel(Level level, Vec3 a, Vec3 b) {
        return true;
    }

    default boolean isInSubLevel(Level level, Vec3 pos) {
        return false;
    }

    default boolean isInUnloadedSubLevel(Level level, BlockPos pos) {
        return false;
    }

    default Quaternionf getRenderOrientation(BlockPos pos, float partialTick) {
        return new Quaternionf();
    }

    private static SubLevelHandler load() {
        if (!ModList.get().isLoaded("sable")) {
            return NONE;
        }

        return ServiceLoader.load(SubLevelHandler.class, SubLevelHandler.class.getClassLoader()).findFirst().orElse(NONE);
    }
}
