package net.grid.whisperinganchors.common.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.grid.whisperinganchors.common.util.AnchorNames;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record DisplayAnchors(List<GlobalPos> anchors) implements IKnownAnchorsTooltip {

    public static final Codec<DisplayAnchors> CODEC = GlobalPos.CODEC.listOf().xmap(DisplayAnchors::new, DisplayAnchors::anchors);

    public static final StreamCodec<ByteBuf, DisplayAnchors> STREAM_CODEC = GlobalPos.STREAM_CODEC.apply(ByteBufCodecs.list()).map(DisplayAnchors::new, DisplayAnchors::anchors);

    public DisplayAnchors {
        anchors = List.copyOf(anchors);
    }

    @Override
    public List<DisplayAnchor> getDisplayAnchors() {
        return anchors().stream().map(pos -> new DisplayAnchor(pos, AnchorNames.generate(pos))).toList();
    }

    @Override
    public boolean ignorePlayerPosition() {
        return true;
    }
}
