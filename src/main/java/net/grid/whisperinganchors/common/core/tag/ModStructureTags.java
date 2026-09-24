package net.grid.whisperinganchors.common.core.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ModStructureTags {

    public static final TagKey<Structure> ON_TRAVERSAL_TOWER_MAPS = create("on_traversal_tower_maps");

    private static TagKey<Structure> create(String name) {
        return TagKey.create(Registries.STRUCTURE, WhisperingAnchors.modLoc(name));
    }
}
