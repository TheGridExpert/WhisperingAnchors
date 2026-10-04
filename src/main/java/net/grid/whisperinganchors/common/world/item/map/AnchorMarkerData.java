package net.grid.whisperinganchors.common.world.item.map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

public interface AnchorMarkerData {

    boolean whispering_anchors$toggleAnchor(Level level, BlockPos top);

    void whispering_anchors$addAnchor(AnchorMarker marker, @Nullable LevelAccessor level);
}
