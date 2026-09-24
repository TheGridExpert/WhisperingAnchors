package net.grid.whisperinganchors.misc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.grid.whisperinganchors.client.renderer.DyedGlint;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer whisperingAnchors$dyedFoilDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean hasFoil, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource unused, int packedLight, int packedOverlay, BakedModel model) {
        DyeColor color = hasFoil && isItem ? AnchorAttunement.colorOf(stack) : null;
        if (color == null) {
            return ItemRenderer.getFoilBufferDirect(bufferSource, renderType, isItem, hasFoil);
        }

        return VertexMultiConsumer.create(bufferSource.getBuffer(DyedGlint.item(color)), bufferSource.getBuffer(renderType));
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer whisperingAnchors$dyedFoil(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean hasFoil, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource unused, int packedLight, int packedOverlay, BakedModel model) {
        DyeColor color = hasFoil && isItem ? AnchorAttunement.colorOf(stack) : null;
        if (color == null) {
            return ItemRenderer.getFoilBuffer(bufferSource, renderType, isItem, hasFoil);
        }

        RenderType glint = Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet() ? DyedGlint.translucent(color) : DyedGlint.item(color);

        return VertexMultiConsumer.create(bufferSource.getBuffer(glint), bufferSource.getBuffer(renderType));
    }
}
