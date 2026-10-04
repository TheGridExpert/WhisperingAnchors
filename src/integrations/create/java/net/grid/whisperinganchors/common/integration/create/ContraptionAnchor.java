package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.grid.whisperinganchors.api.AnchorFeatures;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.MutablePair;

import javax.annotation.Nullable;
import java.util.function.UnaryOperator;

public class ContraptionAnchor {

    private static final Vec3[] AXES = { new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0) };

    private final AnchorEntity swing;
    private final Vec3 localSuspension;
    private @Nullable Vec3 lastSuspension;
    private @Nullable Vec3[] lastAxes;
    private Vec3 lastVelocity = Vec3.ZERO;
    private Vec3 lastSpin = Vec3.ZERO;

    private ContraptionAnchor(MovementContext context) {
        this.swing = AnchorEntity.createDetached(context.world);
        this.localSuspension = Vec3.atLowerCornerOf(context.localPos).add(this.swing.getSuspensionPoint());
        if (context.data.contains(AnchorEntity.KEY_SWING)) {
            this.swing.readSwing(context.data.getCompound(AnchorEntity.KEY_SWING));
        }

        this.swing.setFrame(new ContraptionFrame(context, Vec3.atLowerCornerOf(context.localPos)));
        if (!context.world.isClientSide()) {
            this.swing.setImpulseListener((dirX, dirZ, contactY, specificImpulse) -> {
                AbstractContraptionEntity entity = context.contraption.entity;
                if (entity != null) {
                    PacketDistributor.sendToPlayersTrackingEntity(entity, new ContraptionAnchorImpulsePayload(entity.getId(), context.localPos, dirX, dirZ, contactY, specificImpulse));
                }
            });
        }
    }

    public static ContraptionAnchor of(MovementContext context) {
        if (context.temporaryData instanceof ContraptionAnchor anchor) return anchor;

        ContraptionAnchor anchor = new ContraptionAnchor(context);
        context.temporaryData = anchor;
        return anchor;
    }

    public static @Nullable ContraptionAnchor find(AbstractContraptionEntity entity, BlockPos localPos) {
        for (MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : entity.getContraption().getActors()) {
            if (actor.left.pos().equals(localPos) && actor.right != null && actor.right.temporaryData instanceof ContraptionAnchor anchor) {
                return anchor;
            }
        }

        return null;
    }

    public AnchorEntity getSwing() {
        return this.swing;
    }

    public void tick(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity == null) return;

        Vec3 suspension = entity.toGlobalVector(this.localSuspension, 1.0F);
        Vec3 velocity = this.lastSuspension == null ? Vec3.ZERO : suspension.subtract(this.lastSuspension);
        this.lastSuspension = suspension;
        this.lastVelocity = velocity;

        Vec3[] axes = new Vec3[AXES.length];
        for (int i = 0; i < AXES.length; i++) {
            axes[i] = entity.applyRotation(AXES[i], 1.0F);
        }
        Vec3 spin = this.lastAxes == null ? Vec3.ZERO : entity.reverseRotation(calculateTurn(this.lastAxes, axes), 1.0F);
        this.lastAxes = axes;
        this.lastSpin = spin;

        this.swing.tickSwing(new AnchorEntity.SwingFrame(velocity, spin, direction -> entity.reverseRotation(direction, 1.0F)));

        if (!context.world.isClientSide() && AnchorFeatures.doMovingAnchorsHitEntities()) {
            this.swing.collideWithEntities();
        }
    }

    public void transferSwing(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity == null || context.blockEntityData == null) return;

        UnaryOperator<Vec3> toWorld = direction -> entity.applyRotation(direction, 1.0F);
        Vec3 centreOfMass = this.swing.getCentreOfMass(this.swing.getChainX(1.0F), this.swing.getChainZ(1.0F), this.swing.getBodyX(1.0F), this.swing.getBodyZ(1.0F));
        Vec3 offset = toWorld.apply(centreOfMass.subtract(this.swing.getSuspensionPoint()));
        Vec3 frameVelocity = this.lastVelocity.add(toWorld.apply(this.lastSpin).cross(offset));

        this.swing.transformSwing(toWorld);
        this.swing.addSwingVelocity(frameVelocity);
        context.blockEntityData.put(AnchorEntity.KEY_SWING, this.swing.writeSwing());
    }

    private class ContraptionFrame implements AnchorEntity.Frame {

        private final MovementContext context;
        private final Vec3 offset;

        private ContraptionFrame(MovementContext context, Vec3 offset) {
            this.context = context;
            this.offset = offset;
        }

        @Override
        public Vec3 toLocalPosition(Vec3 worldPosition) {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            return entity == null ? worldPosition : entity.toLocalVector(worldPosition, 1.0F).subtract(this.offset);
        }

        @Override
        public Vec3 toLocalDirection(Vec3 worldDirection) {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            return entity == null ? worldDirection : entity.reverseRotation(worldDirection, 1.0F);
        }

        @Override
        public Vec3 toWorldPosition(Vec3 localPosition) {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            return entity == null ? localPosition : entity.toGlobalVector(localPosition.add(this.offset), 1.0F);
        }

        @Override
        public Vec3 toWorldDirection(Vec3 localDirection) {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            return entity == null ? localDirection : entity.applyRotation(localDirection, 1.0F);
        }

        @Override
        public Vec3 getVelocity(Vec3 localPosition) {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            if (entity == null) return Vec3.ZERO;

            Vec3 arm = localPosition.subtract(ContraptionAnchor.this.swing.getSuspensionPoint());
            return entity.reverseRotation(ContraptionAnchor.this.lastVelocity, 1.0F).add(ContraptionAnchor.this.lastSpin.cross(arm));
        }
    }

    private static Vec3 calculateTurn(Vec3[] from, Vec3[] to) {
        Vec3 sum = Vec3.ZERO;
        for (int i = 0; i < from.length; i++) {
            sum = sum.add(from[i].cross(to[i]));
        }

        double sine = sum.length() * 0.5;
        if (sine < 1.0E-9) return Vec3.ZERO;

        return sum.scale(Math.asin(Math.min(1.0, sine)) / (sine * 2.0));
    }
}
