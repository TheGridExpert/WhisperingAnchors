package net.grid.whisperinganchors.common.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModDataComponents;
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
import net.minecraft.world.item.ItemStack;
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

    public static @Nullable DyeColor colorOf(ItemStack stack) {
        AnchorAttunement attunement = stack.get(ModDataComponents.ANCHOR_ATTUNEMENT);
        return attunement == null ? null : attunement.anchorColor().orElse(DEFAULT_COLOR);
    }

    public static AnchorAttunement of(Level level, BlockPos pos, BlockState state) {
        BlockPos top = WhisperingAnchorBlock.topOf(pos, state);
        AnchorIdentity anchor = AnchorIdentity.at(level, top);

        return new AnchorAttunement(GlobalPos.of(level.dimension(), top), anchor == null ? Optional.empty() : Optional.ofNullable(anchor.getCustomName()), anchor == null ? Optional.empty() : Optional.ofNullable(anchor.getColor()));
    }

    public Component name() {
        Component name = this.anchorName.orElseGet(() -> AnchorNames.generate(this.anchorPos));

        return this.anchorColor.<Component>map(color -> name.copy().withStyle(style -> style.withColor(color.getTextColor()))).orElse(name);
    }

    public @Nullable AnchorAttunement tick(ServerLevel level) {
        if (this.anchorPos.dimension() != level.dimension()) {
            return this;
        }

        BlockPos pos = this.anchorPos.pos();
        if (!level.isInWorldBounds(pos) || !level.getPoiManager().existsAtPosition(ModPoiTypes.WHISPERING_ANCHOR.getKey(), pos)) {
            return null;
        }

        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof WhisperingAnchorBlockEntity blockEntity)) {
            return this;
        }

        AnchorIdentity anchor = blockEntity.identity();
        if (anchor == null) return this;

        Optional<Component> name = Optional.ofNullable(anchor.getCustomName());
        Optional<DyeColor> color = Optional.ofNullable(anchor.getColor());

        return name.equals(this.anchorName) && color.equals(this.anchorColor) ? this : new AnchorAttunement(this.anchorPos, name, color);
    }
}
