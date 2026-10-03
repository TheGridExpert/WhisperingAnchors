package net.grid.whisperinganchors.common.integration.sable;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class SableSubLevelHandler implements SubLevelHandler {

    @Override
    public Vec3 toWorldPosition(Level level, Vec3 pos) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        return subLevel == null ? pos : subLevel.logicalPose().transformPosition(pos);
    }

    @Override
    public Vec3 toLocalPosition(Level level, Vec3 frame, Vec3 pos) {
        Vec3 world = toWorldPosition(level, pos);
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, frame);
        return subLevel == null ? world : subLevel.logicalPose().transformPositionInverse(world);
    }

    @Override
    public Vec3 toWorldDirection(Level level, Vec3 frame, Vec3 direction) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, frame);
        if (subLevel == null) return direction;

        Vector3d rotated = subLevel.logicalPose().orientation().transform(new Vector3d(direction.x, direction.y, direction.z));
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }

    @Override
    public Vec3 toLocalDirection(Level level, Vec3 frame, Vec3 direction) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, frame);
        if (subLevel == null) return direction;

        Vector3d rotated = subLevel.logicalPose().orientation().transformInverse(new Vector3d(direction.x, direction.y, direction.z));
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }

    @Override
    public Vec3 getAngularVelocity(Level level, Vec3 frame) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, frame);
        if (subLevel == null) return Vec3.ZERO;

        Quaterniond turn = new Quaterniond(subLevel.lastPose().orientation()).conjugate().mul(subLevel.logicalPose().orientation());
        double sign = turn.w < 0.0 ? -1.0 : 1.0;
        Vec3 axis = new Vec3(turn.x, turn.y, turn.z).scale(sign);
        double sine = axis.length();
        if (sine < 1.0E-9) return Vec3.ZERO;

        double angle = 2.0 * Math.atan2(sine, Math.abs(turn.w));
        return axis.scale(angle / sine);
    }

    @Override
    public Vec3 getVelocity(Level level, Vec3 pos) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        return subLevel == null ? Vec3.ZERO : subLevel.logicalPose().transformPosition(pos).subtract(subLevel.lastPose().transformPosition(pos));
    }

    @Override
    public boolean isInSameSubLevel(Level level, Vec3 a, Vec3 b) {
        return SableCompanion.INSTANCE.getContaining(level, a) == SableCompanion.INSTANCE.getContaining(level, b);
    }

    @Override
    public boolean isInUnloadedSubLevel(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.isInPlotGrid(level, pos) && SableCompanion.INSTANCE.getContaining(level, pos) == null;
    }

    @Override
    public Quaternionf getRenderOrientation(BlockPos pos, float partialTick) {
        ClientSubLevelAccess subLevel = SableCompanion.INSTANCE.getContainingClient(pos);
        return subLevel == null ? new Quaternionf() : new Quaternionf(subLevel.renderPose(partialTick).orientation());
    }
}
