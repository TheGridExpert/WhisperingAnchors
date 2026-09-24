package net.grid.whisperinganchors.client.model;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.Optional;

public record AnchorRenderSettings(double shackleHeight, double chainPivot, double chainLinkSpacing) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation FILE = WhisperingAnchors.modLoc("models/block/anchor_render.mcmeta");

    private static final double MIN_LINK_SPACING = 1 / 16.0;

    public static final AnchorRenderSettings DEFAULT = new AnchorRenderSettings(24 / 16.0, 16 / 16.0, 4 / 16.0);

    private static AnchorRenderSettings current = DEFAULT;

    public static AnchorRenderSettings current() {
        return current;
    }

    public static void reload(ResourceManager resources) {
        current = DEFAULT;
        Optional<Resource> found = resources.getResource(FILE);

        if (found.isEmpty()) {
            LOGGER.warn("{} is missing; drawing the anchor with the built-in offsets", FILE);
            return;
        }

        try (Reader reader = found.get().openAsReader()) {
            JsonObject json = GsonHelper.parse(reader);
            current = new AnchorRenderSettings(pixels(json, "shackle_height", DEFAULT.shackleHeight), pixels(json, "chain_pivot", DEFAULT.chainPivot), Math.max(MIN_LINK_SPACING, pixels(json, "chain_link_spacing", DEFAULT.chainLinkSpacing)));
            LOGGER.debug("Anchor drawing offsets: shackle at {} px, chain pivot at {} px, links every {} px", current.shackleHeight * 16.0, current.chainPivot * 16.0, current.chainLinkSpacing * 16.0);
        } catch (Exception exception) {
            LOGGER.error("Could not read {}; drawing the anchor with the built-in offsets", FILE, exception);
        }
    }

    private static double pixels(JsonObject json, String key, double fallbackInBlocks) {
        return GsonHelper.getAsFloat(json, key, (float) (fallbackInBlocks * 16.0)) / 16.0;
    }
}
