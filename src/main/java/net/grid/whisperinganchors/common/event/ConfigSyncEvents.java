package net.grid.whisperinganchors.common.event;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModConfigs;
import net.grid.whisperinganchors.common.network.BellUseDurationPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = WhisperingAnchors.MODID)
public class ConfigSyncEvents {

    @SubscribeEvent
    public static void syncOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, BellUseDurationPayload.fromConfig());
        }
    }

    public static void syncOnReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ModConfigs.commonSpec) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> PacketDistributor.sendToAllPlayers(BellUseDurationPayload.fromConfig()));
        }
    }
}
