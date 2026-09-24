package net.grid.whisperinganchors.data.provider.loot;

import net.grid.whisperinganchors.common.core.ModLootTables;
import net.grid.whisperinganchors.common.core.ModMapDecorations;
import net.grid.whisperinganchors.common.core.tag.ModStructureTags;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.function.BiConsumer;

public class ModChestLootProvider implements LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(ModLootTables.WA_PILLAGER_OUTPOST, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.6F))
                        .add(LootItem.lootTableItem(Items.MAP)
                                .apply(ExplorationMapFunction.makeExplorationMap()
                                        .setDestination(ModStructureTags.ON_TRAVERSAL_TOWER_MAPS)
                                        .setMapDecoration(ModMapDecorations.TRAVERSAL_TOWER)
                                        .setZoom((byte) 2)
                                        .setSearchRadius(50)
                                        .setSkipKnownStructures(false))
                                .apply(SetNameFunction.setName(Component.translatable("filled_map.whispering_anchors.traversal_tower"), SetNameFunction.Target.ITEM_NAME)))));
    }
}
