package net.grid.whisperinganchors.misc.mixin.accessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrushableBlockEntity.class)
public interface BrushableBlockEntityAccessor {

    @Accessor("lootTable")
    @Nullable ResourceKey<LootTable> whispering_anchors$getLootTable();

    @Accessor("lootTableSeed")
    long whispering_anchors$getLootTableSeed();

    @Accessor("item")
    void whispering_anchors$setItem(ItemStack item);
}
