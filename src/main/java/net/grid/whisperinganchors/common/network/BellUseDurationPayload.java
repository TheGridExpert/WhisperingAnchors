package net.grid.whisperinganchors.common.network;

import io.netty.buffer.ByteBuf;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModConfigs;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BellUseDurationPayload(int duration) implements CustomPacketPayload {

    public static final Type<BellUseDurationPayload> TYPE = new Type<>(WhisperingAnchors.modLoc("bell_use_duration"));

    public static final StreamCodec<ByteBuf, BellUseDurationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BellUseDurationPayload::duration,
            BellUseDurationPayload::new
    );

    public static BellUseDurationPayload fromConfig() {
        return new BellUseDurationPayload(ModConfigs.getBellUseDuration());
    }

    @Override
    public Type<BellUseDurationPayload> type() {
        return TYPE;
    }

    public static void handle(BellUseDurationPayload payload, IPayloadContext context) {
        ModConfigs.syncBellUseDuration(payload.duration());
    }
}
