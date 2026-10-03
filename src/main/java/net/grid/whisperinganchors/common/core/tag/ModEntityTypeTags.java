package net.grid.whisperinganchors.common.core.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModEntityTypeTags {

    private static TagKey<EntityType<?>> create(String namespace, String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, name));
    }

    public static class Integration {

        public static final TagKey<EntityType<?>> SABLE_RETAIN_IN_SUB_LEVEL = create("sable", "retain_in_sub_level");
        public static final TagKey<EntityType<?>> SABLE_DESTROY_WITH_SUB_LEVEL = create("sable", "destroy_with_sub_level");
    }
}
