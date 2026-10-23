package net.grid.whisperinganchors.common.world.entity;

import net.grid.whisperinganchors.api.AnchorFeatures;
import net.grid.whisperinganchors.common.core.ModConfigs;
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
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
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

public class AnchorEntity extends Entity implements AnchorIdentity, Leashable {

    public static final String KEY_BOUND_POS = "BoundPos";
    public static final String KEY_COLOR = "Color";
    public static final String KEY_CUSTOM_NAME = "CustomName";
    public static final String KEY_SWING = "Swing";
    private static final String KEY_ANGLE_X = "AngleX";
    private static final String KEY_ANGLE_Z = "AngleZ";
    private static final String KEY_VELOCITY_X = "VelocityX";
    private static final String KEY_VELOCITY_Z = "VelocityZ";
    private static final String KEY_ROPE_LENGTH = "RopeLength";

    public static final int UNDYED = -1;

    public static final float BODY_WIDTH = 0.99F;
    public static final float BODY_HEIGHT = 2.33F;
    private static final double MODEL_CENTRE_ABOVE_MASS = 0.309;
    public static final float BODY_TOP = (float) MODEL_CENTRE_ABOVE_MASS + BODY_HEIGHT * 0.5F;
    public static final float EYE_HEIGHT = (float) MODEL_CENTRE_ABOVE_MASS - BODY_HEIGHT * 0.15F;
    private static final float LEASH_HEIGHT = (float) MODEL_CENTRE_ABOVE_MASS - BODY_HEIGHT * 0.2F;
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
    private static final double BODY_RESTORING = GRAVITY * SHACKLE_TO_CENTRE / GYRATION_RADIUS_SQ;
    private static final double BODY_TO_CHAIN = SHACKLE_TO_CENTRE / CHAIN_LENGTH;

    private static final double SWING_DRAG = 0.997;
    private static final double SHACKLE_FRICTION = 0.40;
    private static final float MAX_ANGLE = (float) Math.toRadians(70.0);
    private static final double LIMIT_RESTITUTION = 0.2;
    private static final double LIMIT_FRICTION = 0.5;

    private static final double HIT_SPECIFIC_IMPULSE = 0.02;
    private static final double MIN_HIT_STRENGTH = 0.2;
    private static final double WALK_SPECIFIC_IMPULSE = 0.0005;
    private static final double PROJECTILE_MASS_RATIO = 0.01;

    private static final double IMPACT_RESTITUTION = 0.1;
    private static final double VICTIM_MASS_RATIO = 0.08;
    private static final double HARMLESS_IMPACT_SPEED = 0.02;
    private static final double MAX_SWING_SPEED = 0.200;
    private static final double DAMAGE_SPEED_FRACTION = 0.75;
    private static final float MAX_IMPACT_DAMAGE = 6.0F;
    private static final double CARRIED_DAMAGE_PER_SPEED = 5.0;
    private static final double MAX_CARRIED_DAMAGE_MULTIPLIER = 4.0;

    private static final double SWEEP_MARGIN = 1.0;

    private static final double COLLISION_RESTITUTION = 0.25;

    private static final double SILENT_IMPACT_SPEED = 0.02;
    private static final int IMPACT_SOUND_COOLDOWN = 5;

    private static final float REST_ANGLE_EPSILON = 0.006F;
    private static final double REST_VELOCITY_EPSILON = 0.002;
    private static final float SYNC_EPSILON = 0.0008F;
    private static final float CLIENT_CORRECTION = 0.35F;

    private static final double TIP_BELOW_SHACKLE = SHACKLE_TO_CENTRE + BODY_HEIGHT * 0.5 - MODEL_CENTRE_ABOVE_MASS;
    private static final double TIP_ARM = CHAIN_LENGTH + TIP_BELOW_SHACKLE;
    private static final double LEASH_BELOW_SHACKLE = SHACKLE_TO_CENTRE - LEASH_HEIGHT;

    private static final double MIN_ROPE_LENGTH = 1.5;
    private static final double ROPE_CORRECTION = 0.1;
    private static final double HELD_ROPE_MAX_TENSION = GRAVITY * 0.5;
    private static final double TIED_ROPE_MAX_TENSION = GRAVITY * 2.0;
    private static final double MIN_HANG_DEPTH = 0.25;
    private static final double TAUT_ROPE_DRAG = 0.85;
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
    private static final EntityDataAccessor<Float> DATA_BODY_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BODY_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHAIN_VELOCITY_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHAIN_VELOCITY_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BODY_VELOCITY_X = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BODY_VELOCITY_Z = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ORBIT_DIRECTION = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_ROPE_LENGTH = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);

    private final Joint chain = new Joint("Chain", DATA_CHAIN_X, DATA_CHAIN_Z, DATA_CHAIN_VELOCITY_X, DATA_CHAIN_VELOCITY_Z);
    private final Joint body = new Joint("Body", DATA_BODY_X, DATA_BODY_Z, DATA_BODY_VELOCITY_X, DATA_BODY_VELOCITY_Z);
    private float restX;
    private float restZ;
    private double gravityScale = 1.0;
    private Vec3 localDown = new Vec3(0.0, -1.0, 0.0);
    private @Nullable Vec3 lastFrameVelocity;
    private @Nullable Vec3 lastFrameSpin;
    private Vec3 steadyFrameAcceleration = Vec3.ZERO;
    private int impactSoundCooldown;
    private int orbitTicks;
    private int orbitDirection;
    private float orbitStrength;
    private double orbitCatchUp;
    private Frame frame = new LevelFrame();
    private @Nullable ImpulseListener impulseListener;
    private @Nullable LeashData leashData;

    public AnchorEntity(EntityType<? extends AnchorEntity> type, Level level) {
        super(type, level);
    }

    public @Nullable static AnchorEntity spawnFor(ServerLevel level, BlockPos topPos, @Nullable CompoundTag swing) {
        AnchorEntity anchor = ModEntities.ANCHOR.get().create(level);
        if (anchor == null) return null;

        anchor.setBoundPos(topPos);
        if (swing != null) {
            anchor.readSwing(swing);
        }
        anchor.setPos(anchor.getCurrentCentreOfMass());
        anchor.setYRot(anchor.getFacingYaw());
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

    public Vec3 getCentreOfMass(float chainX, float chainZ, float bodyX, float bodyZ) {
        return getShacklePos(chainX, chainZ).add(calculatePendulumOffset(SHACKLE_TO_CENTRE, bodyX, bodyZ));
    }

    private Vec3 getCurrentShacklePos() {
        return getShacklePos(this.chain.x, this.chain.z);
    }

    private Vec3 getCurrentLeashPos() {
        return getCurrentShacklePos().add(calculatePendulumOffset(LEASH_BELOW_SHACKLE, this.body.x, this.body.z));
    }

    private Vec3 getCurrentCentreOfMass() {
        return getCentreOfMass(this.chain.x, this.chain.z, this.body.x, this.body.z);
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

        rememberPreviousSwing();

        if (level().isClientSide()) {
            if (justSpawned) {
                adoptSyncedState();

                rememberPreviousSwing();
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

            if (ModConfigs.areAnchorsStatic()) {
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
            if (AnchorFeatures.doMovingAnchorsHitEntities() && SubLevelHandler.INSTANCE.isInSubLevel(level(), position())) {
                collideWithEntities();
            }
            syncState();
        }

        setPos(getCurrentCentreOfMass());
        setYRot(getFacingYaw());
    }

    private boolean isStillAnchored() {
        return WhisperingAnchorBlock.isPart(level().getBlockState(getBoundPos()), AnchorPart.TOP);
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
        Vec3 push = this.frame.toLocalDirection(getDeltaMovement());
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
        return CHAIN_LENGTH * Math.sin(this.chain.x - this.restX) + TIP_BELOW_SHACKLE * Math.sin(this.body.x - this.restX);
    }

    private double getTipZ() {
        return CHAIN_LENGTH * Math.sin(this.chain.z - this.restZ) + TIP_BELOW_SHACKLE * Math.sin(this.body.z - this.restZ);
    }

    private double getTipVelX() {
        return CHAIN_LENGTH * this.chain.velX + TIP_BELOW_SHACKLE * this.body.velX;
    }

    private double getTipVelZ() {
        return CHAIN_LENGTH * this.chain.velZ + TIP_BELOW_SHACKLE * this.body.velZ;
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

        this.chain.addVelocity(swingX, swingZ);
        this.body.addVelocity(swingX, swingZ);
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
        rememberPreviousSwing();
        tickPendulum(frame);
        setPos(getCurrentCentreOfMass());
    }

    private void rememberPreviousSwing() {
        this.chain.rememberPrevious();
        this.body.rememberPrevious();
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

        Vec3 offset = getCurrentCentreOfMass().subtract(suspension);
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

    private static Vec3 getSwingAlongX(float angleX, float angleZ) {
        double cosZ = Math.cos(angleZ);
        return new Vec3(Math.cos(angleX) * cosZ, Math.sin(angleX) * cosZ, 0.0);
    }

    private static Vec3 getSwingAlongZ(float angleX, float angleZ) {
        return new Vec3(-Math.sin(angleX) * Math.sin(angleZ), Math.cos(angleX) * Math.sin(angleZ), Math.cos(angleZ));
    }

    public void addSwingVelocity(Vec3 velocity) {
        swingRigidly(velocity, getCurrentCentreOfMass().subtract(getSuspensionPoint()).length());
    }

    public void transformSwing(UnaryOperator<Vec3> rotation) {
        this.chain.transform(rotation);
        this.body.transform(rotation);
    }

    private void swingRigidly(Vec3 velocityChange, double reach) {
        if (reach < 1.0E-4 || velocityChange.lengthSqr() < 1.0E-12) return;

        double cosZ = Math.cos(this.body.z);
        double deltaX = velocityChange.dot(getSwingAlongX(this.body.x, this.body.z)) / (reach * cosZ * cosZ);
        double deltaZ = velocityChange.dot(getSwingAlongZ(this.body.x, this.body.z)) / reach;

        this.chain.addVelocity(deltaX, deltaZ);
        this.body.addVelocity(deltaX, deltaZ);
    }

    private double calculatePullAlongX(float angleX, float angleZ) {
        double cosZ = Math.cos(angleZ);
        return (this.localDown.x * Math.cos(angleX) + this.localDown.y * Math.sin(angleX)) / cosZ;
    }

    private double calculatePullAlongZ(float angleX, float angleZ) {
        return (this.localDown.y * Math.cos(angleX) - this.localDown.x * Math.sin(angleX)) * Math.sin(angleZ) + this.localDown.z * Math.cos(angleZ);
    }

    private void tickPendulum(SwingFrame frame) {
        applyFrameMotion(frame);
        applyOrbitDrive();

        double couplingX = this.gravityScale * BODY_RESTORING * Math.sin(this.chain.x - this.body.x);
        double couplingZ = this.gravityScale * BODY_RESTORING * Math.sin(this.chain.z - this.body.z);

        this.body.addVelocity(couplingX, couplingZ);
        this.chain.addVelocity(this.gravityScale * CHAIN_RESTORING * calculatePullAlongX(this.chain.x, this.chain.z) - BODY_TO_CHAIN * couplingX, this.gravityScale * CHAIN_RESTORING * calculatePullAlongZ(this.chain.x, this.chain.z) - BODY_TO_CHAIN * couplingZ);

        this.chain.applySphericalMotion();
        this.body.applySphericalMotion();

        this.chain.dampVelocity(SWING_DRAG);
        this.body.dampVelocity(SWING_DRAG);

        double slipX = (this.body.velX - this.chain.velX) * SHACKLE_FRICTION / (1.0 + BODY_TO_CHAIN);
        double slipZ = (this.body.velZ - this.chain.velZ) * SHACKLE_FRICTION / (1.0 + BODY_TO_CHAIN);
        this.body.addVelocity(-slipX, -slipZ);
        this.chain.addVelocity(BODY_TO_CHAIN * slipX, BODY_TO_CHAIN * slipZ);

        applyRope();

        this.chain.clampVelocity(MAX_SWING_RATE);
        this.body.clampVelocity(MAX_SWING_RATE);

        this.chain.move();
        this.body.move();

        this.chain.limitSwing();
        this.body.limitSwing();

        if (!this.chain.isFinite() || !this.body.isFinite() || this.orbitStrength <= 0.0F && isAtRest()) {
            this.chain.settle(this.restX, this.restZ);
            this.body.settle(this.restX, this.restZ);
        }
    }

    private void applyRope() {
        Entity holder = getLeashHolder();
        if (holder == null || holder.level() != level()) return;

        Vec3 tied = getCurrentLeashPos();
        Vec3 toHold = this.frame.toLocalPosition(holder.getRopeHoldPosition(1.0F)).subtract(tied);
        double distance = toHold.length();
        double overstretch = distance - this.entityData.get(DATA_ROPE_LENGTH);
        if (overstretch <= 0.0 || distance < 1.0E-4) return;

        Vec3 pull = toHold.scale(1.0 / distance);
        Vec3 tiedMotion = this.chain.getMotion().scale(CHAIN_LENGTH).add(this.body.getMotion().scale(LEASH_BELOW_SHACKLE));
        double needed = (overstretch * ROPE_CORRECTION - pull.dot(tiedMotion)) / calculateInverseEffectiveMass(getCurrentShacklePos().y - tied.y);
        double tension = Mth.clamp(needed, 0.0, holder instanceof LeashFenceKnotEntity ? TIED_ROPE_MAX_TENSION : HELD_ROPE_MAX_TENSION);

        Vec3 hang = tied.subtract(getSuspensionPoint());
        double depth = Math.max(-hang.y, MIN_HANG_DEPTH);
        double pushX = tension * (pull.x + pull.y * hang.x / depth);
        double pushZ = tension * (pull.z + pull.y * hang.z / depth);
        double push = Math.sqrt(pushX * pushX + pushZ * pushZ);
        if (push > 1.0E-6) {
            applyImpulse(pushX / push, pushZ / push, tied.y, push);
        }

        this.chain.dampVelocity(TAUT_ROPE_DRAG);
        this.body.dampVelocity(TAUT_ROPE_DRAG);
    }

    private boolean isAtRest() {
        return this.chain.isAtRest(this.restX, this.restZ) && this.body.isAtRest(this.restX, this.restZ);
    }

    private void syncState() {
        if (!this.chain.hasDriftedFromSent() && !this.body.hasDriftedFromSent()) return;

        sendState();
    }

    private void sendState() {
        this.chain.send();
        this.body.send();
    }

    private void adoptSyncedState() {
        this.chain.adoptSynced();
        this.body.adoptSynced();
    }

    private void correctTowardsSynced() {
        this.chain.correctTowardsSynced();
        this.body.correctTowardsSynced();
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
        double thisContact = getCurrentShacklePos().y - contactY;
        double otherContact = other.getCurrentShacklePos().y - contactY;

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
        return new Vec3(CHAIN_LENGTH * this.chain.velX + belowShackle * this.body.velX, 0.0, CHAIN_LENGTH * this.chain.velZ + belowShackle * this.body.velZ);
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
        if (isSilent()) return;

        Vec3 at = this.frame.toWorldPosition(position());
        level().playSound(null, at.x, at.y, at.z, major ? SoundEvents.HEAVY_CORE_BREAK : SoundEvents.HEAVY_CORE_HIT, getSoundSource(), 2.0F, pitch);
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
        InteractionResult leashing = super.interact(player, hand);
        if (leashing.consumesAction()) return leashing;

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

    @Override
    public @Nullable LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable LeashData leashData) {
        this.leashData = leashData;
    }

    @Override
    public void setLeashedTo(Entity leashHolder, boolean broadcastPacket) {
        if (!isLeashed()) {
            double distance = getCurrentLeashPos().distanceTo(this.frame.toLocalPosition(leashHolder.getRopeHoldPosition(1.0F)));
            this.entityData.set(DATA_ROPE_LENGTH, (float) Mth.clamp(distance, MIN_ROPE_LENGTH, LEASH_ELASTIC_DIST));
        }

        Leashable.super.setLeashedTo(leashHolder, broadcastPacket);
    }

    @Override
    public boolean handleLeashAtDistance(Entity leashHolder, float distance) {
        return distance > LEASH_TOO_FAR_DIST;
    }

    @Override
    public Vec3 getLeashOffset(float partialTick) {
        Vec3 offset = calculatePendulumOffset(LEASH_BELOW_SHACKLE - SHACKLE_TO_CENTRE, getBodyX(partialTick), getBodyZ(partialTick));
        float yaw = getPreciseBodyRotation(partialTick) * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);

        return new Vec3(offset.x * cos + offset.z * sin, offset.y, offset.z * cos - offset.x * sin);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && reason.shouldDestroy() && isLeashed()) {
            dropLeash(true, true);
        }

        super.remove(reason);
    }

    private static BlockHitResult toBlockHitResult(BlockPos pos, Player player) {
        Vec3 centre = Vec3.atCenterOf(pos);
        Direction face = Direction.getNearest(player.getEyePosition().subtract(centre));

        return new BlockHitResult(centre.relative(face, 0.5), face, pos, false);
    }

    @Override
    public void push(Entity entity) {
        if (level().isClientSide() || entity == this) return;

        pushFrom(entity, this.frame.toLocalPosition(entity.position()));
    }

    private void pushFrom(Entity entity, Vec3 pusher) {
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

        double belowShackle = getCurrentShacklePos().y - contactY;
        Vec3 carried = this.frame.getVelocity(new Vec3(getX(), contactY, getZ()));
        Vec3 swing = getContactVelocity(belowShackle).add(carried.x, 0.0, carried.z);
        double anchorSpeedTowards = swing.x * normalX + swing.z * normalZ;
        if (anchorSpeedTowards <= HARMLESS_IMPACT_SPEED) return;

        double swingSpeed = swing.length();
        if (swingSpeed < 1.0E-4) return;

        double pushX = swing.x / swingSpeed;
        double pushZ = swing.z / swingSpeed;

        double effort = getContactVelocity(SHACKLE_TO_CENTRE).add(carried.x, 0.0, carried.z).length() / MAX_SWING_SPEED;
        double carriedTowards = Math.max(0.0, carried.x * normalX + carried.z * normalZ);
        double multiplier = Math.min(1.0 + carriedTowards * CARRIED_DAMAGE_PER_SPEED, MAX_CARRIED_DAMAGE_MULTIPLIER);
        float damage = (float) (Mth.clamp((effort - DAMAGE_SPEED_FRACTION) / (1.0 - DAMAGE_SPEED_FRACTION) * MAX_IMPACT_DAMAGE, 0.0, MAX_IMPACT_DAMAGE) * multiplier);
        if (damage > 0.0F) {
            if (!victim.hurt(damageSources().source(ModDamageTypes.ANCHOR_SWING, this), damage)) return;

            playImpactSound(1.0F, true);
        }

        Vec3 motion = this.frame.toLocalDirection(target.getDeltaMovement());
        double approach = Mth.clamp(anchorSpeedTowards - (motion.x * normalX + motion.z * normalZ), 0.0, anchorSpeedTowards);
        double impulse = (1.0 + IMPACT_RESTITUTION) * approach / (calculateInverseEffectiveMass(belowShackle) + 1.0 / VICTIM_MASS_RATIO);
        applyImpulse(-pushX, -pushZ, contactY, impulse);
        Vec3 knockback = this.frame.toWorldDirection(new Vec3(pushX, 0.0, pushZ));
        victim.knockback(impulse / VICTIM_MASS_RATIO, -knockback.x, -knockback.z);
        victim.hurtMarked = true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!level().isClientSide() && !isRemoved() && attacker instanceof LivingEntity living) {
            takeHitFrom(living);
        }

        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved()) return false;

        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            takeHitFrom(attacker);
        } else if (source.getDirectEntity() instanceof Projectile projectile) {
            onHitByProjectile(projectile);
        }

        return true;
    }

    private void onHitByProjectile(Projectile projectile) {
        Vec3 flight = this.frame.toLocalDirection(projectile.getDeltaMovement());
        double speed = Math.sqrt(flight.x * flight.x + flight.z * flight.z);
        if (speed < 1.0E-4) return;

        double contactY = this.frame.toLocalPosition(projectile.position()).y;
        applyImpulse(flight.x / speed, flight.z / speed, contactY, speed * PROJECTILE_MASS_RATIO);
        playImpactSound(1.0F, false);
    }

    public void takeHitFrom(LivingEntity attacker) {
        float charge = 1.0F;
        if (attacker instanceof Player player) {
            charge = player.getAttackStrengthScale(0.5F);
            player.resetAttackStrengthTicker();
        }

        Vec3 eye = this.frame.toLocalPosition(attacker.getEyePosition());
        Vec3 look = this.frame.toLocalDirection(attacker.getLookAngle());
        punch(eye, look, MIN_HIT_STRENGTH + (1.0 - MIN_HIT_STRENGTH) * charge * charge);
        playImpactSound(1.0F, false);
    }

    private void punch(Vec3 eye, Vec3 look, double strength) {
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        if (length < 1.0E-4) return;

        double contactY = getSwingBody().clip(eye, eye.add(look.scale(6.0))).map(hit -> hit.y).orElse(getCurrentCentreOfMass().y);
        applyImpulse(look.x / length, look.z / length, contactY, HIT_SPECIFIC_IMPULSE * strength);
    }

    public AABB getSwingBody() {
        return AABB.ofSize(getCurrentCentreOfMass().add(0.0, MODEL_CENTRE_ABOVE_MASS, 0.0), BODY_WIDTH, BODY_HEIGHT, BODY_WIDTH);
    }

    public void applyImpulse(double dirX, double dirZ, double contactY, double specificImpulse) {
        double impulse = specificImpulse * (1.0 - ORBIT_RESPONSE_LOSS * this.orbitStrength);
        double armFromCentre = getCurrentShacklePos().y - contactY - SHACKLE_TO_CENTRE;
        double bodyImpulse = impulse * armFromCentre / GYRATION_RADIUS_SQ;
        double chainImpulse = (impulse - SHACKLE_TO_CENTRE * bodyImpulse) / CHAIN_LENGTH;

        this.body.addVelocity(dirX * bodyImpulse, dirZ * bodyImpulse);
        this.chain.addVelocity(dirX * chainImpulse, dirZ * chainImpulse);

        if (this.impulseListener != null) {
            this.impulseListener.onImpulse(dirX, dirZ, contactY, specificImpulse);
        }
    }

    public void collideWithEntities() {
        AABB body = getBoundingBox();
        Vec3 swing = getContactVelocity(SHACKLE_TO_CENTRE);
        AABB search = toWorldBox(body).inflate(this.frame.getVelocity(position()).length() + swing.length() + SWEEP_MARGIN);

        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, search, EntitySelector.NO_SPECTATORS)) {
            double halfWidth = entity.getBbWidth() * 0.5;
            AABB reach = new AABB(body.minX - halfWidth, body.minY - entity.getBbHeight(), body.minZ - halfWidth, body.maxX + halfWidth, body.maxY, body.maxZ + halfWidth);
            Vec3 now = this.frame.toLocalPosition(entity.position());
            if (reach.contains(now)) {
                pushFrom(entity, now);
                continue;
            }

            Vec3 travelled = this.frame.toLocalDirection(entity.getDeltaMovement()).subtract(this.frame.getVelocity(now)).subtract(swing);
            reach.clip(now.subtract(travelled), now).ifPresent(entry -> pushFrom(entity, entry));
        }
    }

    private AABB toWorldBox(AABB local) {
        Vec3 first = this.frame.toWorldPosition(new Vec3(local.minX, local.minY, local.minZ));
        AABB box = new AABB(first, first);
        for (int corner = 1; corner < 8; corner++) {
            Vec3 point = this.frame.toWorldPosition(new Vec3((corner & 1) == 0 ? local.minX : local.maxX, (corner & 2) == 0 ? local.minY : local.maxY, (corner & 4) == 0 ? local.minZ : local.maxZ));
            box = box.minmax(new AABB(point, point));
        }

        return box;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public void setImpulseListener(@Nullable ImpulseListener impulseListener) {
        this.impulseListener = impulseListener;
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
        return AnchorIdentity.colorName(super.getDisplayName(), getColor());
    }

    public float getChainX(float partialTick) {
        return Mth.lerp(partialTick, this.chain.xO, this.chain.x);
    }

    public float getChainZ(float partialTick) {
        return Mth.lerp(partialTick, this.chain.zO, this.chain.z);
    }

    public float getBodyX(float partialTick) {
        return Mth.lerp(partialTick, this.body.xO, this.body.x);
    }

    public float getBodyZ(float partialTick) {
        return Mth.lerp(partialTick, this.body.zO, this.body.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BOUND_POS, BlockPos.ZERO);
        builder.define(DATA_CHAIN_X, 0.0F);
        builder.define(DATA_CHAIN_Z, 0.0F);
        builder.define(DATA_BODY_X, 0.0F);
        builder.define(DATA_BODY_Z, 0.0F);
        builder.define(DATA_CHAIN_VELOCITY_X, 0.0F);
        builder.define(DATA_CHAIN_VELOCITY_Z, 0.0F);
        builder.define(DATA_BODY_VELOCITY_X, 0.0F);
        builder.define(DATA_BODY_VELOCITY_Z, 0.0F);
        builder.define(DATA_COLOR, UNDYED);
        builder.define(DATA_ORBIT_DIRECTION, (byte) 0);
        builder.define(DATA_ROPE_LENGTH, (float) LEASH_ELASTIC_DIST);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setBoundPos(BlockPos.of(tag.getLong(KEY_BOUND_POS)));
        readSwing(tag.getCompound(KEY_SWING));
        setColor(tag.contains(KEY_COLOR) ? DyeColor.byName(tag.getString(KEY_COLOR), null) : null);
        this.leashData = readLeashData(tag);
        if (tag.contains(KEY_ROPE_LENGTH)) {
            this.entityData.set(DATA_ROPE_LENGTH, tag.getFloat(KEY_ROPE_LENGTH));
        }
    }

    public void readSwing(CompoundTag swing) {
        this.chain.read(swing);
        this.body.read(swing);
        sendState();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong(KEY_BOUND_POS, getBoundPos().asLong());
        tag.put(KEY_SWING, writeSwing());
        writeLeashData(tag, this.leashData);
        tag.putFloat(KEY_ROPE_LENGTH, this.entityData.get(DATA_ROPE_LENGTH));

        DyeColor color = getColor();
        if (color != null) {
            tag.putString(KEY_COLOR, color.getName());
        }
    }

    public CompoundTag writeSwing() {
        CompoundTag swing = new CompoundTag();
        this.chain.write(swing);
        this.body.write(swing);

        return swing;
    }

    public record SwingFrame(Vec3 velocity, Vec3 spin, UnaryOperator<Vec3> localDirection) {
    }

    public interface Frame {

        Vec3 toLocalPosition(Vec3 worldPosition);

        Vec3 toLocalDirection(Vec3 worldDirection);

        Vec3 toWorldPosition(Vec3 localPosition);

        Vec3 toWorldDirection(Vec3 localDirection);

        Vec3 getVelocity(Vec3 localPosition);
    }

    @FunctionalInterface
    public interface ImpulseListener {

        void onImpulse(double dirX, double dirZ, double contactY, double specificImpulse);
    }

    private class LevelFrame implements Frame {

        @Override
        public Vec3 toLocalPosition(Vec3 worldPosition) {
            return SubLevelHandler.INSTANCE.toLocalPosition(level(), position(), worldPosition);
        }

        @Override
        public Vec3 toLocalDirection(Vec3 worldDirection) {
            return SubLevelHandler.INSTANCE.toLocalDirection(level(), position(), worldDirection);
        }

        @Override
        public Vec3 toWorldPosition(Vec3 localPosition) {
            return SubLevelHandler.INSTANCE.toWorldPosition(level(), localPosition);
        }

        @Override
        public Vec3 toWorldDirection(Vec3 localDirection) {
            return SubLevelHandler.INSTANCE.toWorldDirection(level(), position(), localDirection);
        }

        @Override
        public Vec3 getVelocity(Vec3 localPosition) {
            return toLocalDirection(SubLevelHandler.INSTANCE.getVelocity(level(), localPosition));
        }
    }

    private class Joint {

        private final String key;
        private final EntityDataAccessor<Float> dataX;
        private final EntityDataAccessor<Float> dataZ;
        private final EntityDataAccessor<Float> dataVelocityX;
        private final EntityDataAccessor<Float> dataVelocityZ;

        private float x;
        private float z;
        private float xO;
        private float zO;
        private double velX;
        private double velZ;
        private float sentX;
        private float sentZ;

        private Joint(String key, EntityDataAccessor<Float> dataX, EntityDataAccessor<Float> dataZ, EntityDataAccessor<Float> dataVelocityX, EntityDataAccessor<Float> dataVelocityZ) {
            this.key = key;
            this.dataX = dataX;
            this.dataZ = dataZ;
            this.dataVelocityX = dataVelocityX;
            this.dataVelocityZ = dataVelocityZ;
        }

        private void rememberPrevious() {
            this.xO = this.x;
            this.zO = this.z;
        }

        private void addVelocity(double deltaX, double deltaZ) {
            this.velX += deltaX;
            this.velZ += deltaZ;
        }

        private void dampVelocity(double drag) {
            this.velX *= drag;
            this.velZ *= drag;
        }

        private void clampVelocity(double max) {
            this.velX = Mth.clamp(this.velX, -max, max);
            this.velZ = Mth.clamp(this.velZ, -max, max);
        }

        private void applySphericalMotion() {
            double velX = this.velX;
            this.velX += 2.0 * Math.tan(this.z) * velX * this.velZ;
            this.velZ -= Math.sin(this.z) * Math.cos(this.z) * velX * velX;
        }

        private Vec3 getMotion() {
            return getSwingAlongX(this.x, this.z).scale(this.velX).add(getSwingAlongZ(this.x, this.z).scale(this.velZ));
        }

        private void move() {
            this.x += (float) this.velX;
            this.z += (float) this.velZ;
        }

        private void limitSwing() {
            double reach = Math.hypot(this.x, this.z);
            if (reach <= MAX_ANGLE) return;

            double outX = this.x / reach;
            double outZ = this.z / reach;
            this.x = (float) (outX * MAX_ANGLE);
            this.z = (float) (outZ * MAX_ANGLE);

            double outward = this.velX * outX + this.velZ * outZ;
            if (outward > 0.0) {
                double alongX = this.velX - outward * outX;
                double alongZ = this.velZ - outward * outZ;
                this.velX = alongX * LIMIT_FRICTION - outward * LIMIT_RESTITUTION * outX;
                this.velZ = alongZ * LIMIT_FRICTION - outward * LIMIT_RESTITUTION * outZ;
            }
        }

        private void transform(UnaryOperator<Vec3> rotation) {
            Vec3 direction = rotation.apply(calculatePendulumOffset(1.0, this.x, this.z));
            Vec3 motion = rotation.apply(getMotion());

            float newX = toAngleX(direction, this.x);
            float newZ = toAngleZ(direction);
            double cosZ = Math.max(Math.cos(newZ), 1.0E-3);

            this.xO = this.x = newX;
            this.zO = this.z = newZ;
            this.velX = motion.dot(getSwingAlongX(newX, newZ)) / (cosZ * cosZ);
            this.velZ = motion.dot(getSwingAlongZ(newX, newZ));
        }

        private boolean isFinite() {
            return Float.isFinite(this.x) && Float.isFinite(this.z) && Double.isFinite(this.velX) && Double.isFinite(this.velZ);
        }

        private boolean isAtRest(float restX, float restZ) {
            return Math.abs(this.x - restX) < REST_ANGLE_EPSILON && Math.abs(this.z - restZ) < REST_ANGLE_EPSILON
                    && Math.abs(this.velX) < REST_VELOCITY_EPSILON && Math.abs(this.velZ) < REST_VELOCITY_EPSILON;
        }

        private void settle(float restX, float restZ) {
            this.x = restX;
            this.z = restZ;
            this.velX = 0.0;
            this.velZ = 0.0;
        }

        private boolean hasDriftedFromSent() {
            return Math.abs(this.x - this.sentX) >= SYNC_EPSILON || Math.abs(this.z - this.sentZ) >= SYNC_EPSILON;
        }

        private void send() {
            getEntityData().set(this.dataX, this.sentX = this.x);
            getEntityData().set(this.dataZ, this.sentZ = this.z);
            getEntityData().set(this.dataVelocityX, (float) this.velX);
            getEntityData().set(this.dataVelocityZ, (float) this.velZ);
        }

        private void adoptSynced() {
            this.x = getEntityData().get(this.dataX);
            this.z = getEntityData().get(this.dataZ);
            this.velX = getEntityData().get(this.dataVelocityX);
            this.velZ = getEntityData().get(this.dataVelocityZ);
        }

        private void correctTowardsSynced() {
            this.x = Mth.lerp(CLIENT_CORRECTION, this.x, getEntityData().get(this.dataX));
            this.z = Mth.lerp(CLIENT_CORRECTION, this.z, getEntityData().get(this.dataZ));
            this.velX = Mth.lerp(CLIENT_CORRECTION, this.velX, (double) getEntityData().get(this.dataVelocityX));
            this.velZ = Mth.lerp(CLIENT_CORRECTION, this.velZ, (double) getEntityData().get(this.dataVelocityZ));
        }

        private void read(CompoundTag swing) {
            CompoundTag tag = swing.getCompound(this.key);
            this.xO = this.x = tag.getFloat(KEY_ANGLE_X);
            this.zO = this.z = tag.getFloat(KEY_ANGLE_Z);
            this.velX = tag.getDouble(KEY_VELOCITY_X);
            this.velZ = tag.getDouble(KEY_VELOCITY_Z);
        }

        private void write(CompoundTag swing) {
            CompoundTag tag = new CompoundTag();
            tag.putFloat(KEY_ANGLE_X, this.x);
            tag.putFloat(KEY_ANGLE_Z, this.z);
            tag.putDouble(KEY_VELOCITY_X, this.velX);
            tag.putDouble(KEY_VELOCITY_Z, this.velZ);
            swing.put(this.key, tag);
        }
    }
}
