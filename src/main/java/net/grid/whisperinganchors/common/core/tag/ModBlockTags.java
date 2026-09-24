package net.grid.whisperinganchors.common.core.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

    public static final TagKey<Block> UNSAFE_TELEPORTS = create("unsafe_teleports");
    public static final TagKey<Block> TRAVERSAL_TOWER_FLOORS = create("traversal_tower_floors");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, WhisperingAnchors.modLoc(name));
    }
}
