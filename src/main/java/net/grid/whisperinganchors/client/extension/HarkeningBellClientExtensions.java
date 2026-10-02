package net.grid.whisperinganchors.client.extension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.grid.whisperinganchors.common.world.item.HarkeningBellItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

import javax.annotation.Nullable;

public class HarkeningBellClientExtensions extends UseAnimationClientExtensions {

    public static final EnumProxy<HumanoidModel.ArmPose> RINGING_ARM_POSE = new EnumProxy<>(HumanoidModel.ArmPose.class, false, (IArmPoseTransformer) HarkeningBellClientExtensions::poseUsingArm);

    private static final float RAISE_TICKS = 6.0F;
    private static final float RAISE_SIDEWAYS = -0.24F;
    private static final float RAISE_UP = 0.20F;
    private static final float RAISE_FORWARD = -0.12F;
    private static final float RAISE_PITCH = -30.0F;
    private static final float RAISE_YAW = 22.0F;
    private static final float RAISE_TURN = 45.0F;

    private static final float ARM_PITCH = -122.0F;
    private static final float ARM_SWING_SCALE = 0.45F;
    private static final float BELL_SWING_SCALE = 1.0F;
    private static final float BELL_PIVOT_DROP = 2.4F;
    private static final float ARM_YAW = -18.0F;
    private static final float ARM_ROLL = -14.0F;

    private static final float RING_AMPLITUDE = 34.0F;
    private static final float TICKS_PER_SECOND = 20.0F;
    private static final float[] RING_TIMES = {
            0.300F, 0.650F,
            1.300F, 1.675F,
            2.400F, 2.800F,
            3.500F, 3.950F,
            4.600F, 5.000F,
            5.845F
    };
    private static final float RING_BOB = 0.025F;
    private static final float RING_PIVOT_RISE = 0.25F;
    private static final float SPRITE_HEIGHT = 0.68F;

    @Override
    protected void transformFirstPerson(PoseStack poseStack, HumanoidArm arm, float used) {
        int side = getSideSign(arm);
        float raise = calculateRaiseProgress(used, RAISE_TICKS);
        float swing = raise * calculateBellSwingAngle(used);

        poseStack.translate(side * RAISE_SIDEWAYS * raise, RAISE_UP * raise, RAISE_FORWARD * raise);

        poseStack.mulPose(Axis.XP.rotationDegrees(RAISE_PITCH * raise));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * RAISE_YAW * raise));

        poseStack.translate(0.0F, RING_BOB * Math.abs(swing) / RING_AMPLITUDE, 0.0F);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(side * swing), 0.0F, RING_PIVOT_RISE * SPRITE_HEIGHT, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(RAISE_TURN * raise));
    }

    @Override
    protected void transformThirdPerson(PoseStack poseStack, HumanoidArm arm, float used) {
        float swing = calculateRaiseProgress(used, RAISE_TICKS) * calculateBellSwingAngle(used);

        poseStack.rotateAround(Axis.ZP.rotationDegrees(getSideSign(arm) * swing * BELL_SWING_SCALE), 0.0F, -BELL_PIVOT_DROP / 16.0F, 0.0F);
    }

    @Override
    public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
        boolean ringing = entityLiving.isUsingItem() && entityLiving.getUseItemRemainingTicks() > 0 && entityLiving.getUsedItemHand() == hand;
        return ringing ? RINGING_ARM_POSE.getValue() : null;
    }

    private static void poseUsingArm(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float used = getUsedTicks(entity);
        float raise = calculateRaiseProgress(used, RAISE_TICKS);
        float swing = raise * calculateBellSwingAngle(used);

        ModelPart limb = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        int side = getSideSign(arm);

        limb.xRot = Mth.lerp(raise, limb.xRot, (float) Math.toRadians(ARM_PITCH));
        limb.yRot = Mth.lerp(raise, limb.yRot, side * (float) Math.toRadians(ARM_YAW));
        limb.zRot = Mth.lerp(raise, limb.zRot, side * (float) Math.toRadians(ARM_ROLL)) + side * (float) Math.toRadians(swing * ARM_SWING_SCALE);
    }

    private static float calculateBellSwingAngle(float used) {
        float seconds = used / TICKS_PER_SECOND;

        if (seconds <= RING_TIMES[0]) {
            return RING_AMPLITUDE * (1.0F - Mth.cos((float) Math.PI * seconds / RING_TIMES[0])) * 0.5F;
        }

        for (int ring = 0; ring < RING_TIMES.length - 1; ring++) {
            if (seconds <= RING_TIMES[ring + 1]) {
                float phase = (seconds - RING_TIMES[ring]) / (RING_TIMES[ring + 1] - RING_TIMES[ring]);
                return RING_AMPLITUDE * getSwingDirection(ring) * Mth.cos((float) Math.PI * phase);
            }
        }

        int last = RING_TIMES.length - 1;
        float tail = Math.max(0.05F, HarkeningBellItem.USE_DURATION / TICKS_PER_SECOND - RING_TIMES[last]);
        float phase = Mth.clamp((seconds - RING_TIMES[last]) / tail, 0.0F, 1.0F);

        return RING_AMPLITUDE * getSwingDirection(last) * (1.0F + Mth.cos((float) Math.PI * phase)) * 0.5F;
    }

    private static int getSwingDirection(int ring) {
        return ring % 2 == 0 ? 1 : -1;
    }
}
