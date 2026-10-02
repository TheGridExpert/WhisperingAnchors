package net.grid.whisperinganchors.client.extension;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class HeedingCompassClientExtensions extends UseAnimationClientExtensions {

    private static final float RAISE_TICKS = 5.0F;
    private static final float RAISE_SIDEWAYS = -0.16F;
    private static final float RAISE_UP = 0.12F;
    private static final float RAISE_PITCH = -5.0F;

    private static final float SHAKE_AMPLITUDE = 28.0F;
    private static final float SHAKE_PERIOD = 4.7F;
    private static final float PIVOT_X = 0.026F;
    private static final float RIGHT_HAND_PIVOT_Y = 0.332F;
    private static final float LEFT_HAND_PIVOT_Y = 0.228F;

    @Override
    protected void transformFirstPerson(PoseStack poseStack, HumanoidArm arm, float used) {
        int side = getSideSign(arm);
        float raise = calculateRaiseProgress(used, RAISE_TICKS);
        float pivotY = arm == HumanoidArm.RIGHT ? RIGHT_HAND_PIVOT_Y : LEFT_HAND_PIVOT_Y;

        poseStack.translate(side * RAISE_SIDEWAYS * raise, RAISE_UP * raise, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(RAISE_PITCH * raise));
        poseStack.rotateAround(Axis.ZP.rotationDegrees(raise * calculateShakeAngle(used)), side * PIVOT_X, pivotY, 0.0F);
    }

    @Override
    protected void transformThirdPerson(PoseStack poseStack, HumanoidArm arm, float used) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(calculateRaiseProgress(used, RAISE_TICKS) * calculateShakeAngle(used)));
    }

    private static float calculateShakeAngle(float used) {
        float phase = Mth.frac(used / SHAKE_PERIOD + 0.25F);
        return SHAKE_AMPLITUDE * (1.0F - 4.0F * Math.abs(phase - 0.5F));
    }
}
