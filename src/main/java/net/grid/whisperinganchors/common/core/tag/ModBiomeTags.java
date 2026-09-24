package net.grid.whisperinganchors.common.core.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class ModBiomeTags {

    public static final TagKey<Biome> HAS_TRAVERSAL_TOWER = create("has_structure/traversal_tower");

    private static TagKey<Biome> create(String name) {
        return TagKey.create(Registries.BIOME, WhisperingAnchors.modLoc(name));
    }
}
