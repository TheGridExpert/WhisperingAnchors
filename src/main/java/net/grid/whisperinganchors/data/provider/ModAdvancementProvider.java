package net.grid.whisperinganchors.data.provider;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.*;
import net.grid.whisperinganchors.common.world.advancement.criterion.HarkeningTrigger;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends AdvancementProvider {

    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
        super(output, registries, existingFileHelper, List.of(ModAdvancementProvider::generate));
    }

    @SuppressWarnings("removal")
    public static void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer, ExistingFileHelper existingFileHelper) {
        AdvancementHolder findTraversalTower = Advancement.Builder.advancement()
                .display(ModBlocks.WHISPERING_ANCHOR.toStack(), Component.translatable("advancement.whispering_anchors.find_traversal_tower.title"), Component.translatable("advancement.whispering_anchors.find_traversal_tower.description"), null, AdvancementType.TASK, true, true, false)
                .parent(ResourceLocation.withDefaultNamespace("adventure/sleep_in_bed"))
                .addCriterion("traversal_tower", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(provider.lookupOrThrow(Registries.STRUCTURE).getOrThrow(ModStructures.TRAVERSAL_TOWER))))
                .save(consumer, WhisperingAnchors.modLoc("adventure/find_traversal_tower"), existingFileHelper);
        Advancement.Builder.advancement()
                .display(ModBlocks.WHISPERING_ANCHOR.toStack(), Component.translatable("advancement.whispering_anchors.get_hit_anchor.title"), Component.translatable("advancement.whispering_anchors.get_hit_anchor.description"), null, AdvancementType.TASK, true, true, true)
                .parent(findTraversalTower)
                .addCriterion("get_hit", EntityHurtPlayerTrigger.TriggerInstance.entityHurtPlayer(DamagePredicate.Builder.damageInstance().type(DamageSourcePredicate.Builder.damageType().source(EntityPredicate.Builder.entity().of(ModEntities.ANCHOR.get())))))
                .save(consumer, WhisperingAnchors.modLoc("adventure/get_hit_anchor"), existingFileHelper);
        AdvancementHolder harkenToAnchor = Advancement.Builder.advancement()
                .display(ModItems.HARKENING_BELL.toStack(), Component.translatable("advancement.whispering_anchors.harken_to_anchor.title"), Component.translatable("advancement.whispering_anchors.harken_to_anchor.description"), null, AdvancementType.TASK, true, true, false)
                .parent(findTraversalTower)
                .addCriterion("harken", HarkeningTrigger.TriggerInstance.harkened())
                .save(consumer, WhisperingAnchors.modLoc("adventure/harken_to_anchor"), existingFileHelper);
        Advancement.Builder.advancement()
                .display(ModItems.HARKENING_BELL.toStack(), Component.translatable("advancement.whispering_anchors.harken_from_falling.title"), Component.translatable("advancement.whispering_anchors.harken_from_falling.description"), null, AdvancementType.CHALLENGE, true, true, false)
                .parent(harkenToAnchor)
                .addCriterion("harken", HarkeningTrigger.TriggerInstance.harkenSavingFromFalling())
                .save(consumer, WhisperingAnchors.modLoc("adventure/harken_from_falling"), existingFileHelper);
        Advancement.Builder.advancement()
                .display(ModItems.HARKENING_BELL.toStack(), Component.translatable("advancement.whispering_anchors.harken_from_all_dimensions.title"), Component.translatable("advancement.whispering_anchors.harken_from_all_dimensions.description"), null, AdvancementType.CHALLENGE, true, true, false)
                .parent(ResourceLocation.withDefaultNamespace("nether/use_lodestone"))
                .addCriterion("harken_overworld", HarkeningTrigger.TriggerInstance.harkenedFrom(Level.OVERWORLD))
                .addCriterion("harken_nether", HarkeningTrigger.TriggerInstance.harkenedFrom(Level.NETHER))
                .addCriterion("harken_end", HarkeningTrigger.TriggerInstance.harkenedFrom(Level.END))
                .save(consumer, WhisperingAnchors.modLoc("nether/harken_from_all_dimensions"), existingFileHelper);
        Advancement.Builder.advancement()
                .display(ModItems.HARKENING_BELL.toStack(), Component.translatable("advancement.whispering_anchors.harken_from_alive_dragon.title"), Component.translatable("advancement.whispering_anchors.harken_from_alive_dragon.description"), null, AdvancementType.TASK, true, true, false)
                .parent(ResourceLocation.withDefaultNamespace("end/root"))
                .addCriterion("harken", HarkeningTrigger.TriggerInstance.harkenFromAliveDragon())
                .save(consumer, WhisperingAnchors.modLoc("adventure/harken_from_alive_dragon"), existingFileHelper);
    }
}
