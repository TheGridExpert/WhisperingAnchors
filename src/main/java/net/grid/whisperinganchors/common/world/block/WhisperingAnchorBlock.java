package net.grid.whisperinganchors.common.world.block;

import com.mojang.serialization.MapCodec;
import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.grid.whisperinganchors.common.core.ModBlockEntities;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.grid.whisperinganchors.common.world.item.map.AnchorMarkerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class WhisperingAnchorBlock extends Block implements EntityBlock {

    public static final MapCodec<WhisperingAnchorBlock> CODEC = simpleCodec(WhisperingAnchorBlock::new);

    public static final EnumProperty<AnchorPart> PART = EnumProperty.create("part", AnchorPart.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final int HEIGHT = 3;

    private static final AnchorPart[] PLACEMENT_ORDER = { AnchorPart.BOTTOM, AnchorPart.MIDDLE, AnchorPart.TOP };

    private static final VoxelShape CHAIN_SHAPE = Block.box(6.5, 10.0, 6.5, 9.5, 16.0, 9.5);
    private static final VoxelShape TOP_SHAPE = Shapes.join(CHAIN_SHAPE, restingBodyWithin(AnchorPart.TOP), BooleanOp.OR);
    private static final VoxelShape MIDDLE_SHAPE = restingBodyWithin(AnchorPart.MIDDLE);
    private static final VoxelShape BOTTOM_SHAPE = restingBodyWithin(AnchorPart.BOTTOM);

    public WhisperingAnchorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, AnchorPart.TOP).setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends WhisperingAnchorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }

    private static boolean isPart(BlockState state, AnchorPart part) {
        return state.getBlock() instanceof WhisperingAnchorBlock && state.getValue(PART) == part;
    }

    public static BlockPos topOf(BlockPos pos, BlockState state) {
        return pos.above(state.getValue(PART).belowTop());
    }

    private static VoxelShape restingBodyWithin(AnchorPart part) {
        AABB body = AnchorEntity.restingBody(BlockPos.ZERO).move(0.0, part.belowTop(), 0.0);
        return Shapes.create(body.intersect(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case TOP -> ModConfig.solidStaticAnchors() ? TOP_SHAPE : CHAIN_SHAPE;
            case MIDDLE -> ModConfig.solidStaticAnchors() ? MIDDLE_SHAPE : Shapes.empty();
            case BOTTOM -> ModConfig.solidStaticAnchors() ? BOTTOM_SHAPE : Shapes.empty();
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return stateFor(context, AnchorPart.TOP);
        }

        for (AnchorPart part : PLACEMENT_ORDER) {
            BlockState state = stateFor(context, part);
            if (state != null) {
                return state;
            }
        }

        return null;
    }

    private @Nullable BlockState stateFor(BlockPlaceContext context, AnchorPart clickedPart) {
        BlockPos top = context.getClickedPos().above(clickedPart.belowTop());
        if (!hasRoom(context, top) || !hasCeiling(context.getLevel(), top)) return null;

        return defaultBlockState().setValue(PART, clickedPart).setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static boolean hasRoom(BlockPlaceContext context, BlockPos top) {
        Level level = context.getLevel();
        for (AnchorPart part : AnchorPart.values()) {
            BlockPos target = top.below(part.belowTop());

            if (target.getY() < level.getMinBuildHeight() || target.getY() >= level.getMaxBuildHeight()) {
                return false;
            }
            if (!target.equals(context.getClickedPos()) && !level.getBlockState(target).canBeReplaced(context)) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasCeiling(LevelReader level, BlockPos top) {
        return Block.canSupportCenter(level, top.above(), Direction.DOWN);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockPos top = topOf(pos, state);
        for (AnchorPart part : AnchorPart.values()) {
            BlockPos target = top.below(part.belowTop());
            if (!target.equals(pos)) {
                level.setBlock(target, state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos top = topOf(pos, state);

        if (stack.is(Items.FILLED_MAP)) {
            return toggleMapMarker(stack, level, top);
        }
        if (stack.is(Items.NAME_TAG)) {
            return renameAnchor(stack, level, top, player);
        }
        if (stack.getItem() instanceof DyeItem dye) {
            return applyToAnchor(stack, level, top, player, anchor -> anchor.setColor(dye.getDyeColor()));
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemInteractionResult toggleMapMarker(ItemStack stack, Level level, BlockPos top) {
        if (!level.isClientSide) {
            MapItemSavedData data = MapItem.getSavedData(stack, level);
            if (data != null && !((AnchorMarkerData) data).whisperingAnchors$toggleAnchor(level, top)) {
                return ItemInteractionResult.FAIL;
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult renameAnchor(ItemStack stack, Level level, BlockPos top, Player player) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return applyToAnchor(stack, level, top, player, anchor -> anchor.rename(name));
    }

    private static ItemInteractionResult applyToAnchor(ItemStack stack, Level level, BlockPos top, Player player, Consumer<AnchorIdentity> action) {
        AnchorIdentity anchor = AnchorIdentity.at(level, top);
        if (anchor == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            action.accept(anchor);
            stack.consume(1, player);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(PART) != AnchorPart.TOP || hasCeiling(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        AnchorPart part = state.getValue(PART);
        if (direction == Direction.UP) {
            AnchorPart expected = part.above();
            boolean held = expected == null ? Block.canSupportCenter(level, neighborPos, Direction.DOWN) : isPart(neighborState, expected);
            return held ? state : Blocks.AIR.defaultBlockState();
        }

        if (direction == Direction.DOWN) {
            AnchorPart expected = part.below();
            if (expected != null && !isPart(neighborState, expected)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(PART) == AnchorPart.TOP && level instanceof ServerLevel serverLevel && ModConfig.staticAnchors()) {
            AnchorEntity.crumble(serverLevel, AnchorEntity.restingBody(pos));
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && state.getValue(PART) != AnchorPart.BOTTOM && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            BlockPos bottom = topOf(pos, state).below(AnchorPart.BOTTOM.belowTop());
            if (isPart(level.getBlockState(bottom), AnchorPart.BOTTOM)) {
                level.setBlock(bottom, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, bottom, Block.getId(state));
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == AnchorPart.TOP ? new WhisperingAnchorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.WHISPERING_ANCHOR.get()) return null;

        if (level.isClientSide()) {
            return (tickLevel, tickPos, tickState, blockEntity) -> {
                if (blockEntity instanceof WhisperingAnchorBlockEntity anchor) {
                    anchor.clientTick();
                }
            };
        }

        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof WhisperingAnchorBlockEntity anchor) {
                anchor.serverTick();
            }
        };
    }
}
