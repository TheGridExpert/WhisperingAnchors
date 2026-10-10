package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.Optional;

public class ModStructureSets {

    public static final ResourceKey<StructureSet> TRAVERSAL_TOWERS = create("traversal_towers");

    public static void bootstrap(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);

        context.register(TRAVERSAL_TOWERS, new StructureSet(
                structures.getOrThrow(ModStructures.TRAVERSAL_TOWER),
                new RandomSpreadStructurePlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 1274903, Optional.of(new StructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.VILLAGES), 12)), 32, 26, RandomSpreadType.LINEAR)
        ));
    }

    private static ResourceKey<StructureSet> create(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, WhisperingAnchors.modLoc(name));
    }
}
