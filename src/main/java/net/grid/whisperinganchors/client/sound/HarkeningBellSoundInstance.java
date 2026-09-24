package net.grid.whisperinganchors.client.sound;

import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.core.ModSounds;
import net.grid.whisperinganchors.common.world.item.HarkeningBellItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class HarkeningBellSoundInstance extends AbstractTickableSoundInstance {

    private static final int LATE_SYNC_TOLERANCE = 2;

    private final LivingEntity ringer;
    private boolean finished;

    public HarkeningBellSoundInstance(LivingEntity ringer) {
        super(ModSounds.HARKENING_BELL_RINGING.get(), ringer.getSoundSource(), SoundInstance.createUnseededRandom());
        this.ringer = ringer;
        this.looping = false;
        this.delay = 0;
        this.volume = 0.8F;
        this.pitch = Mth.randomBetween(this.random, 0.9F, 1.1F);

        updatePosition();
    }

    public static void startFor(LivingEntity ringer) {
        Minecraft.getInstance().getSoundManager().play(new HarkeningBellSoundInstance(ringer));
    }

    public static boolean isRinging(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUseItem().is(ModItems.HARKENING_BELL.get());
    }

    @Override
    public void tick() {
        if (ringer.isRemoved()) {
            stop();
            return;
        }

        if (isRinging(ringer)) {
            this.finished = ringer.getTicksUsingItem() >= HarkeningBellItem.USE_DURATION - LATE_SYNC_TOLERANCE;
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
