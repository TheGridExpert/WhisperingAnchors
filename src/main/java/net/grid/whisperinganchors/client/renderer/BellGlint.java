package net.grid.whisperinganchors.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.joml.Matrix4f;

public final class BellGlint {

    private static final float GLINT_SCALE = 8.0F;
    private static final double GLINT_SCROLL_SPEED = 8.0;
    private static final long SLOW_CYCLE_MILLIS = 110000L;
    private static final long FAST_CYCLE_MILLIS = 30000L;
    private static final float GLINT_SKEW = (float) (Math.PI / 18);

    private static final float GLINT_INTENSITY = 0.525F;

    private static final ResourceLocation TEXTURE = WhisperingAnchors.modLoc("textures/misc/bell_glint.png");

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_GLINT = new RenderStateShard.TransparencyStateShard(
            "whispering_anchors_glint_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    public static final RenderType RENDER_TYPE = glint("whispering_anchors_bell_glint", AnchorAttunement.DEFAULT_COLOR);

    private static RenderType glint(String name, DyeColor color) {
        RenderType.CompositeState.CompositeStateBuilder state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, true, false))
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                .setTransparencyState(ADDITIVE_GLINT)
                .setTexturingState(texturing(name, color));

        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, state.createCompositeState(false));
    }

    private static RenderStateShard.TexturingStateShard texturing(String name, DyeColor color) {
        int rgb = color.getTextureDiffuseColor();
        float red = GLINT_INTENSITY * (rgb >> 16 & 0xFF) / 255.0F;
        float green = GLINT_INTENSITY * (rgb >> 8 & 0xFF) / 255.0F;
        float blue = GLINT_INTENSITY * (rgb & 0xFF) / 255.0F;

        return new RenderStateShard.TexturingStateShard(name, () -> {
            long scroll = (long) ((double) Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * GLINT_SCROLL_SPEED);
            float slow = (float) (scroll % SLOW_CYCLE_MILLIS) / SLOW_CYCLE_MILLIS;
            float fast = (float) (scroll % FAST_CYCLE_MILLIS) / FAST_CYCLE_MILLIS;
            Matrix4f matrix = new Matrix4f().translation(-slow, fast, 0.0F);

            RenderSystem.setTextureMatrix(matrix.rotateZ(GLINT_SKEW).scale(GLINT_SCALE));
            RenderSystem.setShaderColor(red, green, blue, 1.0F);
        }, () -> {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.resetTextureMatrix();
        });
    }
}
