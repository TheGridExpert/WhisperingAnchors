package net.grid.whisperinganchors.common.core.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypeTags {

    public static final TagKey<DamageType> DOES_NOT_INTERRUPT_HARKENING = create("does_not_interrupt_harkening");

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, WhisperingAnchors.modLoc(name));
    }
}
