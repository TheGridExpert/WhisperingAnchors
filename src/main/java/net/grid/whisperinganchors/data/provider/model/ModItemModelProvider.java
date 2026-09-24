package net.grid.whisperinganchors.data.provider.model;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.world.item.HarkeningBellItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    private static final float RINGING_OFFSET_X = 0.0F;
    private static final float RINGING_OFFSET_Y = -6.75F;
    private static final float RINGING_OFFSET_Z = 1.0F;
    private static final float RINGING_SCALE = 0.55F;

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.WHISPERING_ANCHOR.get());
        harkeningBell();
        basicItem(ModItems.ANCHOR_BANNER_PATTERN.get());
    }

    private void harkeningBell() {
        ItemModelBuilder attuned = bell("harkening_bell_attuned", "harkening_bell_colorless");

        ItemModelBuilder ringing = bell("harkening_bell_ringing", "harkening_bell_colorless");
        ringing.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .translation(RINGING_OFFSET_X, RINGING_OFFSET_Y, RINGING_OFFSET_Z)
                .scale(RINGING_SCALE)
                .end()
                .end();

        bell(BuiltInRegistries.ITEM.getKey(ModItems.HARKENING_BELL.get()).getPath(), "harkening_bell")
                .override().predicate(HarkeningBellItem.ATTUNED_PROPERTY, 1.0F).model(attuned).end()
                .override().predicate(HarkeningBellItem.ATTUNED_PROPERTY, 1.0F).predicate(HarkeningBellItem.RINGING_PROPERTY, 1.0F).model(ringing).end();
    }

    private ItemModelBuilder bell(String name, String texture) {
        return withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("item/" + texture));
    }
}
