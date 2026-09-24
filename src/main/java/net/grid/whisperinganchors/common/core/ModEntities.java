package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AnchorEntity>> ANCHOR = registerEntity("anchor", EntityType.Builder.of(AnchorEntity::new, MobCategory.MISC)
            .sized(AnchorEntity.BODY_WIDTH, AnchorEntity.BODY_HEIGHT)
            .attach(EntityAttachment.NAME_TAG, 0.0F, AnchorEntity.BODY_TOP, 0.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .noSummon()
            .fireImmune()
    );

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerEntity(String name, EntityType.Builder<T> builder) {
        return ENTITIES.register(name, () -> builder.build(name));
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
