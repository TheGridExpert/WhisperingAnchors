package net.grid.whisperinganchors.common.world.item.recipe;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.core.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CompassClearingRecipe extends CustomRecipe {

    public static final ResourceLocation ID = WhisperingAnchors.modLoc("compass_clearing");

    public CompassClearingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level) {
        boolean hasCompass = false;

        for (ItemStack stack : craftingInput.items()) {
            if (stack.is(ModItems.HEEDING_COMPASS)) {
                if (hasCompass) {
                    return false;
                }
                hasCompass = true;
            }
        }

        return hasCompass;
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider provider) {
        return craftingInput.items().stream().filter(stack -> stack.is(ModItems.HEEDING_COMPASS)).findFirst().map(stack -> {
            if (stack.is(ModItems.HEEDING_COMPASS)) {
                ItemStack result = stack.copy();
                result.remove(ModDataComponents.KNOWN_ANCHORS);

                return result;
            }

            return stack;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int x, int y) {
        return x * y >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMPASS_CLEARING.get();
    }
}
