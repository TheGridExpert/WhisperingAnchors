package net.grid.whisperinganchors.common.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class WAJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return WhisperingAnchors.modLoc(WhisperingAnchors.MODID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RecipeTypes.CRAFTING, List.of(CompassClearingRecipeMaker.createRecipe()));
    }
}
