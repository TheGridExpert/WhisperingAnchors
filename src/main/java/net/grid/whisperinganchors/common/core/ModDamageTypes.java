package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {

    public static final ResourceKey<DamageType> ANCHOR_SWING = ResourceKey.create(Registries.DAMAGE_TYPE, WhisperingAnchors.modLoc("anchor_swing"));

    public static void bootstrap(BootstrapContext<DamageType> context) {
        ResourceLocation location = ANCHOR_SWING.location();
        context.register(ANCHOR_SWING, new DamageType(location.getNamespace() + "." + location.getPath(), DamageScaling.NEVER, 0.1F));
    }
}
