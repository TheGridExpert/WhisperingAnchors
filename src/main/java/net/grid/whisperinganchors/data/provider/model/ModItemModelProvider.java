package net.grid.whisperinganchors.data.provider.model;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.world.item.HarkeningBellItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Locale;

public class ModItemModelProvider extends ItemModelProvider {

    private static final int COMPASS_FRAMES = 32;
    private static final ResourceLocation COMPASS_ANGLE = ResourceLocation.withDefaultNamespace("angle");

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.WHISPERING_ANCHOR.get());
        harkeningBell();
        compass(ModItems.HEEDING_COMPASS.get());
        basicItem(ModItems.ANCHOR_BANNER_PATTERN.get());
    }

    private void harkeningBell() {
        ItemModelBuilder attuned = bell("harkening_bell_attuned", "harkening_bell");

        ItemModelBuilder ringing = bell("harkening_bell_ringing", "harkening_bell");
        ringing.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .translation(0.0F, -6.75F, 1.0F)
                .scale(0.55F)
                .end()
                .end();

        bell(BuiltInRegistries.ITEM.getKey(ModItems.HARKENING_BELL.get()).getPath(), "harkening_bell")
                .override().predicate(HarkeningBellItem.ATTUNED_PROPERTY, 1.0F).model(attuned).end()
                .override().predicate(HarkeningBellItem.ATTUNED_PROPERTY, 1.0F).predicate(HarkeningBellItem.RINGING_PROPERTY, 1.0F).model(ringing).end();

        bell("harkening_bell_overlay", "harkening_bell_overlay");
    }

    private ItemModelBuilder bell(String name, String texture) {
        return withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc("item/" + texture));
    }

    private void compass(ItemLike item) {
        String name = BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
        ItemModelBuilder[] frames = new ItemModelBuilder[COMPASS_FRAMES];
        frames[COMPASS_FRAMES / 2] = bell(name, name + String.format(Locale.ROOT, "_%02d", COMPASS_FRAMES / 2));

        for (int i = 0; i < COMPASS_FRAMES; i++) {
            if (i != COMPASS_FRAMES / 2) {
                String indexedName = name + String.format(Locale.ROOT, "_%02d", i);
                frames[i] = bell(indexedName, indexedName);
            }
        }

        ItemModelBuilder base = frames[COMPASS_FRAMES / 2];
        base.override().predicate(COMPASS_ANGLE, 0.0F).model(base).end();
        for (int i = 1; i < COMPASS_FRAMES; i++) {
            base.override().predicate(COMPASS_ANGLE, (i - 0.5F) / COMPASS_FRAMES).model(frames[(i + COMPASS_FRAMES / 2) % COMPASS_FRAMES]).end();
        }
        base.override().predicate(COMPASS_ANGLE, 1.0F - 0.5F / COMPASS_FRAMES).model(base).end();
    }
}
