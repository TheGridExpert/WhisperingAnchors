package net.grid.whisperinganchors.client.particle;

import net.grid.whisperinganchors.client.WhisperingAnchorsClient;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.model.data.ModelData;

public class AnchorCrumbleParticle extends TerrainParticle {

    private AnchorCrumbleParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, ModBlocks.WHISPERING_ANCHOR.get().defaultBlockState());
        setSprite(Minecraft.getInstance().getModelManager().getModel(WhisperingAnchorsClient.ANCHOR_BODY_MODEL).getParticleIcon(ModelData.EMPTY));
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new AnchorCrumbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
