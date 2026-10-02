package net.grid.whisperinganchors.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.grid.whisperinganchors.client.renderer.AnchorModelRenderer;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public class AnchorEntityRenderer extends EntityRenderer<AnchorEntity> {

    public AnchorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AnchorEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float chainX = entity.getChainX(partialTick);
        float chainZ = entity.getChainZ(partialTick);

        double originX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double originY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double originZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        Vec3 suspension = entity.getSuspensionPoint();
        Vec3 shackle = entity.getShacklePos(chainX, chainZ);

        int destroyStage = AnchorModelRenderer.getDestroyStage(entity.getBoundPos(), true);

        poseStack.pushPose();
        poseStack.translate(suspension.x - originX, suspension.y - originY, suspension.z - originZ);
        poseStack.mulPose(Axis.ZP.rotation(chainX));
        poseStack.mulPose(Axis.XP.rotation(-chainZ));
        AnchorModelRenderer.renderChain(poseStack, bufferSource, packedLight, destroyStage);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(shackle.x - originX, shackle.y - originY, shackle.z - originZ);
        poseStack.mulPose(Axis.ZP.rotation(entity.getTiltX(partialTick)));
        poseStack.mulPose(Axis.XP.rotation(-entity.getTiltZ(partialTick)));
        AnchorModelRenderer.renderBody(poseStack, bufferSource, packedLight, destroyStage, entity.getFacingYaw());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AnchorEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
