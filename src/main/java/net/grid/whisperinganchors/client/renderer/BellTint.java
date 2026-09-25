package net.grid.whisperinganchors.client.renderer;

import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public final class BellTint {

    private static final double SATURATION = 0.85;
    private static final double CONTRAST = 1.35;
    private static final double HIGHLIGHT_CONTRAST = 1.7;
    private static final double MIDPOINT = 130.0;
    private static final double SHADOW_RANGE = 95.0;
    private static final double HIGHLIGHT_RANGE = 120.0;

    private static final double RED_LUMA = 0.2126;
    private static final double GREEN_LUMA = 0.7152;
    private static final double BLUE_LUMA = 0.0722;

    private static final int OPAQUE = 0xFF << 24;

    public static int of(DyeColor color) {
        int rgb = color.getTextureDiffuseColor();
        double red = rgb >> 16 & 0xFF;
        double green = rgb >> 8 & 0xFF;
        double blue = rgb & 0xFF;
        double luma = RED_LUMA * red + GREEN_LUMA * green + BLUE_LUMA * blue;

        double lightness = MIDPOINT + compressLightness(luma - MIDPOINT);

        return OPAQUE | channel(red, luma, lightness) << 16 | channel(green, luma, lightness) << 8 | channel(blue, luma, lightness);
    }

    private static double compressLightness(double offset) {
        double contrast = offset > 0.0 ? HIGHLIGHT_CONTRAST : CONTRAST;
        double range = offset > 0.0 ? HIGHLIGHT_RANGE : SHADOW_RANGE;

        return range * Math.tanh(offset * contrast / range);
    }

    private static int channel(double value, double luma, double lightness) {
        return Mth.clamp((int) Math.round(lightness + SATURATION * (value - luma)), 0, 0xFF);
    }
}
