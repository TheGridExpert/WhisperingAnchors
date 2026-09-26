package net.grid.whisperinganchors.common.world.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModAdvancements;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class HarkeningTrigger extends SimpleCriterionTrigger<HarkeningTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceKey<Level> dimension, boolean lethalFall, boolean dragonIsAlive) {
        trigger(player, instance -> instance.matches(dimension, lethalFall, dragonIsAlive));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceKey<Level>> dimension, boolean lethalFall, boolean dragonIsAlive) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(TriggerInstance::dimension),
                Codec.BOOL.optionalFieldOf("lethal_fall", false).forGetter(TriggerInstance::lethalFall),
                Codec.BOOL.optionalFieldOf("dragon_is_alive", false).forGetter(TriggerInstance::dragonIsAlive)
        ).apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> harkened() {
            return ModAdvancements.HARKENED_TO_ANCHOR.get().createCriterion(new TriggerInstance(Optional.empty(), Optional.empty(), false, false));
        }

        public static Criterion<TriggerInstance> harkenedFrom(ResourceKey<Level> dimension) {
            return ModAdvancements.HARKENED_TO_ANCHOR.get().createCriterion(new TriggerInstance(Optional.empty(), Optional.of(dimension), false, false));
        }

        public static Criterion<TriggerInstance> harkenSavingFromFalling() {
            return ModAdvancements.HARKENED_TO_ANCHOR.get().createCriterion(new TriggerInstance(Optional.empty(), Optional.empty(), true, false));
        }

        public static Criterion<TriggerInstance> harkenFromAliveDragon() {
            return ModAdvancements.HARKENED_TO_ANCHOR.get().createCriterion(new TriggerInstance(Optional.empty(), Optional.empty(), false, true));
        }

        public boolean matches(ResourceKey<Level> dimension, boolean lethalFall, boolean dragonIsAlive) {
            return (this.dimension.isEmpty() || this.dimension.get().equals(dimension)) && (!this.lethalFall || lethalFall) && (!this.dragonIsAlive || dragonIsAlive);
        }
    }
}
