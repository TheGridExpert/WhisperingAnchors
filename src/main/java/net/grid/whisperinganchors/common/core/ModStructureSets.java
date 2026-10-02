package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

public class ModStructureSets {

    public static final ResourceKey<StructureSet> TRAVERSAL_TOWERS = create("traversal_towers");

    public static void bootstrap(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        context.register(TRAVERSAL_TOWERS, new StructureSet(
                structures.getOrThrow(ModStructures.TRAVERSAL_TOWER),
                new RandomSpreadStructurePlacement(32, 26, RandomSpreadType.LINEAR, 1274903)
        ));
    }

    private static ResourceKey<StructureSet> create(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, WhisperingAnchors.modLoc(name));
    }
}
