package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PunchContraptionAnchorPayload(int contraptionId, BlockPos localPos) implements CustomPacketPayload {

    public static final Type<PunchContraptionAnchorPayload> TYPE = new Type<>(WhisperingAnchors.modLoc("punch_contraption_anchor"));

    public static final StreamCodec<ByteBuf, PunchContraptionAnchorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PunchContraptionAnchorPayload::contraptionId,
            BlockPos.STREAM_CODEC, PunchContraptionAnchorPayload::localPos,
            PunchContraptionAnchorPayload::new
    );

    private static final double REACH_MARGIN = 1.0;

    @Override
    public Type<PunchContraptionAnchorPayload> type() {
        return TYPE;
    }

    public static void handle(PunchContraptionAnchorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level().getEntity(payload.contraptionId()) instanceof AbstractContraptionEntity contraption)) return;

        ContraptionAnchor anchor = ContraptionAnchor.find(contraption, payload.localPos());
        if (anchor == null) return;

        Vec3 eye = contraption.toLocalVector(player.getEyePosition(), 1.0F).subtract(Vec3.atLowerCornerOf(payload.localPos()));
        double reach = player.entityInteractionRange() + REACH_MARGIN;
        if (anchor.getSwing().getSwingBody().distanceToSqr(eye) > reach * reach) return;

        anchor.getSwing().takeHitFrom(player);
    }
}
