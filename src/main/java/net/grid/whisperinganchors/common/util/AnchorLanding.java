package net.grid.whisperinganchors.common.util;

import net.grid.whisperinganchors.common.core.tag.ModBlockTags;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record AnchorLanding(Vec3 position, float yRot, float xRot) {

    private static final int GAP = 1;
    private static final int STAND_OFF = GAP + 1;
    private static final int MAX_DROP = 4;
    private static final int NEARBY_RADIUS = 4;

    private static final List<Vec3i> NEARBY_OFFSETS = createNearbyOffsets();

    public static Optional<AnchorLanding> find(ServerLevel level, BlockPos anchorPos, LivingEntity traveller) {
        BlockState state = level.getBlockState(anchorPos);
        if (!(state.getBlock() instanceof WhisperingAnchorBlock)) return Optional.empty();

        BlockPos top = WhisperingAnchorBlock.getTopPos(anchorPos, state);
        Direction facing = state.getValue(WhisperingAnchorBlock.FACING);
        Direction[] sides = { facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite() };

        for (Direction side : sides) {
            Optional<AnchorLanding> landing = findLandingInColumn(level, top, top.relative(side, STAND_OFF), traveller);
            if (landing.isPresent()) return landing;
        }

        for (Vec3i offset : NEARBY_OFFSETS) {
            Optional<AnchorLanding> landing = findLandingInColumn(level, top, top.offset(offset), traveller);
            if (landing.isPresent()) return landing;
        }

        if (traveller instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.whispering_anchors.anchor.obstructed"), true);
        }

        return Optional.empty();
    }

    private static Optional<AnchorLanding> findLandingInColumn(ServerLevel level, BlockPos top, BlockPos column, LivingEntity traveller) {
        int lowest = top.getY() - (WhisperingAnchorBlock.HEIGHT - 1) - MAX_DROP;

        for (int y = top.getY(); y >= lowest; y--) {
            Vec3 spot = findStandingSpot(level, column.atY(y), traveller);
            if (spot != null) {
                return Optional.of(withRotationTowardsAnchor(level, top, spot, traveller));
            }
        }

        return Optional.empty();
    }

    private static @Nullable Vec3 findStandingSpot(ServerLevel level, BlockPos feet, LivingEntity traveller) {
        BlockPos support = feet.below();
        BlockState floorState = level.getBlockState(support);
        VoxelShape floor = floorState.getCollisionShape(level, support);
        if (floor.isEmpty() || floorState.is(ModBlockTags.UNSAFE_TELEPORTS)) return null;

        Vec3 position = new Vec3(feet.getX() + 0.5, support.getY() + floor.max(Direction.Axis.Y), feet.getZ() + 0.5);
        AABB room = traveller.getDimensions(traveller.getPose()).makeBoundingBox(position);

        return level.noCollision(traveller, room) && !level.containsAnyLiquid(room) && !containsUnsafeBlocks(level, room) ? position : null;
    }

    private static boolean containsUnsafeBlocks(ServerLevel level, AABB room) {
        return level.getBlockStates(room).anyMatch(state -> state.is(ModBlockTags.UNSAFE_TELEPORTS));
    }

    private static AnchorLanding withRotationTowardsAnchor(ServerLevel level, BlockPos top, Vec3 spot, LivingEntity traveller) {
        Vec3 target = SubLevelHandler.INSTANCE.toWorldPosition(level, Vec3.atCenterOf(top.below()));
        Vec3 position = SubLevelHandler.INSTANCE.toWorldPosition(level, spot);

        double dx = target.x - position.x;
        double dy = target.y - (position.y + traveller.getEyeHeight());
        double dz = target.z - position.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yRot = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
        float xRot = Mth.wrapDegrees((float) (-(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG)));

        return new AnchorLanding(position, yRot, xRot);
    }

    private static List<Vec3i> createNearbyOffsets() {
        List<Vec3i> offsets = new ArrayList<>();

        for (int x = -NEARBY_RADIUS; x <= NEARBY_RADIUS; x++) {
            for (int z = -NEARBY_RADIUS; z <= NEARBY_RADIUS; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) >= STAND_OFF) {
                    offsets.add(new Vec3i(x, 0, z));
                }
            }
        }

        offsets.sort(Comparator.comparingInt(offset -> offset.getX() * offset.getX() + offset.getZ() * offset.getZ()));

        return List.copyOf(offsets);
    }
}
