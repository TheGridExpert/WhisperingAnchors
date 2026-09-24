package net.grid.whisperinganchors.common.world.blockentity;

import net.grid.whisperinganchors.client.sound.WhisperingAnchorSoundInstance;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.grid.whisperinganchors.common.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class WhisperingAnchorBlockEntity extends BlockEntity {

    private @Nullable AnchorEntity anchor;

    public WhisperingAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHISPERING_ANCHOR.get(), pos, state);
    }

    public @Nullable AnchorEntity getAnchor() {
        return this.anchor;
    }

    public void clientTick() {
        if (this.level == null) return;

        WhisperingAnchorSoundInstance.updatePlaying(this.level, this.worldPosition);
    }

    public void serverTick() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        if (this.anchor != null && this.anchor.isAlive() && this.anchor.isBoundTo(this.worldPosition)) return;

        AABB searchBox = AnchorEntity.searchArea(this.worldPosition);
        if (!checkIfAreaIsLoaded(searchBox, serverLevel)) return;

        this.anchor = claimExistingAnchor(serverLevel);
        if (this.anchor == null) {
            this.anchor = AnchorEntity.spawnFor(serverLevel, this.worldPosition);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean checkIfAreaIsLoaded(AABB area, ServerLevel level) {
        return level.hasChunksAt(BlockPos.containing(area.minX, area.minY, area.minZ), BlockPos.containing(area.maxX, area.maxY, area.maxZ));
    }

    private @Nullable AnchorEntity claimExistingAnchor(ServerLevel level) {
        List<AnchorEntity> found = AnchorEntity.boundTo(level, this.worldPosition);
        if (found.isEmpty()) return null;

        for (int i = 1; i < found.size(); i++) {
            found.get(i).discard();
        }

        return found.getFirst();
    }
}
