package net.grid.whisperinganchors.common.world.entity;

import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.block.AnchorIdentity;
import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.grid.whisperinganchors.common.core.ModDamageTypes;
import net.grid.whisperinganchors.common.core.ModEntities;
import net.grid.whisperinganchors.common.core.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.UnaryOperator;

public class AnchorEntity extends Entity implements AnchorIdentity {

    public static final String KEY_BOUND_POS = "BoundPos";
    public static final String KEY_COLOR = "Color";
    public static final String KEY_CUSTOM_NAME = "CustomName";
    public static final String KEY_CHAIN_X = "ChainX";
    public static final String KEY_CHAIN_Z = "ChainZ";
    public static final String KEY_TILT_X = "TiltX";
    public static final String KEY_TILT_Z = "TiltZ";
    public static final String KEY_CHAIN_VELOCITY_X = "ChainVelocityX";
    public static final String KEY_CHAIN_VELOCITY_Z = "ChainVelocityZ";
    public static final String KEY_TILT_VELOCITY_X = "TiltVelocityX";
    public static final String KEY_TILT_VELOCITY_Z = "TiltVelocityZ";

    public static final int UNDYED = -1;

    public static final float BODY_WIDTH = 0.99F;
    public static final float BODY_HEIGHT = 2.33F;
    private static final double MODEL_CENTRE_ABOVE_MASS = 0.309;
    public static final float BODY_TOP = (float) MODEL_CENTRE_ABOVE_MASS + BODY_HEIGHT * 0.5F;
    public static final double FIXED_CHAIN_LENGTH = 5 / 16.0;
    public static final double CHAIN_LENGTH = 2 / 16.0;
    public static final double SHACKLE_TO_CENTRE = 1.411;
    public static final double RESTING_DROP = FIXED_CHAIN_LENGTH + CHAIN_LENGTH + SHACKLE_TO_CENTRE;
    private static final double SEARCH_RADIUS = RESTING_DROP + 1.0;

    private static final double GYRATION_RADIUS_SQ = 0.4587;

    private static final double GRAVITY = 0.0245; // Minecraft's default is 0.08
    private static final Vec3 GRAVITY_PULL = new Vec3(0.0, -GRAVITY, 0.0);
    private static final double MAX_GRAVITY_SCALE = 3.0;
    private static final double FRAME_SMOOTHING_TICKS = 4.0;
    private static final double MAX_FRAME_KICK = 1.0;
    private static final double MAX_SWING_RATE = 0.5;

    private static final double CHAIN_RESTORING = GRAVITY / CHAIN_LENGTH;
    private static final double TILT_RESTORING = GRAVITY * SHACKLE_TO_CENTRE / GYRATION_RADIUS_SQ;
    private static final double TILT_TO_CHAIN = SHACKLE_TO_CENTRE / CHAIN_LENGTH;

    private static final double SWING_DRAG = 0.997;
    private static final double SHACKLE_FRICTION = 0.40;
    private static final float MAX_ANGLE = (float) Math.toRadians(70.0);
    private static final double LIMIT_RESTITUTION = 0.2;
    private static final double LIMIT_FRICTION = 0.5;

    private static final double HIT_SPECIFIC_IMPULSE = 0.02;
    private static final double WALK_SPECIFIC_IMPULSE = 0.0005;
    private static final double PROJECTILE_MASS_RATIO = 0.01;

    private static final double IMPACT_RESTITUTION = 0.1;
    private static final double VICTIM_MASS_RATIO = 0.08;
    private static final double HARMLESS_IMPACT_SPEED = 0.02;
    private static final double MAX_SWING_SPEED = 0.200;
    private static final double DAMAGE_SPEED_FRACTION = 0.75;
    private static final float MAX_IMPACT_DAMAGE = 6.0F;

    private static final double COLLISION_RESTITUTION = 0.25;

    private static final double SILENT_IMPACT_SPEED = 0.02;
    private static final int IMPACT_SOUND_COOLDOWN = 5;

    private static final float REST_ANGLE_EPSILON = 0.006F;
    private static final double REST_VELOCITY_EPSILON = 0.002;
    private static final float SYNC_EPSILON = 0.0008F;
    private static final float CLIENT_CORRECTION = 0.35F;

    private static final double TIP_BELOW_SHACKLE = SHACKLE_TO_CENTRE + BODY_HEIGHT * 0.5 - MODEL_CENTRE_ABOVE_MASS;
    private static final double TIP_ARM = CHAIN_LENGTH + TIP_BELOW_SHACKLE;
    private static final double ORBIT_RADIUS = 0.5;
    private static final double ORBIT_NATURAL_RATE = Math.sqrt(GRAVITY * (CHAIN_LENGTH + SHACKLE_TO_CENTRE) / (GYRATION_RADIUS_SQ + (CHAIN_LENGTH + SHACKLE_TO_CENTRE) * (CHAIN_LENGTH + SHACKLE_TO_CENTRE)));
    private static final double ORBIT_SPIN_RATE = ORBIT_NATURAL_RATE * 1.75;
    private static final double ORBIT_RADIAL_STIFFNESS = 0.004;
    private static final double ORBIT_DAMPING = 0.04;
    private static final double ORBIT_RADIAL_CATCH_UP = 0.0004;
    private static final double ORBIT_MAX_CATCH_UP = 0.004;
    private static final float ORBIT_RAMP_UP = 1.0F / 60.0F;
    private static final float ORBIT_RAMP_DOWN = 1.0F / 20.0F;
    private static final int ORBIT_HOLD = 3;
    private static final double ORBIT_SPIN_MOMENTUM = 0.002;
    private static final double ORBIT_RESPONSE_LOSS = 0.8;

    private static final int CRUMBLE_ROWS = 5;
    private static final int CRUMBLE_PARTICLES_PER_ROW = 10;
    private static final double CRUMBLE_SPREAD = 0.25;

    private static final EntityDataAccessor<BlockPos> DATA_BOUND_POS = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> DATA_CHAIN_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHAIN_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TILT_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TILT_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHAIN_VELOCITY_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHAIN_VELOCITY_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TILT_VELOCITY_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TILT_VELOCITY_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ORBIT_DIRECTION = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BYTE);

    private float chainX;
    private float chainZ;
    private float tiltX;
    private float tiltZ;
    private float restX;
    private float restZ;
    private double gravityScale = 1.0;
    private Vec3 localDown = new Vec3(0.0, -1.0, 0.0);
    private @Nullable Vec3 lastFrameVelocity;
    private @Nullable Vec3 lastFrameSpin;
    private Vec3 steadyFrameAcceleration = Vec3.ZERO;
    private float chainXO;
    private float chainZO;
    private float tiltXO;
    private float tiltZO;
    private double chainVelX;
    private double chainVelZ;
    private double tiltVelX;
    private double tiltVelZ;
    private float sentChainX;
    private float sentChainZ;
    private float sentTiltX;
    private float sentTiltZ;
    private int impactSoundCooldown;
    private int orbitTicks;
    private int orbitDirection;
    private float orbitStrength;
    private double orbitCatchUp;

    public AnchorEntity(EntityType<? extends AnchorEntity> type, Level level) {
        super(type, level);
    }

    public @Nullable static AnchorEntity spawnFor(ServerLevel level, BlockPos topPos) {
        AnchorEntity anchor = ModEntities.ANCHOR.get().create(level);
        if (anchor == null) return null;

        anchor.setBoundPos(topPos);
        anchor.setPos(anchor.getCentreOfMass(0.0F, 0.0F, 0.0F, 0.0F));
        level.addFreshEntity(anchor);

        return anchor;
    }

    public static AABB getSearchArea(BlockPos top) {
        return new AABB(top).inflate(SEARCH_RADIUS);
    }

    public static List<AnchorEntity> findBoundTo(Level level, BlockPos top) {
        return level.getEntitiesOfClass(AnchorEntity.class, getSearchArea(top), anchor -> anchor.isBoundTo(top));
    }

    public Vec3 getSuspensionPoint() {
        BlockPos bound = getBoundPos();
        return new Vec3(bound.getX() + 0.5, bound.getY() + 1.0 - FIXED_CHAIN_LENGTH, bound.getZ() + 0.5);
    }

    private static Vec3 calculatePendulumOffset(double length, float angleX, float angleZ) {
        double cosZ = Math.cos(angleZ);
        return new Vec3(length * Math.sin(angleX) * cosZ, -length * Math.cos(angleX) * cosZ, length * Math.sin(angleZ));
    }

    public Vec3 getShacklePos(float chainX, float chainZ) {
        return getSuspensionPoint().add(calculatePendulumOffset(CHAIN_LENGTH, chainX, chainZ));
    }

    public Vec3 getCentreOfMass(float chainX, float chainZ, float tiltX, float tiltZ) {
        return getShacklePos(chainX, chainZ).add(calculatePendulumOffset(SHACKLE_TO_CENTRE, tiltX, tiltZ));
    }

    public static Vec3 getRestingCentreOfMass(BlockPos top) {
        return new Vec3(top.getX() + 0.5, top.getY() + 1.0 - RESTING_DROP, top.getZ() + 0.5);
    }

    public static AABB getRestingBody(BlockPos top) {
        return AABB.ofSize(getRestingCentreOfMass(top).add(0.0, MODEL_CENTRE_ABOVE_MASS, 0.0), BODY_WIDTH, BODY_HEIGHT, BODY_WIDTH);
    }

    public static Vec3 pickRandomPointIn(AABB body, RandomSource random) {
        Vec3 centre = body.getCenter();
        return new Vec3(centre.x + random.nextGaussian() * body.getXsize() * CRUMBLE_SPREAD, Mth.lerp(random.nextDouble(), body.minY, body.maxY), centre.z + random.nextGaussian() * body.getZsize() * CRUMBLE_SPREAD);
    }

    @Override
    protected AABB makeBoundingBox() {
        return AABB.ofSize(position().add(0.0, MODEL_CENTRE_ABOVE_MASS, 0.0), getBbWidth(), getBbHeight(), getBbWidth());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 suspension = getSuspensionPoint();
        return getBoundingBox().minmax(new AABB(suspension, suspension));
    }

    @Override
    public void tick() {
        boolean justSpawned = this.firstTick;

        super.tick();

        this.chainXO = this.chainX;
        this.chainZO = this.chainZ;
        this.tiltXO = this.tiltX;
        this.tiltZO = this.tiltZ;

        if (level().isClientSide()) {
            if (justSpawned) {
                adoptSyncedState();

                this.chainXO = this.chainX;
                this.chainZO = this.chainZ;
                this.tiltXO = this.tiltX;
                this.tiltZO = this.tiltZ;
            } else {
                tickPendulum(captureFrame());
                correctTowardsSynced();
            }
        } else {
            if (!isStillAnchored()) {
                if (level() instanceof ServerLevel serverLevel) {
                    spawnCrumbleParticles(serverLevel, getBoundingBox());
                }
                discard();
                return;
            }

            if (ModConfig.areAnchorsStatic()) {
                if (level().getBlockEntity(getBoundPos()) instanceof WhisperingAnchorBlockEntity blockEntity) {
                    blockEntity.copyIdentityFrom(this);
                }
                discard();
                return;
            }

            if (this.impactSoundCooldown > 0) {
                this.impactSoundCooldown--;
            }

            if (this.orbitTicks > 0 && --this.orbitTicks == 0) {
                this.entityData.set(DATA_ORBIT_DIRECTION, (byte) 0);
            }

            convertPushToSwing();
            tickPendulum(captureFrame());
            collideWithNearbyAnchors();
            syncState();
        }

        setPos(getCentreOfMass(this.chainX, this.chainZ, this.tiltX, this.tiltZ));
    }

    private boolean isStillAnchored() {
        BlockState state = level().getBlockState(getBoundPos());
        return state.getBlock() instanceof WhisperingAnchorBlock && state.getValue(WhisperingAnchorBlock.PART) == AnchorPart.TOP;
    }

    public static void spawnCrumbleParticles(ServerLevel level, AABB body) {
        Vec3 centre = body.getCenter();
        double rowHeight = body.getYsize() / CRUMBLE_ROWS;

        for (int row = 0; row < CRUMBLE_ROWS; row++) {
            double y = body.minY + (row + 0.5) * rowHeight;
            level.sendParticles(ModParticles.ANCHOR_CRUMBLE.get(), centre.x, y, centre.z, CRUMBLE_PARTICLES_PER_ROW, body.getXsize() * CRUMBLE_SPREAD, rowHeight * CRUMBLE_SPREAD, body.getZsize() * CRUMBLE_SPREAD, 0.0);
        }
    }

    private void convertPushToSwing() {
        Vec3 push = SubLevelHandler.INSTANCE.toLocalDirection(level(), position(), getDeltaMovement());
        double speed = Math.sqrt(push.x * push.x + push.z * push.z);
        if (speed < 1.0E-4) return;

        setDeltaMovement(Vec3.ZERO);

        applyImpulse(push.x / speed, push.z / speed, getY(), speed * VICTIM_MASS_RATIO);
    }

    public void refreshOrbitDrive() {
        this.orbitTicks = ORBIT_HOLD;
        if (this.entityData.get(DATA_ORBIT_DIRECTION) != 0) return;

        double momentum = getTipX() * getTipVelZ() - getTipZ() * getTipVelX();
        byte spin = this.orbitDirection != 0 ? (byte) this.orbitDirection : Math.abs(momentum) > ORBIT_SPIN_MOMENTUM ? (byte) Math.signum(momentum) : 1;
        this.entityData.set(DATA_ORBIT_DIRECTION, spin);
    }

    public int getOrbitDirection() {
        return this.orbitDirection == 0 ? 1 : this.orbitDirection;
    }

    private double getTipX() {
        return CHAIN_LENGTH * Math.sin(this.chainX - this.restX) + TIP_BELOW_SHACKLE * Math.sin(this.tiltX - this.restX);
    }

    private double getTipZ() {
        return CHAIN_LENGTH * Math.sin(this.chainZ - this.restZ) + TIP_BELOW_SHACKLE * Math.sin(this.tiltZ - this.restZ);
    }

    private double getTipVelX() {
        return CHAIN_LENGTH * this.chainVelX + TIP_BELOW_SHACKLE * this.tiltVelX;
    }

    private double getTipVelZ() {
        return CHAIN_LENGTH * this.chainVelZ + TIP_BELOW_SHACKLE * this.tiltVelZ;
    }

    private void applyOrbitDrive() {
        byte spin = this.entityData.get(DATA_ORBIT_DIRECTION);
        if (spin != 0) {
            this.orbitDirection = spin;
            this.orbitStrength = Math.min(1.0F, this.orbitStrength + ORBIT_RAMP_UP);
        } else {
            this.orbitStrength = Math.max(0.0F, this.orbitStrength - ORBIT_RAMP_DOWN);
        }

        if (this.orbitStrength <= 0.0F) {
            this.orbitDirection = 0;
            this.orbitCatchUp = 0.0;
            return;
        }

        double tipX = getTipX();
        double tipZ = getTipZ();
        double radius = Math.sqrt(tipX * tipX + tipZ * tipZ);

        double outX;
        double outZ;
        if (radius < 1.0E-4) {
            float yaw = getFacingYaw() * Mth.DEG_TO_RAD;
            outX = -Mth.sin(yaw);
            outZ = Mth.cos(yaw);
        } else {
            outX = tipX / radius;
            outZ = tipZ / radius;
        }

        double aroundX = -this.orbitDirection * outZ;
        double aroundZ = this.orbitDirection * outX;
        double radialSpeed = getTipVelX() * outX + getTipVelZ() * outZ;
        double orbitSpeed = getTipVelX() * aroundX + getTipVelZ() * aroundZ;

        double radiusError = ORBIT_RADIUS - radius;
        this.orbitCatchUp = Mth.clamp(this.orbitCatchUp + ORBIT_RADIAL_CATCH_UP * this.orbitStrength * radiusError, -ORBIT_MAX_CATCH_UP, ORBIT_MAX_CATCH_UP);

        double centripetal = (ORBIT_SPIN_RATE * ORBIT_SPIN_RATE - ORBIT_NATURAL_RATE * ORBIT_NATURAL_RATE) * radius;
        double radialPull = ORBIT_RADIAL_STIFFNESS * radiusError - ORBIT_DAMPING * radialSpeed + this.orbitCatchUp - centripetal;
        double orbitPush = ORBIT_DAMPING * (ORBIT_SPIN_RATE * ORBIT_RADIUS - orbitSpeed);

        double scale = this.orbitStrength / TIP_ARM;
        double swingX = scale * (outX * radialPull + aroundX * orbitPush);
        double swingZ = scale * (outZ * radialPull + aroundZ * orbitPush);

        this.chainVelX += swingX;
        this.chainVelZ += swingZ;
        this.tiltVelX += swingX;
        this.tiltVelZ += swingZ;
    }

    private static float toAngleX(Vec3 direction, float previous) {
        return previous + (float) Math.IEEEremainder(Math.atan2(direction.x, -direction.y) - previous, Math.PI * 2.0);
    }

    private static float toAngleZ(Vec3 direction) {
        return (float) Math.asin(Mth.clamp(direction.z, -1.0, 1.0));
    }

    public static AnchorEntity createDetached(Level level) {
        AnchorEntity anchor = new AnchorEntity(ModEntities.ANCHOR.get(), level);
        anchor.setBoundPos(BlockPos.ZERO);

        return anchor;
    }

    public void tickSwing(SwingFrame frame) {
        this.chainXO = this.chainX;
        this.chainZO = this.chainZ;
        this.tiltXO = this.tiltX;
        this.tiltZO = this.tiltZ;

        tickPendulum(frame);
    }

    private SwingFrame captureFrame() {
        Vec3 suspension = getSuspensionPoint();
        Vec3 velocity = SubLevelHandler.INSTANCE.getVelocity(level(), suspension);
        Vec3 spin = SubLevelHandler.INSTANCE.getAngularVelocity(level(), suspension);

        return new SwingFrame(velocity, spin, direction -> SubLevelHandler.INSTANCE.toLocalDirection(level(), suspension, direction));
    }

    private Vec3 calculateFrameAcceleration(Vec3 velocity) {
        Vec3 acceleration = this.lastFrameVelocity == null ? Vec3.ZERO : velocity.subtract(this.lastFrameVelocity);
        this.lastFrameVelocity = velocity;

        return acceleration;
    }

    private void applyFrameMotion(SwingFrame frame) {
        Vec3 suspension = getSuspensionPoint();
        Vec3 spin = frame.spin();
        Vec3 spinUp = this.lastFrameSpin == null ? Vec3.ZERO : spin.subtract(this.lastFrameSpin);
        this.lastFrameSpin = spin;

        Vec3 offset = getCentreOfMass(this.chainX, this.chainZ, this.tiltX, this.tiltZ).subtract(suspension);
        Vec3 kick = frame.localDirection().apply(calculateFrameAcceleration(frame.velocity())).add(spinUp.cross(offset));
        this.steadyFrameAcceleration = this.steadyFrameAcceleration.add(kick.subtract(this.steadyFrameAcceleration).scale(1.0 / FRAME_SMOOTHING_TICKS));
        Vec3 jolt = kick.subtract(this.steadyFrameAcceleration);
        double joltStrength = jolt.length();
        if (joltStrength > MAX_FRAME_KICK) {
            jolt = jolt.scale(MAX_FRAME_KICK / joltStrength);
        }
        swingRigidly(jolt.reverse(), offset.length());

        Vec3 pull = frame.localDirection().apply(GRAVITY_PULL).subtract(this.steadyFrameAcceleration).subtract(spin.cross(spin.cross(offset)));
        double strength = pull.length();
        if (!Double.isFinite(strength)) return;
        if (strength < 1.0E-6) {
            this.gravityScale = 0.0;
            return;
        }

        this.localDown = pull.scale(1.0 / strength);
        this.gravityScale = Math.min(strength / GRAVITY, MAX_GRAVITY_SCALE);
        this.restX = toAngleX(this.localDown, this.restX);
        this.restZ = toAngleZ(this.localDown);
    }

    private void swingRigidly(Vec3 velocityChange, double reach) {
        if (reach < 1.0E-4 || velocityChange.lengthSqr() < 1.0E-12) return;

        double cosZ = Math.cos(this.tiltZ);
        Vec3 alongX = new Vec3(Math.cos(this.tiltX) * cosZ, Math.sin(this.tiltX) * cosZ, 0.0);
        Vec3 alongZ = new Vec3(-Math.sin(this.tiltX) * Math.sin(this.tiltZ), Math.cos(this.tiltX) * Math.sin(this.tiltZ), cosZ);
        double deltaX = velocityChange.dot(alongX) / (reach * cosZ * cosZ);
        double deltaZ = velocityChange.dot(alongZ) / reach;

        this.chainVelX += deltaX;
        this.chainVelZ += deltaZ;
        this.tiltVelX += deltaX;
        this.tiltVelZ += deltaZ;
    }

    private double calculatePullAlongX(float angleX, float angleZ) {
        double cosZ = Math.cos(angleZ);
        return (this.localDown.x * Math.cos(angleX) + this.localDown.y * Math.sin(angleX)) / cosZ;
    }

    private double calculatePullAlongZ(float angleX, float angleZ) {
        return (this.localDown.y * Math.cos(angleX) - this.localDown.x * Math.sin(angleX)) * Math.sin(angleZ) + this.localDown.z * Math.cos(angleZ);
    }

    private void applySphericalMotion() {
        double chainVelX = this.chainVelX;
        this.chainVelX += 2.0 * Math.tan(this.chainZ) * chainVelX * this.chainVelZ;
        this.chainVelZ -= Math.sin(this.chainZ) * Math.cos(this.chainZ) * chainVelX * chainVelX;

        double tiltVelX = this.tiltVelX;
        this.tiltVelX += 2.0 * Math.tan(this.tiltZ) * tiltVelX * this.tiltVelZ;
        this.tiltVelZ -= Math.sin(this.tiltZ) * Math.cos(this.tiltZ) * tiltVelX * tiltVelX;
    }

    private void tickPendulum(SwingFrame frame) {
        applyFrameMotion(frame);
        applyOrbitDrive();

        double couplingX = this.gravityScale * TILT_RESTORING * Math.sin(this.chainX - this.tiltX);
        double couplingZ = this.gravityScale * TILT_RESTORING * Math.sin(this.chainZ - this.tiltZ);

        this.tiltVelX += couplingX;
        this.tiltVelZ += couplingZ;
        this.chainVelX += this.gravityScale * CHAIN_RESTORING * calculatePullAlongX(this.chainX, this.chainZ) - TILT_TO_CHAIN * couplingX;
        this.chainVelZ += this.gravityScale * CHAIN_RESTORING * calculatePullAlongZ(this.chainX, this.chainZ) - TILT_TO_CHAIN * couplingZ;

        applySphericalMotion();

        this.chainVelX *= SWING_DRAG;
        this.chainVelZ *= SWING_DRAG;
        this.tiltVelX *= SWING_DRAG;
        this.tiltVelZ *= SWING_DRAG;

        double slipX = (this.tiltVelX - this.chainVelX) * SHACKLE_FRICTION / (1.0 + TILT_TO_CHAIN);
        double slipZ = (this.tiltVelZ - this.chainVelZ) * SHACKLE_FRICTION / (1.0 + TILT_TO_CHAIN);
        this.tiltVelX -= slipX;
        this.tiltVelZ -= slipZ;
        this.chainVelX += TILT_TO_CHAIN * slipX;
        this.chainVelZ += TILT_TO_CHAIN * slipZ;

        this.chainVelX = Mth.clamp(this.chainVelX, -MAX_SWING_RATE, MAX_SWING_RATE);
        this.chainVelZ = Mth.clamp(this.chainVelZ, -MAX_SWING_RATE, MAX_SWING_RATE);
        this.tiltVelX = Mth.clamp(this.tiltVelX, -MAX_SWING_RATE, MAX_SWING_RATE);
        this.tiltVelZ = Mth.clamp(this.tiltVelZ, -MAX_SWING_RATE, MAX_SWING_RATE);

        this.chainX += (float) this.chainVelX;
        this.chainZ += (float) this.chainVelZ;
        this.tiltX += (float) this.tiltVelX;
        this.tiltZ += (float) this.tiltVelZ;

        limitChainSwing();
        limitTiltSwing();

        if (!isSwingFinite()) {
            this.chainX = this.tiltX = this.restX;
            this.chainZ = this.tiltZ = this.restZ;
            this.chainVelX = this.chainVelZ = this.tiltVelX = this.tiltVelZ = 0.0;
        }

        if (this.orbitStrength <= 0.0F && isAtRest()) {
            this.chainX = this.tiltX = this.restX;
            this.chainZ = this.tiltZ = this.restZ;
            this.chainVelX = this.chainVelZ = this.tiltVelX = this.tiltVelZ = 0.0;
        }
    }

    private boolean isSwingFinite() {
        return Float.isFinite(this.chainX) && Float.isFinite(this.chainZ) && Float.isFinite(this.tiltX) && Float.isFinite(this.tiltZ)
                && Double.isFinite(this.chainVelX) && Double.isFinite(this.chainVelZ) && Double.isFinite(this.tiltVelX) && Double.isFinite(this.tiltVelZ);
    }

    private void limitChainSwing() {
        double reach = Math.hypot(this.chainX, this.chainZ);
        if (reach <= MAX_ANGLE) return;

        double outX = this.chainX / reach;
        double outZ = this.chainZ / reach;
        this.chainX = (float) (outX * MAX_ANGLE);
        this.chainZ = (float) (outZ * MAX_ANGLE);

        double outward = this.chainVelX * outX + this.chainVelZ * outZ;
        if (outward > 0.0) {
            double alongX = this.chainVelX - outward * outX;
            double alongZ = this.chainVelZ - outward * outZ;
            this.chainVelX = alongX * LIMIT_FRICTION - outward * LIMIT_RESTITUTION * outX;
            this.chainVelZ = alongZ * LIMIT_FRICTION - outward * LIMIT_RESTITUTION * outZ;
        }
    }

    private void limitTiltSwing() {
        double reach = Math.hypot(this.tiltX, this.tiltZ);
        if (reach <= MAX_ANGLE) return;

        double outX = this.tiltX / reach;
        double outZ = this.tiltZ / reach;
        this.tiltX = (float) (outX * MAX_ANGLE);
        this.tiltZ = (float) (outZ * MAX_ANGLE);

        double outward = this.tiltVelX * outX + this.tiltVelZ * outZ;
        if (outward > 0.0) {
            double alongX = this.tiltVelX - outward * outX;
            double alongZ = this.tiltVelZ - outward * outZ;
            this.tiltVelX = alongX * LIMIT_FRICTION - outward * LIMIT_RESTITUTION * outX;
            this.tiltVelZ = alongZ * LIMIT_FRICTION - outward * LIMIT_RESTITUTION * outZ;
        }
    }

    private boolean isAtRest() {
        return Math.abs(this.chainX - this.restX) < REST_ANGLE_EPSILON && Math.abs(this.chainZ - this.restZ) < REST_ANGLE_EPSILON
                && Math.abs(this.tiltX - this.restX) < REST_ANGLE_EPSILON && Math.abs(this.tiltZ - this.restZ) < REST_ANGLE_EPSILON
                && Math.abs(this.chainVelX) < REST_VELOCITY_EPSILON && Math.abs(this.chainVelZ) < REST_VELOCITY_EPSILON
                && Math.abs(this.tiltVelX) < REST_VELOCITY_EPSILON && Math.abs(this.tiltVelZ) < REST_VELOCITY_EPSILON;
    }

    private void syncState() {
        if (Math.abs(this.chainX - this.sentChainX) < SYNC_EPSILON
                && Math.abs(this.chainZ - this.sentChainZ) < SYNC_EPSILON
                && Math.abs(this.tiltX - this.sentTiltX) < SYNC_EPSILON
                && Math.abs(this.tiltZ - this.sentTiltZ) < SYNC_EPSILON) {
            return;
        }

        this.entityData.set(DATA_CHAIN_X, this.sentChainX = this.chainX);
        this.entityData.set(DATA_CHAIN_Z, this.sentChainZ = this.chainZ);
        this.entityData.set(DATA_TILT_X, this.sentTiltX = this.tiltX);
        this.entityData.set(DATA_TILT_Z, this.sentTiltZ = this.tiltZ);
        this.entityData.set(DATA_CHAIN_VELOCITY_X, (float) this.chainVelX);
        this.entityData.set(DATA_CHAIN_VELOCITY_Z, (float) this.chainVelZ);
        this.entityData.set(DATA_TILT_VELOCITY_X, (float) this.tiltVelX);
        this.entityData.set(DATA_TILT_VELOCITY_Z, (float) this.tiltVelZ);
    }

    private void adoptSyncedState() {
        this.chainX = this.entityData.get(DATA_CHAIN_X);
        this.chainZ = this.entityData.get(DATA_CHAIN_Z);
        this.tiltX = this.entityData.get(DATA_TILT_X);
        this.tiltZ = this.entityData.get(DATA_TILT_Z);
        this.chainVelX = this.entityData.get(DATA_CHAIN_VELOCITY_X);
        this.chainVelZ = this.entityData.get(DATA_CHAIN_VELOCITY_Z);
        this.tiltVelX = this.entityData.get(DATA_TILT_VELOCITY_X);
        this.tiltVelZ = this.entityData.get(DATA_TILT_VELOCITY_Z);
    }

    private void correctTowardsSynced() {
        this.chainX = Mth.lerp(CLIENT_CORRECTION, this.chainX, this.entityData.get(DATA_CHAIN_X));
        this.chainZ = Mth.lerp(CLIENT_CORRECTION, this.chainZ, this.entityData.get(DATA_CHAIN_Z));
        this.tiltX = Mth.lerp(CLIENT_CORRECTION, this.tiltX, this.entityData.get(DATA_TILT_X));
        this.tiltZ = Mth.lerp(CLIENT_CORRECTION, this.tiltZ, this.entityData.get(DATA_TILT_Z));
        this.chainVelX = Mth.lerp(CLIENT_CORRECTION, this.chainVelX, (double) this.entityData.get(DATA_CHAIN_VELOCITY_X));
        this.chainVelZ = Mth.lerp(CLIENT_CORRECTION, this.chainVelZ, (double) this.entityData.get(DATA_CHAIN_VELOCITY_Z));
        this.tiltVelX = Mth.lerp(CLIENT_CORRECTION, this.tiltVelX, (double) this.entityData.get(DATA_TILT_VELOCITY_X));
        this.tiltVelZ = Mth.lerp(CLIENT_CORRECTION, this.tiltVelZ, (double) this.entityData.get(DATA_TILT_VELOCITY_Z));
    }

    private void collideWithNearbyAnchors() {
        if (isAtRest()) return;

        for (AnchorEntity other : level().getEntitiesOfClass(AnchorEntity.class, getBoundingBox(), candidate -> candidate != this && SubLevelHandler.INSTANCE.isInSameSubLevel(level(), position(), candidate.position()))) {
            if (other.isAtRest() || getId() < other.getId()) {
                resolveContact(other);
            }
        }
    }

    private void resolveContact(AnchorEntity other) {
        double dx = getX() - other.getX();
        double dz = getZ() - other.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double clearance = (getBbWidth() + other.getBbWidth()) * 0.5;
        if (distance < 1.0E-4 || distance >= clearance) return;

        double normalX = dx / distance;
        double normalZ = dz / distance;

        AABB self = getBoundingBox();
        AABB theirs = other.getBoundingBox();
        double contactY = (Math.min(self.maxY, theirs.maxY) + Math.max(self.minY, theirs.minY)) * 0.5;
        double thisContact = getShacklePos(this.chainX, this.chainZ).y - contactY;
        double otherContact = other.getShacklePos(other.chainX, other.chainZ).y - contactY;

        double closingSpeed = other.getContactSpeedAlong(normalX, normalZ, otherContact) - getContactSpeedAlong(normalX, normalZ, thisContact);
        if (closingSpeed <= 0.0) return;

        double impulse = (1.0 + COLLISION_RESTITUTION) * closingSpeed / (calculateInverseEffectiveMass(thisContact) + calculateInverseEffectiveMass(otherContact));
        applyImpulse(normalX, normalZ, contactY, impulse);
        other.applyImpulse(-normalX, -normalZ, contactY, impulse);

        if (closingSpeed > SILENT_IMPACT_SPEED && other.impactSoundCooldown <= 0) {
            playImpactSound(0.85F, true);
            other.impactSoundCooldown = this.impactSoundCooldown;
        }
    }

    private Vec3 getContactVelocity(double belowShackle) {
        return new Vec3(CHAIN_LENGTH * this.chainVelX + belowShackle * this.tiltVelX, 0.0, CHAIN_LENGTH * this.chainVelZ + belowShackle * this.tiltVelZ);
    }

    private double getContactSpeedAlong(double normalX, double normalZ, double belowShackle) {
        Vec3 velocity = getContactVelocity(belowShackle);
        return velocity.x * normalX + velocity.z * normalZ;
    }

    private static double calculateInverseEffectiveMass(double belowShackle) {
        double offset = belowShackle - SHACKLE_TO_CENTRE;
        return 1.0 + offset * offset / GYRATION_RADIUS_SQ;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.BLOCKS;
    }

    private void playImpactSound(float pitch, boolean major) {
        if (this.impactSoundCooldown > 0) return;

        this.impactSoundCooldown = IMPACT_SOUND_COOLDOWN;
        playSound(major ? SoundEvents.HEAVY_CORE_BREAK : SoundEvents.HEAVY_CORE_HIT, 2.0F, pitch);
    }

    @Override
    public Vec3 getLightProbePosition(float partialTicks) {
        return getPosition(partialTicks);
    }

    public float getFacingYaw() {
        BlockState state = level().getBlockState(getBoundPos());
        return state.getBlock() instanceof WhisperingAnchorBlock ? state.getValue(WhisperingAnchorBlock.FACING).toYRot() : 0.0F;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        BlockPos bound = getBoundPos();
        BlockState state = level().getBlockState(bound);
        if (!(state.getBlock() instanceof WhisperingAnchorBlock)) {
            return InteractionResult.PASS;
        }

        BlockHitResult hit = toBlockHitResult(bound, player);
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer serverPlayer) {
            if (!level().mayInteract(serverPlayer, bound)) {
                return InteractionResult.FAIL;
            }

            return serverPlayer.gameMode.useItemOn(serverPlayer, level(), stack, hand, hit);
        }

        ItemInteractionResult withItem = state.useItemOn(stack, level(), player, hand, hit);
        if (withItem.consumesAction()) return withItem.result();

        if (withItem == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND) {
            InteractionResult withoutItem = state.useWithoutItem(level(), player, hit);
            if (withoutItem.consumesAction()) {
                return withoutItem;
            }
        }

        if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        }

        return stack.useOn(new UseOnContext(player, hand, hit));
    }

    private static BlockHitResult toBlockHitResult(BlockPos pos, Player player) {
        Vec3 centre = Vec3.atCenterOf(pos);
        Direction face = Direction.getNearest(player.getEyePosition().subtract(centre));

        return new BlockHitResult(centre.relative(face, 0.5), face, pos, false);
    }

    @Override
    public void push(Entity entity) {
        if (level().isClientSide() || entity == this) return;

        Vec3 pusher = SubLevelHandler.INSTANCE.toLocalPosition(level(), position(), entity.position());
        double dx = getX() - pusher.x;
        double dz = getZ() - pusher.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) return;

        AABB self = getBoundingBox();
        double contactY = (Math.min(self.maxY, pusher.y + entity.getBbHeight()) + Math.max(self.minY, pusher.y)) * 0.5;

        double towardsAnchorX = dx / length;
        double towardsAnchorZ = dz / length;
        applyImpulse(towardsAnchorX, towardsAnchorZ, contactY, WALK_SPECIFIC_IMPULSE);
        applyContactHit(entity, -towardsAnchorX, -towardsAnchorZ, contactY);
    }

    private void applyContactHit(Entity target, double normalX, double normalZ, double contactY) {
        if (!(target instanceof LivingEntity victim)) return;

        double belowShackle = getShacklePos(this.chainX, this.chainZ).y - contactY;
        double anchorSpeedTowards = getContactSpeedAlong(normalX, normalZ, belowShackle);
        if (anchorSpeedTowards <= HARMLESS_IMPACT_SPEED) return;

        Vec3 swing = getContactVelocity(belowShackle);
        double swingSpeed = swing.length();
        if (swingSpeed < 1.0E-4) return;

        double pushX = swing.x / swingSpeed;
        double pushZ = swing.z / swingSpeed;

        double effort = getContactVelocity(SHACKLE_TO_CENTRE).length() / MAX_SWING_SPEED;
        float damage = (float) Mth.clamp((effort - DAMAGE_SPEED_FRACTION) / (1.0 - DAMAGE_SPEED_FRACTION) * MAX_IMPACT_DAMAGE, 0.0, MAX_IMPACT_DAMAGE);
        if (damage > 0.0F) {
            if (!victim.hurt(damageSources().source(ModDamageTypes.ANCHOR_SWING, this), damage)) return;

            playImpactSound(1.0F, true);
        }

        Vec3 motion = SubLevelHandler.INSTANCE.toLocalDirection(level(), position(), target.getDeltaMovement());
        double approach = Mth.clamp(anchorSpeedTowards - (motion.x * normalX + motion.z * normalZ), 0.0, anchorSpeedTowards);
        double impulse = (1.0 + IMPACT_RESTITUTION) * approach / (calculateInverseEffectiveMass(belowShackle) + 1.0 / VICTIM_MASS_RATIO);
        applyImpulse(-pushX, -pushZ, contactY, impulse);
        Vec3 knockback = SubLevelHandler.INSTANCE.toWorldDirection(level(), position(), new Vec3(pushX, 0.0, pushZ));
        victim.knockback(impulse / VICTIM_MASS_RATIO, -knockback.x, -knockback.z);
        victim.hurtMarked = true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!level().isClientSide() && !isRemoved() && attacker instanceof LivingEntity living) {
            onHitByAttacker(living);
        }

        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved()) return false;

        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            onHitByAttacker(attacker);
        } else if (source.getDirectEntity() instanceof Projectile projectile) {
            onHitByProjectile(projectile);
        }

        return true;
    }

    private void onHitByProjectile(Projectile projectile) {
        Vec3 flight = SubLevelHandler.INSTANCE.toLocalDirection(level(), position(), projectile.getDeltaMovement());
        double speed = Math.sqrt(flight.x * flight.x + flight.z * flight.z);
        if (speed < 1.0E-4) return;

        double contactY = SubLevelHandler.INSTANCE.toLocalPosition(level(), position(), projectile.position()).y;
        applyImpulse(flight.x / speed, flight.z / speed, contactY, speed * PROJECTILE_MASS_RATIO);
        playImpactSound(1.0F, false);
    }

    private void onHitByAttacker(LivingEntity attacker) {
        Vec3 look = SubLevelHandler.INSTANCE.toLocalDirection(level(), position(), attacker.getLookAngle());
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        if (length >= 1.0E-4) {
            Vec3 eye = SubLevelHandler.INSTANCE.toLocalPosition(level(), position(), attacker.getEyePosition());
            double contactY = getBoundingBox().clip(eye, eye.add(look.scale(6.0))).map(hit -> hit.y).orElse(getY());

            applyImpulse(look.x / length, look.z / length, contactY, HIT_SPECIFIC_IMPULSE);
        }

        playImpactSound(1.0F, false);
    }

    private void applyImpulse(double dirX, double dirZ, double contactY, double specificImpulse) {
        specificImpulse *= 1.0 - ORBIT_RESPONSE_LOSS * this.orbitStrength;
        double armFromCentre = getShacklePos(this.chainX, this.chainZ).y - contactY - SHACKLE_TO_CENTRE;
        double tiltImpulse = specificImpulse * armFromCentre / GYRATION_RADIUS_SQ;
        double chainImpulse = (specificImpulse - SHACKLE_TO_CENTRE * tiltImpulse) / CHAIN_LENGTH;

        this.tiltVelX += dirX * tiltImpulse;
        this.tiltVelZ += dirZ * tiltImpulse;
        this.chainVelX += dirX * chainImpulse;
        this.chainVelZ += dirZ * chainImpulse;
    }

    public BlockPos getBoundPos() {
        return this.entityData.get(DATA_BOUND_POS);
    }

    public void setBoundPos(BlockPos pos) {
        this.entityData.set(DATA_BOUND_POS, pos.immutable());
    }

    public boolean isBoundTo(BlockPos pos) {
        return getBoundPos().equals(pos);
    }

    @Override
    public @Nullable DyeColor getColor() {
        int id = this.entityData.get(DATA_COLOR);
        return id == UNDYED ? null : DyeColor.byId(id);
    }

    @Override
    public void setColor(@Nullable DyeColor color) {
        this.entityData.set(DATA_COLOR, color == null ? UNDYED : color.getId());
    }

    @Override
    public void rename(Component name) {
        setCustomName(name);
        setCustomNameVisible(true);
    }

    @Override
    public Component getDisplayName() {
        DyeColor color = getColor();
        Component name = super.getDisplayName();

        return color == null ? name : name.copy().withStyle(style -> style.withColor(color.getTextColor()));
    }

    public float getChainX(float partialTick) {
        return Mth.lerp(partialTick, this.chainXO, this.chainX);
    }

    public float getChainZ(float partialTick) {
        return Mth.lerp(partialTick, this.chainZO, this.chainZ);
    }

    public float getTiltX(float partialTick) {
        return Mth.lerp(partialTick, this.tiltXO, this.tiltX);
    }

    public float getTiltZ(float partialTick) {
        return Mth.lerp(partialTick, this.tiltZO, this.tiltZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BOUND_POS, BlockPos.ZERO);
        builder.define(DATA_CHAIN_X, 0.0F);
        builder.define(DATA_CHAIN_Z, 0.0F);
        builder.define(DATA_TILT_X, 0.0F);
        builder.define(DATA_TILT_Z, 0.0F);
        builder.define(DATA_CHAIN_VELOCITY_X, 0.0F);
        builder.define(DATA_CHAIN_VELOCITY_Z, 0.0F);
        builder.define(DATA_TILT_VELOCITY_X, 0.0F);
        builder.define(DATA_TILT_VELOCITY_Z, 0.0F);
        builder.define(DATA_COLOR, UNDYED);
        builder.define(DATA_ORBIT_DIRECTION, (byte) 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setBoundPos(BlockPos.of(tag.getLong(KEY_BOUND_POS)));
        this.chainXO = this.chainX = tag.getFloat(KEY_CHAIN_X);
        this.chainZO = this.chainZ = tag.getFloat(KEY_CHAIN_Z);
        this.tiltXO = this.tiltX = tag.getFloat(KEY_TILT_X);
        this.tiltZO = this.tiltZ = tag.getFloat(KEY_TILT_Z);
        this.chainVelX = tag.getDouble(KEY_CHAIN_VELOCITY_X);
        this.chainVelZ = tag.getDouble(KEY_CHAIN_VELOCITY_Z);
        this.tiltVelX = tag.getDouble(KEY_TILT_VELOCITY_X);
        this.tiltVelZ = tag.getDouble(KEY_TILT_VELOCITY_Z);
        this.entityData.set(DATA_CHAIN_X, this.sentChainX = this.chainX);
        this.entityData.set(DATA_CHAIN_Z, this.sentChainZ = this.chainZ);
        this.entityData.set(DATA_TILT_X, this.sentTiltX = this.tiltX);
        this.entityData.set(DATA_TILT_Z, this.sentTiltZ = this.tiltZ);
        this.entityData.set(DATA_CHAIN_VELOCITY_X, (float) this.chainVelX);
        this.entityData.set(DATA_CHAIN_VELOCITY_Z, (float) this.chainVelZ);
        this.entityData.set(DATA_TILT_VELOCITY_X, (float) this.tiltVelX);
        this.entityData.set(DATA_TILT_VELOCITY_Z, (float) this.tiltVelZ);
        setColor(tag.contains(KEY_COLOR) ? DyeColor.byName(tag.getString(KEY_COLOR), null) : null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong(KEY_BOUND_POS, getBoundPos().asLong());
        tag.putFloat(KEY_CHAIN_X, this.chainX);
        tag.putFloat(KEY_CHAIN_Z, this.chainZ);
        tag.putFloat(KEY_TILT_X, this.tiltX);
        tag.putFloat(KEY_TILT_Z, this.tiltZ);
        tag.putDouble(KEY_CHAIN_VELOCITY_X, this.chainVelX);
        tag.putDouble(KEY_CHAIN_VELOCITY_Z, this.chainVelZ);
        tag.putDouble(KEY_TILT_VELOCITY_X, this.tiltVelX);
        tag.putDouble(KEY_TILT_VELOCITY_Z, this.tiltVelZ);

        DyeColor color = getColor();
        if (color != null) {
            tag.putString(KEY_COLOR, color.getName());
        }
    }

    public record SwingFrame(Vec3 velocity, Vec3 spin, UnaryOperator<Vec3> localDirection) {
    }
}
