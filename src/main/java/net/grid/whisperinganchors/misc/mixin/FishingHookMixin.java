package net.grid.whisperinganchors.misc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHook.class)
public class FishingHookMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getY(D)D"))
    private double whispering_anchors$hookAnchorBody(Entity hookedIn, double scale, Operation<Double> original) {
        return hookedIn instanceof AnchorEntity ? hookedIn.getEyeY() : original.call(hookedIn, scale);
    }
}
