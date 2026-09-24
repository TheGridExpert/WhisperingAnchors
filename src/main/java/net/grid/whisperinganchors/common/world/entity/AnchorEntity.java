package net.grid.whisperinganchors.common.world.entity;

import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
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

public class AnchorEntity extends Entity {

    public static final String KEY_BOUND_POS = "BoundPos";
    public static final String KEY_COLOR = "Color";
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

    private static final double CHAIN_RESTORING = GRAVITY / CHAIN_LENGTH;
    private static final double TILT_RESTORING = GRAVITY * SHACKLE_TO_CENTRE / GYRATION_RADIUS_SQ;
    private static final double TILT_TO_CHAIN = SHACKLE_TO_CENTRE / CHAIN_LENGTH;

    private static final double SWING_DRAG = 0.997;
    private static final double SHACKLE_FRICTION = 0.40;
    private static final float MAX_ANGLE = (float) Math.toRadians(70.0);

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

    private float chainX;
    private float chainZ;
    private float tiltX;
    private float tiltZ;
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

    public AnchorEntity(EntityType<? extends AnchorEntity> type, Level level) {
        super(type, level);
    }

    public @Nullable static AnchorEntity spawnFor(ServerLevel level, BlockPos topPos) {
        AnchorEntity anchor = ModEntities.ANCHOR.get().create(level);
        if (anchor == null) return null;

        anchor.setBoundPos(topPos);
        anchor.setPos(anchor.centreOfMass(0.0F, 0.0F, 0.0F, 0.0F));
        level.addFreshEntity(anchor);

        return anchor;
    }

    public static AABB searchArea(BlockPos top) {
        return new AABB(top).inflate(SEARCH_RADIUS);
    }

    public static List<AnchorEntity> boundTo(Level level, BlockPos top) {
        return level.getEntitiesOfClass(AnchorEntity.class, searchArea(top), anchor -> anchor.isBoundTo(top));
    }

    public Vec3 getSuspensionPoint() {
        BlockPos bound = getBoundPos();
        return new Vec3(bound.getX() + 0.5, bound.getY() + 1.0 - FIXED_CHAIN_LENGTH, bound.getZ() + 0.5);
    }

    private static Vec3 hang(double length, float angleX, float angleZ) {
        double cosZ = Math.cos(angleZ);
        return new Vec3(length * Math.sin(angleX) * cosZ, -length * Math.cos(angleX) * cosZ, length * Math.sin(angleZ));
    }

    public Vec3 shacklePos(float chainX, float chainZ) {
        return getSuspensionPoint().add(hang(CHAIN_LENGTH, chainX, chainZ));
    }

    public Vec3 centreOfMass(float chainX, float chainZ, float tiltX, float tiltZ) {
        return shacklePos(chainX, chainZ).add(hang(SHACKLE_TO_CENTRE, tiltX, tiltZ));
    }

    public Vec3 randomBodyPoint(RandomSource random) {
        AABB body = getBoundingBox();
        return new Vec3(getX() + random.nextGaussian() * body.getXsize() * CRUMBLE_SPREAD, Mth.lerp(random.nextDouble(), body.minY, body.maxY), getZ() + random.nextGaussian() * body.getZsize() * CRUMBLE_SPREAD);
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
                tickPendulum();
                correctTowardsSynced();
            }
        } else {
            if (!isStillAnchored()) {
                crumble();
                discard();
                return;
            }

            if (this.impactSoundCooldown > 0) {
                this.impactSoundCooldown--;
            }

            absorbPush();
            tickPendulum();
            collideWithNearbyAnchors();
            syncState();
        }

        setPos(centreOfMass(this.chainX, this.chainZ, this.tiltX, this.tiltZ));
    }

    private boolean isStillAnchored() {
        BlockState state = level().getBlockState(getBoundPos());
        return state.getBlock() instanceof WhisperingAnchorBlock && state.getValue(WhisperingAnchorBlock.PART) == AnchorPart.TOP;
    }

    private void crumble() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        AABB body = getBoundingBox();
        double rowHeight = body.getYsize() / CRUMBLE_ROWS;

        for (int row = 0; row < CRUMBLE_ROWS; row++) {
            double y = body.minY + (row + 0.5) * rowHeight;
            serverLevel.sendParticles(ModParticles.ANCHOR_CRUMBLE.get(), getX(), y, getZ(), CRUMBLE_PARTICLES_PER_ROW, body.getXsize() * CRUMBLE_SPREAD, rowHeight * CRUMBLE_SPREAD, body.getZsize() * CRUMBLE_SPREAD, 0.0);
        }
    }

    private void absorbPush() {
        Vec3 push = getDeltaMovement();
        double speed = Math.sqrt(push.x * push.x + push.z * push.z);
        if (speed < 1.0E-4) return;

        setDeltaMovement(Vec3.ZERO);

        applyImpulse(push.x / speed, push.z / speed, getY(), speed * VICTIM_MASS_RATIO);
    }

    private void tickPendulum() {
        double couplingX = TILT_RESTORING * Math.sin(this.chainX - this.tiltX);
        double couplingZ = TILT_RESTORING * Math.sin(this.chainZ - this.tiltZ);

        this.tiltVelX += couplingX;
        this.tiltVelZ += couplingZ;
        this.chainVelX -= CHAIN_RESTORING * Math.sin(this.chainX) + TILT_TO_CHAIN * couplingX;
        this.chainVelZ -= CHAIN_RESTORING * Math.sin(this.chainZ) + TILT_TO_CHAIN * couplingZ;

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

        this.chainX += (float) this.chainVelX;
        this.chainZ += (float) this.chainVelZ;
        this.tiltX += (float) this.tiltVelX;
        this.tiltZ += (float) this.tiltVelZ;

        if (Math.abs(this.chainX) > MAX_ANGLE) {
            this.chainX = Math.copySign(MAX_ANGLE, this.chainX);
            this.chainVelX = 0.0;
        }
        if (Math.abs(this.chainZ) > MAX_ANGLE) {
            this.chainZ = Math.copySign(MAX_ANGLE, this.chainZ);
            this.chainVelZ = 0.0;
        }
        if (Math.abs(this.tiltX) > MAX_ANGLE) {
            this.tiltX = Math.copySign(MAX_ANGLE, this.tiltX);
            this.tiltVelX = 0.0;
        }
        if (Math.abs(this.tiltZ) > MAX_ANGLE) {
            this.tiltZ = Math.copySign(MAX_ANGLE, this.tiltZ);
            this.tiltVelZ = 0.0;
        }

        if (isAtRest()) {
            this.chainX = this.chainZ = this.tiltX = this.tiltZ = 0.0F;
            this.chainVelX = this.chainVelZ = this.tiltVelX = this.tiltVelZ = 0.0;
        }
    }

    private boolean isAtRest() {
        return Math.abs(this.chainX) < REST_ANGLE_EPSILON && Math.abs(this.chainZ) < REST_ANGLE_EPSILON
                && Math.abs(this.tiltX) < REST_ANGLE_EPSILON && Math.abs(this.tiltZ) < REST_ANGLE_EPSILON
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

        for (AnchorEntity other : level().getEntitiesOfClass(AnchorEntity.class, getBoundingBox(), candidate -> candidate != this)) {
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
        double thisContact = shacklePos(this.chainX, this.chainZ).y - contactY;
        double otherContact = other.shacklePos(other.chainX, other.chainZ).y - contactY;

        double closingSpeed = other.contactSpeedAlong(normalX, normalZ, otherContact) - contactSpeedAlong(normalX, normalZ, thisContact);
        if (closingSpeed <= 0.0) return;

        double impulse = (1.0 + COLLISION_RESTITUTION) * closingSpeed / (inverseEffectiveMass(thisContact) + inverseEffectiveMass(otherContact));
        applyImpulse(normalX, normalZ, contactY, impulse);
        other.applyImpulse(-normalX, -normalZ, contactY, impulse);

        if (closingSpeed > SILENT_IMPACT_SPEED && other.impactSoundCooldown <= 0) {
            playImpactSound(0.85F, true);
            other.impactSoundCooldown = this.impactSoundCooldown;
        }
    }

    private Vec3 contactVelocity(double belowShackle) {
        return new Vec3(CHAIN_LENGTH * this.chainVelX + belowShackle * this.tiltVelX, 0.0, CHAIN_LENGTH * this.chainVelZ + belowShackle * this.tiltVelZ);
    }

    private double contactSpeedAlong(double normalX, double normalZ, double belowShackle) {
        Vec3 velocity = contactVelocity(belowShackle);
        return velocity.x * normalX + velocity.z * normalZ;
    }

    private static double inverseEffectiveMass(double belowShackle) {
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

        BlockHitResult hit = asBlockClick(bound, player);
        ItemStack stack = player.getItemInHand(hand);

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

    private static BlockHitResult asBlockClick(BlockPos pos, Player player) {
        Vec3 centre = Vec3.atCenterOf(pos);
        Direction face = Direction.getNearest(player.getEyePosition().subtract(centre));

        return new BlockHitResult(centre.relative(face, 0.5), face, pos, false);
    }

    @Override
    public void push(Entity entity) {
        if (level().isClientSide() || entity == this) return;

        double dx = getX() - entity.getX();
        double dz = getZ() - entity.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) return;

        AABB self = getBoundingBox();
        AABB other = entity.getBoundingBox();
        double contactY = (Math.min(self.maxY, other.maxY) + Math.max(self.minY, other.minY)) * 0.5;

        double towardsAnchorX = dx / length;
        double towardsAnchorZ = dz / length;
        applyImpulse(towardsAnchorX, towardsAnchorZ, contactY, WALK_SPECIFIC_IMPULSE);
        strike(entity, -towardsAnchorX, -towardsAnchorZ, contactY);
    }

    private void strike(Entity target, double normalX, double normalZ, double contactY) {
        if (!(target instanceof LivingEntity victim)) return;

        double belowShackle = shacklePos(this.chainX, this.chainZ).y - contactY;
        double anchorSpeedTowards = contactSpeedAlong(normalX, normalZ, belowShackle);
        if (anchorSpeedTowards <= HARMLESS_IMPACT_SPEED) return;

        Vec3 swing = contactVelocity(belowShackle);
        double swingSpeed = swing.length();
        if (swingSpeed < 1.0E-4) return;

        double pushX = swing.x / swingSpeed;
        double pushZ = swing.z / swingSpeed;

        double effort = contactVelocity(SHACKLE_TO_CENTRE).length() / MAX_SWING_SPEED;
        float damage = (float) Mth.clamp((effort - DAMAGE_SPEED_FRACTION) / (1.0 - DAMAGE_SPEED_FRACTION) * MAX_IMPACT_DAMAGE, 0.0, MAX_IMPACT_DAMAGE);
        if (damage > 0.0F) {
            if (!victim.hurt(damageSources().source(ModDamageTypes.ANCHOR_SWING, this), damage)) return;

            playImpactSound(1.0F, true);
        }

        Vec3 motion = target.getDeltaMovement();
        double approach = Mth.clamp(anchorSpeedTowards - (motion.x * normalX + motion.z * normalZ), 0.0, anchorSpeedTowards);
        double impulse = (1.0 + IMPACT_RESTITUTION) * approach / (inverseEffectiveMass(belowShackle) + 1.0 / VICTIM_MASS_RATIO);
        applyImpulse(-pushX, -pushZ, contactY, impulse);
        victim.knockback(impulse / VICTIM_MASS_RATIO, -pushX, -pushZ);
        victim.hurtMarked = true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!level().isClientSide() && !isRemoved() && attacker instanceof LivingEntity living) {
            struckBy(living);
        }

        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved()) return false;

        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            struckBy(attacker);
        } else if (source.getDirectEntity() instanceof Projectile projectile) {
            struckBy(projectile);
        }

        return true;
    }

    private void struckBy(Projectile projectile) {
        Vec3 flight = projectile.getDeltaMovement();
        double speed = Math.sqrt(flight.x * flight.x + flight.z * flight.z);
        if (speed < 1.0E-4) return;

        applyImpulse(flight.x / speed, flight.z / speed, projectile.getY(), speed * PROJECTILE_MASS_RATIO);
        playImpactSound(1.0F, false);
    }

    private void struckBy(LivingEntity attacker) {
        Vec3 look = attacker.getLookAngle();
        double length = Math.sqrt(look.x * look.x + look.z * look.z);
        if (length >= 1.0E-4) {
            Vec3 eye = attacker.getEyePosition();
            double contactY = getBoundingBox().clip(eye, eye.add(look.scale(6.0))).map(hit -> hit.y).orElse(getY());

            applyImpulse(look.x / length, look.z / length, contactY, HIT_SPECIFIC_IMPULSE);
        }

        playImpactSound(1.0F, false);
    }

    private void applyImpulse(double dirX, double dirZ, double contactY, double specificImpulse) {
        double armFromCentre = shacklePos(this.chainX, this.chainZ).y - contactY - SHACKLE_TO_CENTRE;
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

    public @Nullable DyeColor getColor() {
        int id = this.entityData.get(DATA_COLOR);
        return id == UNDYED ? null : DyeColor.byId(id);
    }

    public void setColor(@Nullable DyeColor color) {
        this.entityData.set(DATA_COLOR, color == null ? UNDYED : color.getId());
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
}
