package net.grid.whisperinganchors.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.grid.whisperinganchors.client.renderer.AnchorModelRenderer;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public class AnchorEntityRenderer extends EntityRenderer<AnchorEntity> {

    public AnchorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AnchorEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Vec3 origin = entity.getPosition(partialTick);
        int destroyStage = AnchorModelRenderer.getDestroyStage(entity.getBoundPos(), true);

        renderSwing(entity, origin, entity.getFacingYaw(), partialTick, poseStack, bufferSource, packedLight, destroyStage);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public static void renderSwing(AnchorEntity entity, Vec3 origin, float facingYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int destroyStage) {
        float chainX = entity.getChainX(partialTick);
        float chainZ = entity.getChainZ(partialTick);
        Vec3 suspension = entity.getSuspensionPoint().subtract(origin);
        Vec3 shackle = entity.getShacklePos(chainX, chainZ).subtract(origin);

        poseStack.pushPose();
        poseStack.translate(suspension.x, suspension.y, suspension.z);
        poseStack.mulPose(Axis.ZP.rotation(chainX));
        poseStack.mulPose(Axis.XP.rotation(-chainZ));
        AnchorModelRenderer.renderChain(poseStack, bufferSource, packedLight, destroyStage);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(shackle.x, shackle.y, shackle.z);
        poseStack.mulPose(Axis.ZP.rotation(entity.getBodyX(partialTick)));
        poseStack.mulPose(Axis.XP.rotation(-entity.getBodyZ(partialTick)));
        AnchorModelRenderer.renderBody(poseStack, bufferSource, packedLight, destroyStage, facingYaw);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AnchorEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
