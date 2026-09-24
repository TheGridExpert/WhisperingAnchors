package net.grid.whisperinganchors.common.core.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class ModBannerPatternTags {

    public static final TagKey<BannerPattern> PATTERN_ITEM_ANCHOR = create("pattern_item/anchor");

    private static TagKey<BannerPattern> create(String name) {
        return TagKey.create(Registries.BANNER_PATTERN, WhisperingAnchors.modLoc(name));
    }
}
