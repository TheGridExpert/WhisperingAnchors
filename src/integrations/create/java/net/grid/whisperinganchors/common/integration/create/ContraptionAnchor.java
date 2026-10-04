package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class ContraptionAnchor {

    private static final Vec3[] AXES = { new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0) };

    private final AnchorEntity swing;
    private final Vec3 localSuspension;
    private @Nullable Vec3 lastSuspension;
    private @Nullable Vec3[] lastAxes;

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

        Vec3[] axes = new Vec3[AXES.length];
        for (int i = 0; i < AXES.length; i++) {
            axes[i] = entity.applyRotation(AXES[i], 1.0F);
        }
        Vec3 spin = this.lastAxes == null ? Vec3.ZERO : entity.reverseRotation(calculateTurn(this.lastAxes, axes), 1.0F);
        this.lastAxes = axes;

        this.swing.tickSwing(new AnchorEntity.SwingFrame(velocity, spin, direction -> entity.reverseRotation(direction, 1.0F)));
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
