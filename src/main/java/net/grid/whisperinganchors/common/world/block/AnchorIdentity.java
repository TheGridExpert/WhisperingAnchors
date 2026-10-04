package net.grid.whisperinganchors.common.world.block;

import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.world.blockentity.WhisperingAnchorBlockEntity;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public interface AnchorIdentity {

    @Nullable DyeColor getColor();

    void setColor(@Nullable DyeColor color);

    @Nullable Component getCustomName();

    void rename(Component name);

    static Component colorName(Component name, @Nullable DyeColor color) {
        return color == null ? name : name.copy().withStyle(style -> style.withColor(color.getTextColor()));
    }

    static @Nullable AnchorIdentity find(Level level, BlockPos top) {
        if (ModConfig.areAnchorsStatic()) {
            return level.getBlockEntity(top) instanceof WhisperingAnchorBlockEntity blockEntity ? blockEntity : null;
        }

        List<AnchorEntity> anchors = AnchorEntity.findBoundTo(level, top);

        return anchors.isEmpty() ? null : anchors.getFirst();
    }
}
