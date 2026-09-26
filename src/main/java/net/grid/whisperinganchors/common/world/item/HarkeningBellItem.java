package net.grid.whisperinganchors.common.world.item;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.client.sound.HarkeningBellSoundInstance;
import net.grid.whisperinganchors.common.core.ModAdvancements;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.util.AnchorLanding;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.grid.whisperinganchors.common.world.particle.HarkeningSoulParticleOption;
import net.grid.whisperinganchors.misc.mixin.accessor.EndDragonFightAccessor;
import net.grid.whisperinganchors.misc.mixin.accessor.LivingEntityAccessor;
import net.minecraft.ChatFormatting;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HarkeningBellItem extends Item {

    public static final ResourceLocation ATTUNED_PROPERTY = WhisperingAnchors.modLoc("attuned");
    public static final ResourceLocation RINGING_PROPERTY = WhisperingAnchors.modLoc("ringing");

    public static final int USE_DURATION = 160;
    public static final int USE_COOLDOWN = 80;
    public static final int USE_COOLDOWN_UNSUCCESSFUL = 20;

    private static final float SOUL_SPIN = 0.18F;
    private static final float RINGER_SOUL_RADIUS = 0.7F;
    private static final float ANCHOR_SOUL_RADIUS = 1.0F;
    private static final double RINGER_SOUL_SPREAD = 0.3;
    private static final double ANCHOR_SOUL_DROP = 0.5;
    private static final double ANCHOR_SOUL_SPREAD = 0.4;
    private static final float MIN_SOULS_PER_TICK = 0.2F;
    private static final float MAX_SOULS_PER_TICK = 1.0F;

    public HarkeningBellItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level instanceof ServerLevel serverLevel) {
            AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
            if (attunement != null) {
                AnchorAttunement updated = attunement.tick(serverLevel);
                if (updated == null) {
                    stack.remove(ModDataComponents.ANCHOR_ATTUNEMENT);
                } else if (updated != attunement) {
                    stack.set(ModDataComponents.ANCHOR_ATTUNEMENT, updated);
                }
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);

        if (!state.is(ModBlocks.WHISPERING_ANCHOR)) {
            return super.useOn(context);
        } else {
            // TODO: Add attunement sound

            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                AnchorAttunement attunement = AnchorAttunement.of(level, pos, state);
                if (!player.hasInfiniteMaterials() && stack.getCount() == 1) {
                    stack.set(ModDataComponents.ANCHOR_ATTUNEMENT, attunement);
                } else {
                    ItemStack newStack = stack.transmuteCopy(ModItems.HARKENING_BELL.get(), 1);
                    stack.consume(1, player);
                    newStack.set(ModDataComponents.ANCHOR_ATTUNEMENT, attunement);
                    if (!player.getInventory().add(newStack)) {
                        player.drop(newStack, false);
                    }
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (stack.has(ModDataComponents.ANCHOR_ATTUNEMENT)) {
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        } else {
            return InteractionResultHolder.pass(stack);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        float progress = 1.0F - (float) remainingUseDuration / USE_DURATION;

        if (level.isClientSide) {
            if (remainingUseDuration == USE_DURATION) {
                HarkeningBellSoundInstance.play(livingEntity);
            }

            HarkeningSoulParticleOption souls = new HarkeningSoulParticleOption(RINGER_SOUL_RADIUS, SOUL_SPIN);
            for (int i = particleCountForTick(level.random, progress); i > 0; i--) {
                level.addParticle(souls, livingEntity.getX(), livingEntity.getY() + level.random.nextDouble() * RINGER_SOUL_SPREAD, livingEntity.getZ(), 0.0, 0.0, 0.0);
            }
        } else if (level instanceof ServerLevel serverLevel) {
            AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
            if (attunement != null) {
                tickTargetAnchor(serverLevel, attunement.anchorPos(), progress);
            }
        }
    }

    private static void tickTargetAnchor(ServerLevel level, GlobalPos anchorPos, float progress) {
        ServerLevel anchorLevel = level.getServer().getLevel(anchorPos.dimension());
        BlockPos top = anchorPos.pos();
        if (anchorLevel == null || !anchorLevel.isLoaded(top) || !(anchorLevel.getBlockEntity(top) instanceof WhisperingAnchorBlockEntity blockEntity)) return;

        AnchorEntity anchor = blockEntity.getAnchor();
        Vec3 centre = anchor == null ? AnchorEntity.restingCentreOfMass(top) : anchor.position();
        int spin = 1;
        if (anchor != null) {
            anchor.refreshOrbitDrive();
            spin = anchor.getOrbitDirection();
        }

        int count = particleCountForTick(anchorLevel.random, progress);
        if (count > 0) {
            anchorLevel.sendParticles(new HarkeningSoulParticleOption(ANCHOR_SOUL_RADIUS, spin * SOUL_SPIN), centre.x, centre.y - ANCHOR_SOUL_DROP, centre.z, count, 0.0, ANCHOR_SOUL_SPREAD, 0.0, 0.0);
        }
    }

    private static int particleCountForTick(RandomSource random, float progress) {
        float expected = Mth.lerp(1.0F - progress * progress, MIN_SOULS_PER_TICK, MAX_SOULS_PER_TICK);
        int count = (int) expected;

        return random.nextFloat() < expected - count ? count + 1 : count;
    }

    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return Objects.equals(oldStack.get(ModDataComponents.ANCHOR_ATTUNEMENT), newStack.get(ModDataComponents.ANCHOR_ATTUNEMENT)) && super.canContinueUsing(oldStack, newStack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return stack;

        AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
        if (attunement == null) return stack;

        GlobalPos anchorPos = attunement.anchorPos();
        ServerLevel anchorLevel = serverLevel.getServer().getLevel(anchorPos.dimension());
        if (anchorLevel == null) return stack;

        Optional<AnchorLanding> landing = AnchorLanding.find(anchorLevel, anchorPos.pos(), livingEntity);
        if (landing.isEmpty()) {
            if (livingEntity instanceof Player player) {
                player.getCooldowns().addCooldown(this, USE_COOLDOWN_UNSUCCESSFUL);
            }
            return stack;
        }

        // TODO: Add teleport sound

        if (livingEntity.isPassenger()) {
            livingEntity.stopRiding();
        }

        boolean savedFromLethalFall = livingEntity instanceof ServerPlayer serverPlayer && isFallingToDeath(serverPlayer);
        boolean dragonIsAlive = serverLevel.dimension() == Level.END && anchorLevel.dimension() != Level.END && isDragonAlive(serverLevel);

        AnchorLanding destination = landing.get();
        Entity arrived = livingEntity.changeDimension(new DimensionTransition(anchorLevel, destination.position(), Vec3.ZERO, destination.yRot(), destination.xRot(), DimensionTransition.DO_NOTHING));
        if (arrived == null) return stack;

        arrived.resetFallDistance();
        if (arrived instanceof Player player) {
            player.getCooldowns().addCooldown(this, USE_COOLDOWN);

            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.HARKENED_TO_ANCHOR.get().trigger(serverPlayer, level.dimension(), savedFromLethalFall, dragonIsAlive);
            }
        }

        return stack;
    }

    private static boolean isDragonAlive(ServerLevel level) {
        EndDragonFight fight = level.getDragonFight();
        return fight != null && fight.getDragonUUID() != null && !((EndDragonFightAccessor) fight).whispering_anchors$isDragonKilled();
    }

    private static boolean isFallingToDeath(ServerPlayer player) {
        if (player.onGround() || player.getDeltaMovement().y >= 0.0 || player.isFallFlying() || player.getAbilities().flying || player.isInWater() || player.hasEffect(MobEffects.SLOW_FALLING)) {
            return false;
        }

        Level level = player.level();
        BlockPos.MutableBlockPos cursor = player.blockPosition().mutable();
        while (true) {
            if (cursor.getY() <= level.getMinBuildHeight()) {
                return true;
            }

            BlockState state = level.getBlockState(cursor);
            if (state.is(BlockTags.FALL_DAMAGE_RESETTING) || !level.getFluidState(cursor).isEmpty()) {
                return false;
            }

            VoxelShape floor = state.getCollisionShape(level, cursor);
            if (!floor.isEmpty() && cursor.getY() + floor.max(Direction.Axis.Y) <= player.getY()) {
                float fallDistance = player.fallDistance + (float) (player.getY() - (cursor.getY() + floor.max(Direction.Axis.Y)));
                LivingEntityAccessor accessor = (LivingEntityAccessor) player;
                float damage = accessor.whispering_anchors$getDamageAfterMagicAbsorb(player.damageSources().fall(), accessor.whispering_anchors$calculateFallDamage(fallDistance, 1.0F));

                return damage >= player.getHealth() + player.getAbsorptionAmount();
            }

            cursor.move(Direction.DOWN);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
        if (attunement != null) {
            tooltipComponents.add(Component.translatable("tooltip.whispering_anchors.harkening_bell.attuned", attunement.name()).withStyle(ChatFormatting.GRAY));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.ANCHOR_ATTUNEMENT) || super.isFoil(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CUSTOM;
    }
}
