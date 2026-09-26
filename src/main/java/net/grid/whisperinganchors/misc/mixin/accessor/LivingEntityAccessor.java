package net.grid.whisperinganchors.misc.mixin.accessor;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("calculateFallDamage")
    int whispering_anchors$calculateFallDamage(float fallDistance, float damageMultiplier);

    @Invoker("getDamageAfterMagicAbsorb")
    float whispering_anchors$getDamageAfterMagicAbsorb(DamageSource damageSource, float damageAmount);
}
