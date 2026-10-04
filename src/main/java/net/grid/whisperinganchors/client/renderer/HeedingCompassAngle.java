package net.grid.whisperinganchors.client.renderer;

import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class HeedingCompassAngle extends CompassItemPropertyFunction {

    private static final long TRANSITION_MILLIS = 750L;

    private final Map<Object, Map<Object, Transition>> transitions = new WeakHashMap<>();

    public HeedingCompassAngle() {
        super((level, stack, entity) -> getPointedTarget(level, stack));
    }

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        float angle = super.unclampedCall(stack, level, entity, seed);

        Object owner = entity != null ? entity : stack.getEntityRepresentation() != null ? stack.getEntityRepresentation() : stack;
        Object slot = entity == null ? seed : entity.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : entity.getOffhandItem() == stack ? InteractionHand.OFF_HAND : seed;
        Transition transition = this.transitions.computeIfAbsent(owner, key -> new HashMap<>()).computeIfAbsent(slot, key -> new Transition(getTarget(stack), angle));

        return transition.update(getTarget(stack), angle, Util.getMillis());
    }

    private static @Nullable GlobalPos getPointedTarget(ClientLevel level, ItemStack stack) {
        GlobalPos target = getTarget(stack);
        if (target == null || target.dimension() != level.dimension()) return target;
        if (SubLevelHandler.INSTANCE.isInUnloadedSubLevel(level, target.pos())) return null;

        Vec3 position = SubLevelHandler.INSTANCE.toWorldPosition(level, Vec3.atCenterOf(target.pos()));
        return GlobalPos.of(target.dimension(), BlockPos.containing(position));
    }

    private static @Nullable GlobalPos getTarget(ItemStack stack) {
        AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
        return attunement != null ? attunement.anchorPos() : null;
    }

    private static class Transition {

        private @Nullable GlobalPos target;
        private float shown;
        private float from;
        private long start;
        private boolean active;

        private Transition(@Nullable GlobalPos target, float angle) {
            this.target = target;
            this.shown = angle;
        }

        private float update(@Nullable GlobalPos target, float angle, long now) {
            if (!Objects.equals(target, this.target)) {
                this.target = target;
                this.from = this.shown;
                this.start = now;
                this.active = true;
            }

            float progress = this.active ? Mth.clamp((float) (now - this.start) / TRANSITION_MILLIS, 0.0F, 1.0F) : 1.0F;
            if (progress >= 1.0F) {
                this.active = false;
                this.shown = angle;
            } else {
                float delta = Mth.positiveModulo(angle - this.from + 0.5F, 1.0F) - 0.5F;
                this.shown = Mth.positiveModulo(this.from + delta * smoothOut(progress), 1.0F);
            }

            return this.shown;
        }

        private static float smoothOut(float x) {
            return x * x * x * (4.0F - 3.0F * x);
        }
    }
}
