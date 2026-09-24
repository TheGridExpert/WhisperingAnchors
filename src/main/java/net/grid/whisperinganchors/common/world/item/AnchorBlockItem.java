package net.grid.whisperinganchors.common.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;

public class AnchorBlockItem extends BlockItem {

    private static final int MAX_GAP_BELOW = 1;

    public AnchorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return context;
        }

        Level level = context.getLevel();
        BlockPos.MutableBlockPos cursor = context.getClickedPos().mutable();
        for (int offset = 0; offset <= WhisperingAnchorBlock.HEIGHT + MAX_GAP_BELOW; offset++) {
            if (cursor.getY() >= level.getMaxBuildHeight()) break;

            if (!level.getBlockState(cursor).canBeReplaced(context)) {
                return Block.canSupportCenter(level, cursor, Direction.DOWN) ? BlockPlaceContext.at(context, cursor.below(), Direction.DOWN) : context;
            }

            cursor.move(Direction.UP);
        }

        return context;
    }
}
