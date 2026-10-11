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
        for (int segment = 0; segment < ModSounds.HARKENING_BELL_RINGING.size(); segment++) {
            add(ModSounds.HARKENING_BELL_RINGING.get(segment), definition()
                    .subtitle("subtitles.whispering_anchors.harkening_bell_ringing")
                    .with(sound(WhisperingAnchors.modLoc("harkening_bell_ringing/segment_" + segment))));
        }
        add(ModSounds.HARKENING_BELL_ABRUPTED, definition()
                .subtitle("subtitles.whispering_anchors.harkening_bell_abrupted")
                .with(sound(WhisperingAnchors.modLoc("harkening_bell_abrupted"))));
        add(ModSounds.WHISPERS, definition()
                .subtitle("subtitles.whispering_anchors.whispers")
                .with(sound(WhisperingAnchors.modLoc("whispers"))));
    }
}
