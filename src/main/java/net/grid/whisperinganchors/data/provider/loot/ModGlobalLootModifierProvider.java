package net.grid.whisperinganchors.data.provider.loot;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModLootTables;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {

    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, WhisperingAnchors.MODID);
    }

    @Override
    protected void start() {
        addLoot(ModLootTables.WA_BURIED_TREASURE, BuiltInLootTables.BURIED_TREASURE);
        addLoot(ModLootTables.WA_PILLAGER_OUTPOST, BuiltInLootTables.PILLAGER_OUTPOST);
    }

    public void addLoot(ResourceKey<LootTable> toInject, ResourceKey<LootTable> target) {
        add(toInject.location().getPath(), new AddTableLootModifier(new LootItemCondition[]{LootTableIdCondition.builder(target.location()).build()}, toInject));
    }
}
