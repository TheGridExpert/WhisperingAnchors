package net.grid.whisperinganchors.common.world.item.map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public interface AnchorMarkerData {

    boolean whisperingAnchors$toggleAnchor(Level level, BlockPos top);

    void whisperingAnchors$addAnchor(AnchorMarker marker, @Nullable LevelAccessor level);
}
