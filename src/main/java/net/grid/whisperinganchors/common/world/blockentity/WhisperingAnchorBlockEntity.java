package net.grid.whisperinganchors.common.world.blockentity;

import net.grid.whisperinganchors.client.sound.WhisperingAnchorSoundInstance;
import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.world.block.AnchorIdentity;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.grid.whisperinganchors.common.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class WhisperingAnchorBlockEntity extends BlockEntity implements AnchorIdentity {

    private @Nullable AnchorEntity anchor;
    private @Nullable DyeColor color;
    private @Nullable Component customName;

    public WhisperingAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHISPERING_ANCHOR.get(), pos, state);
    }

    public @Nullable AnchorEntity getAnchor() {
        return this.anchor != null && this.anchor.isAlive() ? this.anchor : null;
    }

    public @Nullable AnchorIdentity getIdentity() {
        return ModConfig.areAnchorsStatic() ? this : this.anchor;
    }

    public void clientTick() {
        if (this.level == null) return;

        WhisperingAnchorSoundInstance.playIfAudible(this.level, this.worldPosition);
    }

    public void serverTick() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        if (ModConfig.areAnchorsStatic()) return;
        if (this.anchor != null && this.anchor.isAlive() && this.anchor.isBoundTo(this.worldPosition)) return;

        AABB searchBox = AnchorEntity.getSearchArea(this.worldPosition);
        if (!checkIfAreaIsLoaded(searchBox, serverLevel)) return;

        this.anchor = claimExistingAnchor(serverLevel);
        if (this.anchor == null) {
            this.anchor = AnchorEntity.spawnFor(serverLevel, this.worldPosition);
            if (this.anchor != null) {
                transferIdentityTo(this.anchor);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean checkIfAreaIsLoaded(AABB area, ServerLevel level) {
        return level.hasChunksAt(BlockPos.containing(area.minX, area.minY, area.minZ), BlockPos.containing(area.maxX, area.maxY, area.maxZ));
    }

    private @Nullable AnchorEntity claimExistingAnchor(ServerLevel level) {
        List<AnchorEntity> found = AnchorEntity.findBoundTo(level, this.worldPosition);
        if (found.isEmpty()) return null;

        for (int i = 1; i < found.size(); i++) {
            found.get(i).discard();
        }

        return found.getFirst();
    }

    private void transferIdentityTo(AnchorEntity entity) {
        if (this.color == null && this.customName == null) return;

        entity.setColor(this.color);
        if (this.customName != null) {
            entity.rename(this.customName);
        }

        this.color = null;
        this.customName = null;
        markChangedAndSync();
    }

    public void copyIdentityFrom(AnchorEntity entity) {
        this.color = entity.getColor();
        this.customName = entity.getCustomName();
        markChangedAndSync();
    }

    @Override
    public @Nullable DyeColor getColor() {
        return this.color;
    }

    @Override
    public void setColor(@Nullable DyeColor color) {
        this.color = color;
        markChangedAndSync();
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.customName;
    }

    @Override
    public void rename(Component name) {
        this.customName = name;
        markChangedAndSync();
    }

    private void markChangedAndSync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.color = tag.contains(AnchorEntity.KEY_COLOR) ? DyeColor.byName(tag.getString(AnchorEntity.KEY_COLOR), null) : null;
        this.customName = tag.contains(AnchorEntity.KEY_CUSTOM_NAME) ? parseCustomNameSafe(tag.getString(AnchorEntity.KEY_CUSTOM_NAME), registries) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        AnchorEntity anchor = getAnchor();
        AnchorIdentity identity = anchor != null ? anchor : this;
        DyeColor savedColor = identity.getColor();
        Component savedName = identity.getCustomName();
        if (savedColor != null) {
            tag.putString(AnchorEntity.KEY_COLOR, savedColor.getName());
        }
        if (savedName != null) {
            tag.putString(AnchorEntity.KEY_CUSTOM_NAME, Component.Serializer.toJson(savedName, registries));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
