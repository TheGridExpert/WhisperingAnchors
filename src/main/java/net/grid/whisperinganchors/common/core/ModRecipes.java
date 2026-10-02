package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.world.item.recipe.CompassClearingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, WhisperingAnchors.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> COMPASS_CLEARING = RECIPES.register("compass_clearing", () -> new SimpleCraftingRecipeSerializer<>(CompassClearingRecipe::new));

    public static void register(IEventBus eventBus) {
        RECIPES.register(eventBus);
    }
}
