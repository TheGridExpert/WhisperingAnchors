package net.grid.whisperinganchors.common.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record KnownAnchors(List<AnchorAttunement> anchors) implements IKnownAnchorsTooltip {

    public static final KnownAnchors EMPTY = new KnownAnchors(List.of());

    public static final Codec<KnownAnchors> CODEC = AnchorAttunement.CODEC.listOf().xmap(KnownAnchors::new, KnownAnchors::anchors);

    public static final StreamCodec<RegistryFriendlyByteBuf, KnownAnchors> STREAM_CODEC = AnchorAttunement.STREAM_CODEC.apply(ByteBufCodecs.list()).map(KnownAnchors::new, KnownAnchors::anchors);

    public KnownAnchors {
        anchors = List.copyOf(anchors);
    }

    public KnownAnchors with(AnchorAttunement anchor) {
        List<AnchorAttunement> updated = new ArrayList<>(this.anchors);
        updated.removeIf(known -> known.anchorPos().equals(anchor.anchorPos()));
        updated.add(anchor);

        return new KnownAnchors(updated);
    }

    @Override
    public List<DisplayAnchor> getDisplayAnchors() {
        return anchors().stream().map(anchor -> new DisplayAnchor(anchor.anchorPos(), anchor.getDisplayName())).toList();
    }
}
