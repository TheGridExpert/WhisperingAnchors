package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.tag.ModBannerPatternTags;
import net.grid.whisperinganchors.common.world.item.AnchorBlockItem;
import net.grid.whisperinganchors.common.world.item.HarkeningBellItem;
import net.grid.whisperinganchors.common.world.item.HeedingCompassItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WhisperingAnchors.MODID);

    public static final DeferredItem<AnchorBlockItem> WHISPERING_ANCHOR = ITEMS.registerItem("whispering_anchor", props -> new AnchorBlockItem(ModBlocks.WHISPERING_ANCHOR.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<HarkeningBellItem> HARKENING_BELL = ITEMS.registerItem("harkening_bell", props -> new HarkeningBellItem(props.stacksTo(1)));
    public static final DeferredItem<HeedingCompassItem> HEEDING_COMPASS = ITEMS.registerItem("heeding_compass", props -> new HeedingCompassItem(props.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<BannerPatternItem> ANCHOR_BANNER_PATTERN = ITEMS.registerItem("anchor_banner_pattern", props -> new BannerPatternItem(ModBannerPatternTags.PATTERN_ITEM_ANCHOR, props.stacksTo(1).rarity(Rarity.RARE)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
