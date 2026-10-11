package net.grid.whisperinganchors.common.event;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModConfigs;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.core.ModSounds;
import net.grid.whisperinganchors.common.core.tag.ModDamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = WhisperingAnchors.MODID)
public class HarkeningBellEvents {

    @SubscribeEvent
    public static void cancelBellUseOnDamage(LivingIncomingDamageEvent event) {
        LivingEntity ringer = event.getEntity();
        if (ringer.level().isClientSide() || !ringer.isUsingItem() || !ringer.getUseItem().is(ModItems.HARKENING_BELL.get())) {
            return;
        }
        if (event.getSource().is(ModDamageTypeTags.DOES_NOT_INTERRUPT_HARKENING)) return;

        ringer.stopUsingItem();
        ringer.level().playSound(null, ringer.getX(), ringer.getY(), ringer.getZ(), ModSounds.HARKENING_BELL_ABRUPTED.get(), ringer.getSoundSource(), 0.8F, Mth.randomBetween(ringer.getRandom(), 0.95F, 1.05F));
        ModItems.HARKENING_BELL.get().startCooldown(ringer, ModConfigs.getBellFailedCooldown());
    }
}
