package net.grid.whisperinganchors.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL14;

import java.util.EnumMap;
import java.util.Map;

public final class DyedGlint {

    private static final float GLINT_SCALE = 8.0F;
    private static final double GLINT_SCROLL_SPEED = 8.0;
    private static final long SLOW_CYCLE_MILLIS = 110000L;
    private static final long FAST_CYCLE_MILLIS = 30000L;
    private static final float GLINT_SKEW = (float) (Math.PI / 18);

    private static final double GLINT_FLOOR = 33.0 / 255.0;
    private static final double GLINT_PEAK = 202.0 / 255.0;

    private static final double GLINT_NEUTRAL_BIAS = 0.25;
    private static final double GLINT_NEUTRAL = GLINT_FLOOR + (GLINT_PEAK - GLINT_FLOOR) * GLINT_NEUTRAL_BIAS;

    private static final double GLINT_SWING = 0.5;

    private static final double GLINT_BRIGHTNESS = 1.0;
    private static final double GLINT_SATURATION = 0.35;
    private static final double GLINT_VALUE = 0.0;

    private static final ResourceLocation TEXTURE = WhisperingAnchors.modLoc("textures/misc/bell_glint.png");

    private static final RenderStateShard.TransparencyStateShard MODULATING_GLINT = new RenderStateShard.TransparencyStateShard(
            "whispering_anchors_glint_transparency",
            () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.CONSTANT_COLOR, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            },
            () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private static final Map<DyeColor, RenderType> ITEM = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, RenderType> TRANSLUCENT = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            ITEM.put(color, glint("whispering_anchors_glint_" + color.getName(), color, false));
            TRANSLUCENT.put(color, glint("whispering_anchors_glint_translucent_" + color.getName(), color, true));
        }
    }

    public static void registerBuffers(RegisterRenderBuffersEvent event) {
        ITEM.values().forEach(event::registerRenderBuffer);
        TRANSLUCENT.values().forEach(event::registerRenderBuffer);
    }

    public static RenderType item(DyeColor color) {
        return ITEM.get(color);
    }

    public static RenderType translucent(DyeColor color) {
        return TRANSLUCENT.get(color);
    }

    private static float glintStrength(int channel, int rgb) {
        int peak = Math.max(rgb >> 16 & 0xFF, Math.max(rgb >> 8 & 0xFF, rgb & 0xFF));
        if (peak == 0) return 0.0F;

        double hue = (double) channel / peak;
        double value = 1.0 - GLINT_VALUE * (1.0 - peak / 255.0);
        double target = GLINT_SWING * value * (1.0 - GLINT_SATURATION * (1.0 - hue));

        return (float) (Math.max(target, 0.0) / (GLINT_PEAK - GLINT_FLOOR));
    }

    private static float glintOffset(float swing) {
        return (float) (GLINT_BRIGHTNESS - GLINT_NEUTRAL * swing);
    }

    private static RenderType glint(String name, DyeColor color, boolean translucent) {
        RenderType.CompositeState.CompositeStateBuilder state = RenderType.CompositeState.builder()
                .setShaderState(translucent ? RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER : RenderStateShard.RENDERTYPE_GLINT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, true, false))
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                .setTransparencyState(MODULATING_GLINT)
                .setTexturingState(texturing(name, color));

        if (translucent) {
            state.setOutputState(RenderStateShard.ITEM_ENTITY_TARGET);
        }

        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, state.createCompositeState(false));
    }

    private static RenderStateShard.TexturingStateShard texturing(String name, DyeColor color) {
        int rgb = color.getTextureDiffuseColor();
        float red = glintStrength(rgb >> 16 & 0xFF, rgb);
        float green = glintStrength(rgb >> 8 & 0xFF, rgb);
        float blue = glintStrength(rgb & 0xFF, rgb);
        float offsetRed = glintOffset(red);
        float offsetGreen = glintOffset(green);
        float offsetBlue = glintOffset(blue);

        return new RenderStateShard.TexturingStateShard(name, () -> {
            long scroll = (long) ((double) Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * GLINT_SCROLL_SPEED);
            float slow = (float) (scroll % SLOW_CYCLE_MILLIS) / SLOW_CYCLE_MILLIS;
            float fast = (float) (scroll % FAST_CYCLE_MILLIS) / FAST_CYCLE_MILLIS;
            Matrix4f matrix = new Matrix4f().translation(-slow, fast, 0.0F);

            RenderSystem.setTextureMatrix(matrix.rotateZ(GLINT_SKEW).scale(GLINT_SCALE));
            RenderSystem.setShaderColor(red, green, blue, 1.0F);
            GL14.glBlendColor(offsetRed, offsetGreen, offsetBlue, 1.0F);
        }, () -> {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL14.glBlendColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.resetTextureMatrix();
        });
    }
}
