package net.grid.whisperinganchors.client.extension;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public abstract class UseAnimationClientExtensions implements IClientItemExtensions {

    private static final float REST_SIDEWAYS = 0.56F;
    private static final float REST_UP = -0.52F;
    private static final float REST_EQUIP_DROP = -0.6F;
    private static final float REST_FORWARD = -0.72F;

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
        if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0 || player.getUseItem() != itemInHand) {
            return false;
        }

        poseStack.translate(getSideSign(arm) * REST_SIDEWAYS, REST_UP + equipProcess * REST_EQUIP_DROP, REST_FORWARD);
        transformFirstPerson(poseStack, arm, player.getTicksUsingItem() + partialTick);

        return true;
    }

    public static void transformHeldItem(LivingEntity holder, ItemStack stack, HumanoidArm arm, PoseStack poseStack) {
        if (holder.isUsingItem() && holder.getUseItem() == stack && IClientItemExtensions.of(stack) instanceof UseAnimationClientExtensions extensions) {
            extensions.transformThirdPerson(poseStack, arm, getUsedTicks(holder));
        }
    }

    protected abstract void transformFirstPerson(PoseStack poseStack, HumanoidArm arm, float used);

    protected abstract void transformThirdPerson(PoseStack poseStack, HumanoidArm arm, float used);

    protected static float getUsedTicks(LivingEntity entity) {
        return entity.getTicksUsingItem() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    protected static float calculateRaiseProgress(float used, float raiseTicks) {
        float t = Mth.clamp(used / raiseTicks, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    protected static int getSideSign(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? 1 : -1;
    }
}
