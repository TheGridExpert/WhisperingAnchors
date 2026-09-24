package net.grid.whisperinganchors.common.world.item;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.client.sound.HarkeningBellSoundInstance;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.util.AnchorLanding;
import net.minecraft.ChatFormatting;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
        if (level.isClientSide && remainingUseDuration == USE_DURATION) {
            HarkeningBellSoundInstance.startFor(livingEntity);
        }
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

        AnchorLanding destination = landing.get();
        Entity arrived = livingEntity.changeDimension(new DimensionTransition(anchorLevel, destination.position(), Vec3.ZERO, destination.yRot(), destination.xRot(), DimensionTransition.DO_NOTHING));
        if (arrived == null) return stack;

        arrived.resetFallDistance();
        if (arrived instanceof Player player) {
            player.getCooldowns().addCooldown(this, USE_COOLDOWN);
        }

        return stack;
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
