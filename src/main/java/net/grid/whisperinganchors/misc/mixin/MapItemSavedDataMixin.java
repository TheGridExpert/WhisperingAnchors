package net.grid.whisperinganchors.misc.mixin;

import net.grid.whisperinganchors.common.world.block.AnchorIdentity;
import net.grid.whisperinganchors.common.world.item.map.AnchorMarker;
import net.grid.whisperinganchors.common.world.item.map.AnchorMarkerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin implements AnchorMarkerData {

    @Unique
    private static final String WHISPERING_ANCHORS$ANCHORS_KEY = "whispering_anchors:anchors";

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final public int centerX;
    @Shadow @Final public int centerZ;
    @Shadow @Final public ResourceKey<Level> dimension;
    @Shadow @Final public byte scale;
    @Shadow @Final Map<String, MapDecoration> decorations;

    @Unique
    private final Map<String, AnchorMarker> whisperingAnchors$anchorMarkers = new HashMap<>();

    @Shadow
    public abstract void addDecoration(Holder<MapDecorationType> decorationType, @Nullable LevelAccessor level, String id, double x, double z, double yRot, @Nullable Component displayName);

    @Shadow
    public abstract void removeDecoration(String identifier);

    @Shadow
    public abstract boolean isTrackedCountOverLimit(int trackedCount);

    @Inject(method = "load", at = @At("RETURN"))
    private static void whisperingAnchors$loadAnchors(CompoundTag tag, HolderLookup.Provider registries, CallbackInfoReturnable<MapItemSavedData> cir) {
        if (!tag.contains(WHISPERING_ANCHORS$ANCHORS_KEY)) return;

        AnchorMarkerData data = (AnchorMarkerData) cir.getReturnValue();
        AnchorMarker.LIST_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(WHISPERING_ANCHORS$ANCHORS_KEY))
                .resultOrPartial(error -> LOGGER.warn("Failed to parse map anchor: '{}'", error))
                .orElse(List.of())
                .forEach(marker -> data.whisperingAnchors$addAnchor(marker, null));
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void whisperingAnchors$saveAnchors(CompoundTag tag, HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.whisperingAnchors$anchorMarkers.isEmpty()) return;

        tag.put(WHISPERING_ANCHORS$ANCHORS_KEY, AnchorMarker.LIST_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), List.copyOf(this.whisperingAnchors$anchorMarkers.values())).getOrThrow());
    }

    @Inject(method = "locked", at = @At("RETURN"))
    private void whisperingAnchors$copyAnchors(CallbackInfoReturnable<MapItemSavedData> cir) {
        AnchorMarkerData copy = (AnchorMarkerData) cir.getReturnValue();
        this.whisperingAnchors$anchorMarkers.values().forEach(marker -> copy.whisperingAnchors$addAnchor(marker, null));
    }

    @Inject(method = "checkBanners", at = @At("TAIL"))
    private void whisperingAnchors$checkAnchors(BlockGetter reader, int x, int z, CallbackInfo ci) {
        Iterator<Map.Entry<String, AnchorMarker>> iterator = this.whisperingAnchors$anchorMarkers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, AnchorMarker> entry = iterator.next();
            BlockPos pos = entry.getValue().pos();
            if (pos.getX() != x || pos.getZ() != z) continue;

            if (!AnchorMarker.isAnchor(reader, pos)) {
                iterator.remove();
                removeDecoration(entry.getKey());
                continue;
            }

            if (!(reader instanceof Level level)) continue;

            AnchorIdentity anchor = AnchorIdentity.find(level, pos);
            if (anchor == null) continue;

            AnchorMarker current = AnchorMarker.of(pos, anchor);
            if (!current.equals(entry.getValue())) {
                entry.setValue(current);
                whisperingAnchors$decorate(current, level);
            }
        }
    }

    @Override
    public boolean whisperingAnchors$toggleAnchor(Level level, BlockPos top) {
        if (level.dimension() != this.dimension) return false;

        int scaleFactor = 1 << this.scale;
        double mapX = (top.getX() + 0.5 - this.centerX) / scaleFactor;
        double mapZ = (top.getZ() + 0.5 - this.centerZ) / scaleFactor;
        if (mapX < -63.0 || mapZ < -63.0 || mapX > 63.0 || mapZ > 63.0) return false;
        if (!AnchorMarker.isAnchor(level, top)) return false;

        AnchorMarker marker = AnchorMarker.of(top, AnchorIdentity.find(level, top));
        if (this.whisperingAnchors$anchorMarkers.remove(marker.getId()) != null) {
            removeDecoration(marker.getId());
            return true;
        }

        if (isTrackedCountOverLimit(MapItemSavedData.TRACKED_DECORATION_LIMIT)) return false;

        whisperingAnchors$addAnchor(marker, level);
        return true;
    }

    @Override
    public void whisperingAnchors$addAnchor(AnchorMarker marker, @Nullable LevelAccessor level) {
        this.whisperingAnchors$anchorMarkers.put(marker.getId(), marker);
        whisperingAnchors$decorate(marker, level);
    }

    @Unique
    private void whisperingAnchors$decorate(AnchorMarker marker, @Nullable LevelAccessor level) {
        addDecoration(marker.getDecoration(), level, marker.getId(), marker.pos().getX() + 0.5, marker.pos().getZ() + 0.5, 180.0, marker.name().orElse(null));

        MapDecoration decoration = this.decorations.get(marker.getId());
        if (decoration == null) return;

        int scaleFactor = 1 << this.scale;
        int pixelX = Math.floorDiv(marker.pos().getX() - this.centerX, scaleFactor);
        int pixelZ = Math.floorDiv(marker.pos().getZ() - this.centerZ, scaleFactor);
        byte x = (byte) Mth.clamp(2 * pixelX, -128, 127);
        byte y = (byte) Mth.clamp(2 * pixelZ + 2, -128, 127);

        this.decorations.put(marker.getId(), new MapDecoration(decoration.type(), x, y, decoration.rot(), decoration.name()));
    }
}
