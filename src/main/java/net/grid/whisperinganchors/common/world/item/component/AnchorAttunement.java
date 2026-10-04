package net.grid.whisperinganchors.common.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModPoiTypes;
import net.grid.whisperinganchors.common.util.AnchorNames;
import net.grid.whisperinganchors.common.world.block.AnchorIdentity;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record AnchorAttunement(GlobalPos anchorPos, Optional<Component> anchorName, Optional<DyeColor> anchorColor) {

    public static final DyeColor DEFAULT_COLOR = DyeColor.LIGHT_BLUE;

    public static final Codec<AnchorAttunement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("anchor_pos").forGetter(AnchorAttunement::anchorPos),
            ComponentSerialization.CODEC.optionalFieldOf("anchor_name").forGetter(AnchorAttunement::anchorName),
            DyeColor.CODEC.optionalFieldOf("anchor_color").forGetter(AnchorAttunement::anchorColor)
    ).apply(instance, AnchorAttunement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorAttunement> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC, AnchorAttunement::anchorPos,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, AnchorAttunement::anchorName,
            ByteBufCodecs.optional(DyeColor.STREAM_CODEC), AnchorAttunement::anchorColor,
            AnchorAttunement::new
    );

    public static AnchorAttunement of(Level level, BlockPos pos, BlockState state) {
        BlockPos top = WhisperingAnchorBlock.getTopPos(pos, state);

        return of(GlobalPos.of(level.dimension(), top), AnchorIdentity.find(level, top));
    }

    public static AnchorAttunement of(GlobalPos anchorPos, @Nullable AnchorIdentity anchor) {
        if (anchor == null) {
            return new AnchorAttunement(anchorPos, Optional.empty(), Optional.empty());
        }

        return new AnchorAttunement(anchorPos, Optional.ofNullable(anchor.getCustomName()), Optional.ofNullable(anchor.getColor()));
    }

    public Component getDisplayName() {
        return AnchorIdentity.colorName(this.anchorName.orElseGet(() -> AnchorNames.generate(this.anchorPos)), this.anchorColor.orElse(null));
    }

    public @Nullable AnchorAttunement tick(ServerLevel holderLevel) {
        ServerLevel level = holderLevel.getServer().getLevel(this.anchorPos.dimension());
        if (level == null) return this;

        BlockPos pos = this.anchorPos.pos();
        if (!level.isInWorldBounds(pos) || !level.getPoiManager().existsAtPosition(ModPoiTypes.WHISPERING_ANCHOR.getKey(), pos)) {
            return null;
        }

        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof WhisperingAnchorBlockEntity blockEntity)) {
            return this;
        }

        AnchorIdentity anchor = blockEntity.getIdentity();
        if (anchor == null) return this;

        AnchorAttunement current = of(this.anchorPos, anchor);

        return current.equals(this) ? this : current;
    }
}
