package net.grid.whisperinganchors.common.integration.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import net.grid.whisperinganchors.client.renderer.AnchorModelRenderer;
import net.grid.whisperinganchors.client.renderer.entity.AnchorEntityRenderer;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ContraptionAnchorRenderer {

    public static void render(MovementContext context, ContraptionAnchor anchor, ContraptionMatrices matrices, MultiBufferSource buffer) {
        PoseStack poseStack = new PoseStack();
        ContraptionMatrices.transform(poseStack, matrices.getViewProjection());
        ContraptionMatrices.transform(poseStack, matrices.getModel());

        int light = context.position == null ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(context.world, BlockPos.containing(context.position));
        float facingYaw = context.state.getValue(WhisperingAnchorBlock.FACING).toYRot();
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);

        AnchorEntityRenderer.renderSwing(anchor.getSwing(), Vec3.ZERO, facingYaw, partialTick, poseStack, buffer, light, AnchorModelRenderer.INTACT);
    }
}
