package net.grid.whisperinganchors.client.particle;

import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.particle.HarkeningSoulParticleOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class HarkeningSoulParticle extends TextureSheetParticle {

    private static final float TIGHTENING = 0.55F;
    private static final float SPIN_UP = 1.0F;
    private static final double BASE_RISE = 0.035;
    private static final double RISE_SPREAD = 0.02;
    private static final double WOBBLE = 0.08;
    private static final float WOBBLE_RATE = 0.4F;
    private static final float FADE_FROM = 0.7F;
    private static final float DARK_SHADE = 0.7F;

    private final SpriteSet sprites;
    private final double centreX;
    private final double centreZ;
    private final double baseY;
    private final double radius;
    private final float spin;
    private final double rise;
    private final float phase;
    private float angle;
    private double height;

    private HarkeningSoulParticle(ClientLevel level, double x, double y, double z, HarkeningSoulParticleOption options, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.centreX = x;
        this.centreZ = z;
        this.baseY = y;
        this.radius = options.radius() * Mth.randomBetween(this.random, 0.8F, 1.2F);
        this.spin = options.spin() * Mth.randomBetween(this.random, 0.8F, 1.2F);
        this.rise = BASE_RISE + this.random.nextDouble() * RISE_SPREAD;
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.angle = this.random.nextFloat() * Mth.TWO_PI;
        this.lifetime = 36 + this.random.nextInt(16);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        scale(1.2F);
        if (this.random.nextFloat() < 0.33) {
            setColor(DARK_SHADE, DARK_SHADE, DARK_SHADE);
        }

        moveAlongSpiral();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }

        float progress = (float) this.age / this.lifetime;
        this.angle += this.spin * (1.0F + SPIN_UP * progress);
        this.height += this.rise * (0.5 + 1.5 * progress);
        moveAlongSpiral();

        this.alpha = progress > FADE_FROM ? (1.0F - progress) / (1.0F - FADE_FROM) : 1.0F;
        setSpriteFromAge(this.sprites);
    }

    private void moveAlongSpiral() {
        float progress = (float) this.age / this.lifetime;
        double currentRadius = this.radius * (1.0F - TIGHTENING * progress) + WOBBLE * Mth.sin(this.age * WOBBLE_RATE + this.phase);
        Vec3 local = new Vec3(this.centreX + currentRadius * Mth.cos(this.angle), this.baseY + this.height, this.centreZ + currentRadius * Mth.sin(this.angle));
        Vec3 world = SubLevelHandler.INSTANCE.toWorldPosition(this.level, local);
        setPos(world.x, world.y, world.z);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<HarkeningSoulParticleOption> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(HarkeningSoulParticleOption options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new HarkeningSoulParticle(level, x, y, z, options, this.sprites);
        }
    }
}
