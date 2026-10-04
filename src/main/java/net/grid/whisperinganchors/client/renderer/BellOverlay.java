package net.grid.whisperinganchors.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class BellOverlay {

    public static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(WhisperingAnchors.modLoc("item/harkening_bell_overlay"));

    public static final RenderType RENDER_TYPE = RenderType.create("whispering_anchors_bell_overlay", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setOverlayState(RenderStateShard.OVERLAY)
            .createCompositeState(true));

    private static final long QUAD_SEED = 42L;

    public static void render(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!stack.is(ModItems.HARKENING_BELL)) return;

        AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
        if (attunement == null || attunement.anchorColor().isEmpty()) return;

        int rgb = attunement.anchorColor().get().getTextureDiffuseColor();
        float red = FastColor.ARGB32.red(rgb) / 255.0F;
        float green = FastColor.ARGB32.green(rgb) / 255.0F;
        float blue = FastColor.ARGB32.blue(rgb) / 255.0F;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL);
        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create();

        for (Direction direction : Direction.values()) {
            random.setSeed(QUAD_SEED);
            putQuads(consumer, pose, model, direction, random, red, green, blue, packedLight, packedOverlay);
        }

        random.setSeed(QUAD_SEED);
        putQuads(consumer, pose, model, null, random, red, green, blue, packedLight, packedOverlay);
    }

    @SuppressWarnings("deprecation")
    private static void putQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedModel model, @Nullable Direction direction, RandomSource random, float red, float green, float blue, int packedLight, int packedOverlay) {
        for (BakedQuad quad : model.getQuads(null, direction, random)) {
            consumer.putBulkData(pose, quad, red, green, blue, 1.0F, packedLight, packedOverlay, true);
        }
    }
}
