package net.grid.whisperinganchors.common.world.item.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModMapDecorations;
import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public record AnchorMarker(BlockPos pos, Optional<DyeColor> color, Optional<Component> name) {

    public static final Codec<AnchorMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(AnchorMarker::pos),
            DyeColor.CODEC.lenientOptionalFieldOf("color").forGetter(AnchorMarker::color),
            ComponentSerialization.FLAT_CODEC.lenientOptionalFieldOf("name").forGetter(AnchorMarker::name)
    ).apply(instance, AnchorMarker::new));
    public static final Codec<List<AnchorMarker>> LIST_CODEC = CODEC.listOf();

    public static AnchorMarker of(BlockPos top, @Nullable AnchorEntity anchor) {
        if (anchor == null) {
            return new AnchorMarker(top, Optional.empty(), Optional.empty());
        }

        return new AnchorMarker(top, Optional.ofNullable(anchor.getColor()), Optional.ofNullable(anchor.getCustomName()));
    }

    public static boolean isAnchor(BlockGetter level, BlockPos top) {
        BlockState state = level.getBlockState(top);
        return state.getBlock() instanceof WhisperingAnchorBlock && state.getValue(WhisperingAnchorBlock.PART) == AnchorPart.TOP;
    }

    public static @Nullable AnchorEntity loadedAnchor(Level level, BlockPos top) {
        List<AnchorEntity> anchors = AnchorEntity.boundTo(level, top);
        return anchors.isEmpty() ? null : anchors.getFirst();
    }

    public Holder<MapDecorationType> getDecoration() {
        return this.color.<Holder<MapDecorationType>>map(ModMapDecorations::anchor).orElse(ModMapDecorations.ANCHOR);
    }

    public String getId() {
        return "whispering_anchors:anchor-" + this.pos.getX() + "," + this.pos.getY() + "," + this.pos.getZ();
    }
}
