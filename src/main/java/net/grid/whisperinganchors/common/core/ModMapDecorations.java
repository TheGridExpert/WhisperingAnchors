package net.grid.whisperinganchors.common.core;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMapDecorations {
    private static final DeferredRegister<MapDecorationType> DECORATIONS = DeferredRegister.create(BuiltInRegistries.MAP_DECORATION_TYPE, WhisperingAnchors.MODID);

    public static final DeferredHolder<MapDecorationType, MapDecorationType> TRAVERSAL_TOWER = register("traversal_tower", true, 0x044e59, false, true);

    private static DeferredHolder<MapDecorationType, MapDecorationType> register(String name, boolean showOnItemFrame, int mapColor, boolean explorationMapElement, boolean trackCount) {
        return DECORATIONS.register(name, () -> new MapDecorationType(WhisperingAnchors.modLoc(name), showOnItemFrame, mapColor, explorationMapElement, trackCount));
    }

    public static void register(IEventBus eventBus) {
        DECORATIONS.register(eventBus);
    }
}
