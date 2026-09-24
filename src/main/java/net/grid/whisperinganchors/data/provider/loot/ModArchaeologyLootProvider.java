package net.grid.whisperinganchors.data.provider.loot;

import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.core.ModLootTables;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ModArchaeologyLootProvider implements LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(ModLootTables.TRAVERSAL_TOWER_ARCHAEOLOGY_GUARANTEED, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(single(ModItems.HARKENING_BELL.get()))));

        output.accept(ModLootTables.TRAVERSAL_TOWER_ARCHAEOLOGY_GENERAL, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(single(Items.ROTTEN_FLESH).setWeight(10))
                        .add(stack(Items.STRING, 1, 3).setWeight(10))
                        .add(stack(Items.STICK, 1, 3).setWeight(10))
                        .add(single(Items.BONE).setWeight(8))
                        .add(single(Items.FLINT).setWeight(8))
                        .add(stack(Items.ARROW, 1, 4).setWeight(8))
                        .add(stack(Items.COBBLED_DEEPSLATE, 1, 4).setWeight(8))
                        .add(stack(Items.WHEAT, 1, 3).setWeight(6))
                        .add(single(Items.LADDER).setWeight(6))
                        .add(stack(Items.COBBLED_DEEPSLATE, 1, 3).setWeight(6))
                        .add(stack(Items.DEEPSLATE_TILES, 1, 3).setWeight(6))
                        .add(single(Items.IRON_NUGGET).setWeight(6))
                        .add(single(Items.COAL).setWeight(6))
                        .add(single(Items.POTATO).setWeight(5))
                        .add(single(Items.CARROT).setWeight(5))
                        .add(single(Items.CHAIN).setWeight(5))
                        .add(single(Items.SOUL_LANTERN).setWeight(5))
                        .add(single(Items.DARK_OAK_LOG).setWeight(5))
                        .add(single(Items.GUNPOWDER).setWeight(4))
                        .add(single(Items.LEATHER).setWeight(4))
                        .add(stack(Items.PAPER, 1, 3).setWeight(4))
                        .add(single(Items.IRON_INGOT).setWeight(3))
                        .add(single(Items.EMERALD).setWeight(3))
                        .add(single(Items.EXPERIENCE_BOTTLE).setWeight(2))
                        .add(single(Items.CROSSBOW)
                                .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.5F)))
                                .setWeight(2))
                        .add(single(ModItems.ANCHOR_BANNER_PATTERN.get()).setWeight(10))
                        .add(single(ModItems.HARKENING_BELL.get()).setWeight(3))));
    }

    private static LootPoolSingletonContainer.Builder<?> single(Item item) {
        return LootItem.lootTableItem(item);
    }

    private static LootPoolSingletonContainer.Builder<?> stack(Item item, int min, int max) {
        return LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }
}
