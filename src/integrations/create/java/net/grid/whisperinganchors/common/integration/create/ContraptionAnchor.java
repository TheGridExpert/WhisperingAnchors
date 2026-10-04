package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

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
    }

    public static ContraptionAnchor of(MovementContext context) {
        if (context.temporaryData instanceof ContraptionAnchor anchor) return anchor;

        ContraptionAnchor anchor = new ContraptionAnchor(context);
        context.temporaryData = anchor;
        return anchor;
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
    }

    public void transferSwing(MovementContext context) {
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity == null || context.blockEntityData == null) return;

        UnaryOperator<Vec3> toWorld = direction -> entity.applyRotation(direction, 1.0F);
        Vec3 centreOfMass = this.swing.getCentreOfMass(this.swing.getChainX(1.0F), this.swing.getChainZ(1.0F), this.swing.getTiltX(1.0F), this.swing.getTiltZ(1.0F));
        Vec3 offset = toWorld.apply(centreOfMass.subtract(this.swing.getSuspensionPoint()));
        Vec3 frameVelocity = this.lastVelocity.add(toWorld.apply(this.lastSpin).cross(offset));

        this.swing.transformSwing(toWorld);
        this.swing.addSwingVelocity(frameVelocity);
        context.blockEntityData.put(AnchorEntity.KEY_SWING, this.swing.writeSwing(new CompoundTag()));
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
