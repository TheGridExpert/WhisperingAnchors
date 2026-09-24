package net.grid.whisperinganchors.data.provider.tag;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModDamageTypes;
import net.grid.whisperinganchors.common.core.tag.ModDamageTypeTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.NO_KNOCKBACK).add(ModDamageTypes.ANCHOR_SWING); // The anchor has its own separate knockback
        tag(DamageTypeTags.DAMAGES_HELMET).add(ModDamageTypes.ANCHOR_SWING);
        tag(DamageTypeTags.PANIC_CAUSES).add(ModDamageTypes.ANCHOR_SWING);
        tag(Tags.DamageTypes.IS_PHYSICAL).add(ModDamageTypes.ANCHOR_SWING);

        tag(ModDamageTypeTags.DOES_NOT_INTERRUPT_HARKENING).add(
                DamageTypes.FELL_OUT_OF_WORLD,
                DamageTypes.OUTSIDE_BORDER,
                DamageTypes.IN_WALL,
                DamageTypes.CRAMMING,
                DamageTypes.DROWN,
                DamageTypes.DRY_OUT,
                DamageTypes.STARVE,
                DamageTypes.FREEZE,
                DamageTypes.ON_FIRE,
                DamageTypes.HOT_FLOOR,
                DamageTypes.CACTUS,
                DamageTypes.SWEET_BERRY_BUSH,
                DamageTypes.FALL,
                DamageTypes.WITHER
        );
    }
}
