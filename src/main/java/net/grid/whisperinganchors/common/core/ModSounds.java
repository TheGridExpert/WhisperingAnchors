package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.stream.IntStream;

public class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, WhisperingAnchors.MODID);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HARKENING_BELL_RINGING = IntStream.range(0, 7)
            .mapToObj(segment -> SOUND_EVENTS.register("harkening_bell_ringing_" + segment, SoundEvent::createVariableRangeEvent))
            .toList();
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPERS = SOUND_EVENTS.register("whispers", SoundEvent::createVariableRangeEvent);

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
