package net.grid.whisperinganchors.common.integration.jei;

import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.grid.whisperinganchors.common.world.item.component.KnownAnchors;
import net.grid.whisperinganchors.common.world.item.recipe.CompassClearingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class CompassClearingRecipeMaker {

    private static final RandomSource RANDOM = RandomSource.create();

    public static RecipeHolder<CraftingRecipe> createRecipe() {
        List<AnchorAttunement> anchors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            GlobalPos pos = GlobalPos.of(Level.OVERWORLD, BlockPos.containing(RANDOM.nextInt(-1000, 1000), RANDOM.nextInt(64, 256), RANDOM.nextInt(-1000, 1000)));
            anchors.add(AnchorAttunement.of(pos, null));
        }

        ItemStack filledCompass = ModItems.HEEDING_COMPASS.toStack();
        filledCompass.set(ModDataComponents.KNOWN_ANCHORS, new KnownAnchors(anchors));

        return new RecipeHolder<>(CompassClearingRecipe.ID, new ShapelessRecipe("whispering_anchors.compass_clearing", CraftingBookCategory.MISC, ModItems.HEEDING_COMPASS.toStack(), NonNullList.of(Ingredient.EMPTY, Ingredient.of(filledCompass))));
    }
}
