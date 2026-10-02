package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class ModLootTables {

    public static final ResourceKey<LootTable> TRAVERSAL_TOWER_ARCHAEOLOGY_GUARANTEED = create("archaeology/traversal_tower_guaranteed");
    public static final ResourceKey<LootTable> TRAVERSAL_TOWER_ARCHAEOLOGY_RARE = create("archaeology/traversal_tower_rare");
    public static final ResourceKey<LootTable> TRAVERSAL_TOWER_ARCHAEOLOGY_GENERAL = create("archaeology/traversal_tower_general");

    public static final ResourceKey<LootTable> WA_BURIED_TREASURE = create("chests/wa_buried_treasure");
    public static final ResourceKey<LootTable> WA_PILLAGER_OUTPOST = create("chests/wa_pillager_outpost");

    private static ResourceKey<LootTable> create(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, WhisperingAnchors.modLoc(name));
    }
}
