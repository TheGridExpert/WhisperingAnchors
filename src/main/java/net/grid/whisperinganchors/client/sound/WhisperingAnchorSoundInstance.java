package net.grid.whisperinganchors.client.sound;

import net.grid.whisperinganchors.common.core.ModSounds;
import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhisperingAnchorSoundInstance extends AbstractTickableSoundInstance {

    private static final float MAX_VOLUME = 0.75F;
    private static final double AUDIBLE_RANGE = 3.0;
    private static final double RELEASE_MARGIN = 1.0;

    private static final Map<BlockPos, WhisperingAnchorSoundInstance> PLAYING = new HashMap<>();

    private final Level level;
    private final BlockPos anchorPos;
    private @Nullable AnchorEntity anchor;

    private WhisperingAnchorSoundInstance(Level level, BlockPos anchorPos, LocalPlayer listener) {
        super(ModSounds.WHISPERS.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.level = level;
        this.anchorPos = anchorPos;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;

        updateSource(listener);
    }

    public static void updatePlaying(Level level, BlockPos anchorPos) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer listener = minecraft.player;
        if (listener == null || listener.level() != level) return;

        WhisperingAnchorSoundInstance playing = PLAYING.get(anchorPos);
        if (playing != null) {
            if (minecraft.getSoundManager().isActive(playing)) {
                return;
            }

            PLAYING.remove(anchorPos, playing);
        }

        if (volumeAt(listener.getEyePosition().distanceTo(restingPosition(anchorPos))) <= 0.0F) {
            return;
        }

        WhisperingAnchorSoundInstance started = new WhisperingAnchorSoundInstance(level, anchorPos.immutable(), listener);
        PLAYING.put(started.anchorPos, started);
        minecraft.getSoundManager().play(started);
    }

    @Override
    public void tick() {
        LocalPlayer listener = Minecraft.getInstance().player;
        if (listener == null || listener.level() != this.level || !anchorBlockExists()) {
            stopAndUnregister();
            return;
        }

        if (updateSource(listener) > AUDIBLE_RANGE + RELEASE_MARGIN) {
            stopAndUnregister();
        }
    }

    private double updateSource(LocalPlayer listener) {
        Vec3 position = sourcePosition();
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;

        double distance = listener.getEyePosition().distanceTo(position);
        this.volume = volumeAt(distance);

        return distance;
    }

    private Vec3 sourcePosition() {
        AnchorEntity resolved = resolveAnchor();
        return resolved == null ? restingPosition(this.anchorPos) : resolved.position();
    }

    private @Nullable AnchorEntity resolveAnchor() {
        if (this.anchor != null && this.anchor.isAlive() && this.anchor.isBoundTo(this.anchorPos)) return this.anchor;

        List<AnchorEntity> found = AnchorEntity.boundTo(this.level, this.anchorPos);
        this.anchor = found.isEmpty() ? null : found.getFirst();

        return this.anchor;
    }

    private boolean anchorBlockExists() {
        return this.level.getBlockState(this.anchorPos).getBlock() instanceof WhisperingAnchorBlock;
    }

    private void stopAndUnregister() {
        PLAYING.remove(this.anchorPos, this);
        stop();
    }

    private static Vec3 restingPosition(BlockPos anchorPos) {
        return Vec3.atCenterOf(anchorPos.below(AnchorPart.MIDDLE.belowTop()));
    }

    private static float volumeAt(double distance) {
        double falloff = Mth.clamp(1.0 - distance / AUDIBLE_RANGE, 0.0, 1.0);
        return (float) (MAX_VOLUME * falloff * falloff * falloff);
    }
}
