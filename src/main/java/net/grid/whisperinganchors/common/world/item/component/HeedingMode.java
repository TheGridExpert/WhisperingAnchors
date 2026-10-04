package net.grid.whisperinganchors.common.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum HeedingMode implements StringRepresentable {
    UNKNOWN_TOWERS("unknown_towers"),
    KNOWN_ANCHORS("known_anchors");

    public static final Codec<HeedingMode> CODEC = StringRepresentable.fromEnum(HeedingMode::values);

    private static final IntFunction<HeedingMode> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, HeedingMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;

    HeedingMode(String name) {
        this.name = name;
    }

    public HeedingMode getNext() {
        return BY_ID.apply((this.ordinal() + 1) % values().length);
    }

    public Component getTooltip() {
        return Component.translatable("tooltip.whispering_anchors.heeding_compass.mode." + this.name);
    }

    public Component getSwitchMessage() {
        return Component.translatable("message.whispering_anchors.heeding_compass.mode." + this.name);
    }

    public Component getNotFoundMessage() {
        return Component.translatable("message.whispering_anchors.heeding_compass.cannot_find." + this.name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
