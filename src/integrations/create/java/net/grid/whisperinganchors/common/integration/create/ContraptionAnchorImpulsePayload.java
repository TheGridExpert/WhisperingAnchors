package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ContraptionAnchorImpulsePayload(int contraptionId, BlockPos localPos, double dirX, double dirZ, double contactY, double specificImpulse) implements CustomPacketPayload {

    public static final Type<ContraptionAnchorImpulsePayload> TYPE = new Type<>(WhisperingAnchors.modLoc("contraption_anchor_impulse"));

    public static final StreamCodec<ByteBuf, ContraptionAnchorImpulsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ContraptionAnchorImpulsePayload::contraptionId,
            BlockPos.STREAM_CODEC, ContraptionAnchorImpulsePayload::localPos,
            ByteBufCodecs.DOUBLE, ContraptionAnchorImpulsePayload::dirX,
            ByteBufCodecs.DOUBLE, ContraptionAnchorImpulsePayload::dirZ,
            ByteBufCodecs.DOUBLE, ContraptionAnchorImpulsePayload::contactY,
            ByteBufCodecs.DOUBLE, ContraptionAnchorImpulsePayload::specificImpulse,
            ContraptionAnchorImpulsePayload::new
    );

    @Override
    public Type<ContraptionAnchorImpulsePayload> type() {
        return TYPE;
    }

    public static void handle(ContraptionAnchorImpulsePayload payload, IPayloadContext context) {
        if (!(context.player().level().getEntity(payload.contraptionId()) instanceof AbstractContraptionEntity contraption)) return;

        ContraptionAnchor anchor = ContraptionAnchor.find(contraption, payload.localPos());
        if (anchor != null) {
            anchor.getSwing().applyImpulse(payload.dirX(), payload.dirZ(), payload.contactY(), payload.specificImpulse());
        }
    }
}
