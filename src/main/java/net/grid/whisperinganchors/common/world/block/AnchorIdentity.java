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

    static @Nullable AnchorIdentity at(Level level, BlockPos top) {
        if (ModConfig.staticAnchors()) {
            return level.getBlockEntity(top) instanceof WhisperingAnchorBlockEntity blockEntity ? blockEntity : null;
        }

        List<AnchorEntity> anchors = AnchorEntity.boundTo(level, top);

        return anchors.isEmpty() ? null : anchors.getFirst();
    }
}
