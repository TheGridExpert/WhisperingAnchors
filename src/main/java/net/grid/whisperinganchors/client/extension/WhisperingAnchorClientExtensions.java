package net.grid.whisperinganchors.client.extension;

import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.core.ModParticles;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public class WhisperingAnchorClientExtensions implements IClientBlockExtensions {

    private static final int CRUMBS_PER_HIT = 2;
    private static final float CRUMB_POWER = 0.2F;
    private static final float CRUMB_SCALE = 0.6F;

    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (target instanceof BlockHitResult hit) {
            BlockPos top = WhisperingAnchorBlock.getTopPos(hit.getBlockPos(), state);
            if (ModConfig.areAnchorsStatic()) {
                spawnCrumbParticles(AnchorEntity.getRestingBody(top), level.getRandom(), manager);
            } else {
                for (AnchorEntity anchor : AnchorEntity.findBoundTo(level, top)) {
                    spawnCrumbParticles(anchor.getBoundingBox(), anchor.getRandom(), manager);
                }
            }
        }

        return false;
    }

    private static void spawnCrumbParticles(AABB body, RandomSource random, ParticleEngine manager) {
        for (int crumb = 0; crumb < CRUMBS_PER_HIT; crumb++) {
            Vec3 point = AnchorEntity.pickRandomPointIn(body, random);
            Particle particle = manager.createParticle(ModParticles.ANCHOR_CRUMBLE.get(), point.x, point.y, point.z, 0.0, 0.0, 0.0);
            if (particle != null) {
                particle.setPower(CRUMB_POWER).scale(CRUMB_SCALE);
            }
        }
    }
}
