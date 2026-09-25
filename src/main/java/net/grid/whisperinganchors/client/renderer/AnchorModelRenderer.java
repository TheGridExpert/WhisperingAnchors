package net.grid.whisperinganchors.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.grid.whisperinganchors.client.WhisperingAnchorsClient;
import net.grid.whisperinganchors.client.model.AnchorRenderSettings;
import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.grid.whisperinganchors.misc.mixin.accessor.LevelRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.SortedSet;

public final class AnchorModelRenderer {

    public static final int INTACT = -1;

    private AnchorModelRenderer() {
    }

    public static void renderChain(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int destroyStage) {
        AnchorRenderSettings offsets = AnchorRenderSettings.current();
        BakedModel link = model(WhisperingAnchorsClient.ANCHOR_CHAIN_MODEL);
        int links = Math.max(1, Mth.ceil(AnchorEntity.CHAIN_LENGTH / offsets.chainLinkSpacing()));

        for (int i = 0; i < links; i++) {
            poseStack.pushPose();
            poseStack.translate(-0.5, -offsets.chainPivot() - i * offsets.chainLinkSpacing(), -0.5);
            renderModel(link, poseStack, bufferSource, packedLight, destroyStage);
            poseStack.popPose();
        }
    }

    public static void renderBody(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int destroyStage, float facingYaw) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-facingYaw));
        poseStack.translate(-0.5, -AnchorRenderSettings.current().shackleHeight(), -0.5);
        renderModel(model(WhisperingAnchorsClient.ANCHOR_BODY_MODEL), poseStack, bufferSource, packedLight, destroyStage);
        poseStack.popPose();
    }

    public static int destroyStageOf(BlockPos top, boolean includeTop) {
        Long2ObjectMap<SortedSet<BlockDestructionProgress>> destruction = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getDestructionProgress();
        if (destruction.isEmpty()) return INTACT;

        int stage = INTACT;
        for (AnchorPart part : AnchorPart.values()) {
            if (part == AnchorPart.TOP && !includeTop) continue;

            SortedSet<BlockDestructionProgress> progress = destruction.get(top.below(part.offsetFromTop()).asLong());
            if (progress != null && !progress.isEmpty()) {
                stage = Math.max(stage, progress.last().getProgress());
            }
        }

        return stage;
    }

    private static BakedModel model(ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    private static void renderModel(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int destroyStage) {
        ModelBlockRenderer renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        renderer.renderModel(poseStack.last(), bufferSource.getBuffer(Sheets.cutoutBlockSheet()), null, model, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());

        if (destroyStage == INTACT) return;

        VertexConsumer cracks = new SheetedDecalTextureGenerator(Minecraft.getInstance().renderBuffers().crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(destroyStage)), poseStack.last(), 1.0F);
        renderer.renderModel(poseStack.last(), cracks, null, model, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
    }
}
