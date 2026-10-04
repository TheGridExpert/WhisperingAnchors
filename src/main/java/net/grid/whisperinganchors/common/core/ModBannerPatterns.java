package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class ModBannerPatterns {

    public static final ResourceKey<BannerPattern> ANCHOR = ResourceKey.create(Registries.BANNER_PATTERN, WhisperingAnchors.modLoc("anchor"));

    public static void bootstrap(BootstrapContext<BannerPattern> context) {
        context.register(ANCHOR, new BannerPattern(ANCHOR.location(), "block.minecraft.banner." + ANCHOR.location().toShortLanguageKey()));
    }
}
