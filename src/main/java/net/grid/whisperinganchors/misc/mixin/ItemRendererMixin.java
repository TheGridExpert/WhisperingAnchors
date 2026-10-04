package net.grid.whisperinganchors.misc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.grid.whisperinganchors.client.renderer.BellGlint;
import net.grid.whisperinganchors.client.renderer.BellOverlay;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer whispering_anchors$bellFoilDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean hasFoil, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource unused, int packedLight, int packedOverlay, BakedModel model) {
        if (!hasFoil || !isItem || !stack.has(ModDataComponents.ANCHOR_ATTUNEMENT)) {
            return ItemRenderer.getFoilBufferDirect(bufferSource, renderType, isItem, hasFoil);
        }

        return VertexMultiConsumer.create(bufferSource.getBuffer(BellGlint.RENDER_TYPE), bufferSource.getBuffer(renderType));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V", shift = At.Shift.AFTER))
    private void whispering_anchors$renderBellOverlay(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BakedModel model, CallbackInfo ci) {
        BellOverlay.render(stack, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
