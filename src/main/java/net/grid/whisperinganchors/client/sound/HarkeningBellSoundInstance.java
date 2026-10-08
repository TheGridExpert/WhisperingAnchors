package net.grid.whisperinganchors.client.sound;

import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.core.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class HarkeningBellSoundInstance extends AbstractTickableSoundInstance {

    private static final int LATE_SYNC_TOLERANCE = 2;

    private final LivingEntity ringer;
    private final int duration;
    private boolean finished;

    public HarkeningBellSoundInstance(LivingEntity ringer, int segment, int duration, float pitch) {
        super(ModSounds.HARKENING_BELL_RINGING.get(segment).get(), ringer.getSoundSource(), SoundInstance.createUnseededRandom());
        this.ringer = ringer;
        this.duration = duration;
        this.looping = false;
        this.delay = 0;
        this.volume = 0.8F;
        this.pitch = pitch;

        updatePosition();
    }

    public static void playSegment(LivingEntity ringer, int usedTicks) {
        HarkeningBellPeal peal = HarkeningBellPeal.get();
        peal.findSegmentStartingAt(usedTicks).ifPresent(segment -> {
            float pitch = Mth.randomBetween(new XoroshiroRandomSource(ringer.getId() * 31L + ringer.tickCount - usedTicks), 0.9F, 1.1F);
            Minecraft.getInstance().getSoundManager().play(new HarkeningBellSoundInstance(ringer, segment, peal.getDuration(), pitch));
        });
    }

    public static boolean isUsingBell(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUseItem().is(ModItems.HARKENING_BELL.get());
    }

    @Override
    public void tick() {
        if (ringer.isRemoved()) {
            stop();
            return;
        }

        if (isUsingBell(ringer)) {
            this.finished = ringer.getTicksUsingItem() >= duration - LATE_SYNC_TOLERANCE;
            updatePosition();
            return;
        }

        if (!this.finished) {
            stop();
        }
    }

    private void updatePosition() {
        this.x = ringer.getX();
        this.y = ringer.getY();
        this.z = ringer.getZ();
    }
}
