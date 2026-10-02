package net.grid.whisperinganchors.common.world.item;

import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class AttunableItem extends Item {

    protected AttunableItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
        if (attunement == null) return;

        AnchorAttunement updated = attunement.tick(serverLevel);
        if (updated == null) {
            updated = onAttunementLost(serverLevel, entity, stack, attunement);
        }

        if (updated == null) {
            stack.remove(ModDataComponents.ANCHOR_ATTUNEMENT);
        } else if (updated != attunement) {
            stack.set(ModDataComponents.ANCHOR_ATTUNEMENT, updated);
        }
    }

    protected @Nullable AnchorAttunement onAttunementLost(ServerLevel level, Entity holder, ItemStack stack, AnchorAttunement lost) {
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        return state.is(ModBlocks.WHISPERING_ANCHOR) ? useOnAnchor(context, state) : super.useOn(context);
    }

    protected abstract InteractionResult useOnAnchor(UseOnContext context, BlockState state);
}
