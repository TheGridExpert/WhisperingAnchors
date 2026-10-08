package net.grid.whisperinganchors.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.grid.whisperinganchors.client.renderer.AnchorModelRenderer;
import net.grid.whisperinganchors.common.core.ModConfigs;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.block.AnchorIdentity;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WhisperingAnchorRenderer implements BlockEntityRenderer<WhisperingAnchorBlockEntity> {

    private static final double NAME_TAG_RANGE_SQ = 64.0 * 64.0;
    private static final double NAME_TAG_LIFT = 0.5;
    private static final float NAME_TAG_SCALE = 0.025F;
    private static final int NAME_TAG_SHADOW_COLOR = 0x20FFFFFF;

    private final Font font;
    private final EntityRenderDispatcher entityRenderer;

    public WhisperingAnchorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(WhisperingAnchorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null || !ModConfigs.areAnchorsStatic()) return;

        BlockPos top = blockEntity.getBlockPos();
        Vec3 centreOfMass = AnchorEntity.getRestingCentreOfMass(top);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(centreOfMass));
        int destroyStage = AnchorModelRenderer.getDestroyStage(top, false);
        float facingYaw = blockEntity.getBlockState().getValue(WhisperingAnchorBlock.FACING).toYRot();

        double suspensionY = 1.0 - AnchorEntity.FIXED_CHAIN_LENGTH;

        poseStack.pushPose();
        poseStack.translate(0.5, suspensionY, 0.5);
        AnchorModelRenderer.renderChain(poseStack, bufferSource, light, destroyStage);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5, suspensionY - AnchorEntity.CHAIN_LENGTH, 0.5);
        AnchorModelRenderer.renderBody(poseStack, bufferSource, light, destroyStage, facingYaw);
        poseStack.popPose();

        Component name = blockEntity.getCustomName();
        if (name != null) {
            renderNameTag(level, AnchorIdentity.colorName(name, blockEntity.getColor()), centreOfMass, top, partialTick, poseStack, bufferSource, light);
        }
    }

    private void renderNameTag(Level level, Component name, Vec3 centreOfMass, BlockPos top, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        Vec3 anchorPoint = centreOfMass.add(0.0, AnchorEntity.BODY_TOP + NAME_TAG_LIFT, 0.0);
        if (this.entityRenderer.camera == null || this.entityRenderer.camera.getPosition().distanceToSqr(SubLevelHandler.INSTANCE.toWorldPosition(level, centreOfMass)) > NAME_TAG_RANGE_SQ) return;

        poseStack.pushPose();
        poseStack.translate(anchorPoint.x - top.getX(), anchorPoint.y - top.getY(), anchorPoint.z - top.getZ());
        poseStack.mulPose(SubLevelHandler.INSTANCE.getRenderOrientation(top, partialTick).conjugate());
        poseStack.mulPose(this.entityRenderer.cameraOrientation());
        poseStack.scale(NAME_TAG_SCALE, -NAME_TAG_SCALE, NAME_TAG_SCALE);

        Matrix4f pose = poseStack.last().pose();
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        float x = (float) (-this.font.width(name) / 2);
        this.font.drawInBatch(name, x, 0.0F, NAME_TAG_SHADOW_COLOR, false, pose, bufferSource, Font.DisplayMode.SEE_THROUGH, background, light);
        this.font.drawInBatch(name, x, 0.0F, -1, false, pose, bufferSource, Font.DisplayMode.NORMAL, 0, light);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WhisperingAnchorBlockEntity blockEntity) {
        BlockPos top = blockEntity.getBlockPos();
        return AABB.encapsulatingFullBlocks(top.below(WhisperingAnchorBlock.HEIGHT - 1), top.above());
    }
}
