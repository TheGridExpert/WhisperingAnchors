package net.grid.whisperinganchors.data.provider;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class ModSoundProvider extends SoundDefinitionsProvider {

    public ModSoundProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, WhisperingAnchors.MODID, existingFileHelper);
    }

    @Override
    public void registerSounds() {
        add(ModSounds.HARKENING_BELL_RINGING, definition()
                .subtitle("subtitles.whispering_anchors.harkening_bell_ringing")
                .with(sound(WhisperingAnchors.modLoc("harkening_bell_ringing"))));
        add(ModSounds.WHISPERS, definition()
                .subtitle("subtitles.whispering_anchors.whispers")
                .with(sound(WhisperingAnchors.modLoc("whispers"))));
    }
}
