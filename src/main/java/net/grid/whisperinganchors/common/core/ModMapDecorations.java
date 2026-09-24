package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModMapDecorations {
    private static final DeferredRegister<MapDecorationType> DECORATIONS = DeferredRegister.create(BuiltInRegistries.MAP_DECORATION_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<MapDecorationType, MapDecorationType> TRAVERSAL_TOWER = register("traversal_tower", true, 0x044e59, false, true);

    public static final DeferredHolder<MapDecorationType, MapDecorationType> ANCHOR = register("anchor", true, MapDecorationType.NO_MAP_COLOR, false, true);
    private static final Map<DyeColor, DeferredHolder<MapDecorationType, MapDecorationType>> DYED_ANCHORS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            DYED_ANCHORS.put(color, register(color.getName() + "_anchor", true, MapDecorationType.NO_MAP_COLOR, false, true));
        }
    }

    public static DeferredHolder<MapDecorationType, MapDecorationType> anchor(DyeColor color) {
        return DYED_ANCHORS.get(color);
    }

    private static DeferredHolder<MapDecorationType, MapDecorationType> register(String name, boolean showOnItemFrame, int mapColor, boolean explorationMapElement, boolean trackCount) {
        return DECORATIONS.register(name, () -> new MapDecorationType(WhisperingAnchors.modLoc(name), showOnItemFrame, mapColor, explorationMapElement, trackCount));
    }

    public static void register(IEventBus eventBus) {
        DECORATIONS.register(eventBus);
    }
}
