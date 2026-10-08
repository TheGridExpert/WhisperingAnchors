package net.grid.whisperinganchors.common.core;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfigs {

    public static final ModConfigSpec commonSpec;
    public static final Common COMMON;
    public static final ModConfigSpec serverSpec;
    public static final Server SERVER;

    private static volatile int syncedBellUseDuration = -1;

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, commonSpec);
        modContainer.registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    static {
        var commonPair = new ModConfigSpec.Builder().configure(Common::new);
        commonSpec = commonPair.getRight();
        COMMON = commonPair.getLeft();
        var serverPair = new ModConfigSpec.Builder().configure(Server::new);
        serverSpec = serverPair.getRight();
        SERVER = serverPair.getLeft();
    }

    public static int getBellUseDuration() {
        return commonSpec.isLoaded() ? COMMON.bellUseDuration.get() : COMMON.bellUseDuration.getDefault();
    }

    public static int getBellUseDuration(boolean clientSide) {
        return clientSide && syncedBellUseDuration > 0 ? syncedBellUseDuration : getBellUseDuration();
    }

    public static void syncBellUseDuration(int duration) {
        syncedBellUseDuration = duration;
    }

    public static int getBellCooldown() {
        return commonSpec.isLoaded() ? COMMON.bellCooldown.get() : COMMON.bellCooldown.getDefault();
    }

    public static int getBellFailedCooldown() {
        return commonSpec.isLoaded() ? COMMON.bellFailedCooldown.get() : COMMON.bellFailedCooldown.getDefault();
    }

    public static int getCompassCooldown() {
        return commonSpec.isLoaded() ? COMMON.compassCooldown.get() : COMMON.compassCooldown.getDefault();
    }

    public static boolean areAnchorsStatic() {
        return serverSpec.isLoaded() && SERVER.makeAnchorsStatic.get();
    }

    public static boolean hasStaticAnchorHitboxes() {
        return areAnchorsStatic() && SERVER.staticAnchorHitboxes.get();
    }

    public static class Common {

        public final ModConfigSpec.IntValue bellUseDuration;
        public final ModConfigSpec.IntValue bellCooldown;
        public final ModConfigSpec.IntValue bellFailedCooldown;
        public final ModConfigSpec.IntValue compassCooldown;

        public Common(ModConfigSpec.Builder builder) {
            this.bellUseDuration = builder
                    .comment("How many ticks the harkening bell has to ring before it teleports its ringer. The chime is stretched or shortened to match. On a server, its value is sent to the joining players.")
                    .defineInRange("bellUseDuration", 160, 20, 1200);
            this.bellCooldown = builder
                    .comment("How many ticks the harkening bell is on cooldown after teleporting its ringer.")
                    .defineInRange("bellCooldown", 80, 0, 72000);
            this.bellFailedCooldown = builder
                    .comment("How many ticks the harkening bell is on cooldown after its ringing is interrupted or finds no safe place to teleport to.")
                    .defineInRange("bellFailedCooldown", 20, 0, 72000);
            this.compassCooldown = builder
                    .comment("How many ticks the heeding compass is on cooldown after it is shaken, whether or not the shaking is finished.")
                    .defineInRange("compassCooldown", 20, 0, 72000);
        }
    }

    public static class Server {

        public final ModConfigSpec.BooleanValue makeAnchorsStatic;
        public final ModConfigSpec.BooleanValue staticAnchorHitboxes;

        public Server(ModConfigSpec.Builder builder) {
            this.makeAnchorsStatic = builder
                    .comment("When enabled, whispering anchors are drawn as still blocks instead of swinging entities with physics.")
                    .worldRestart()
                    .define("makeAnchorsStatic", false);
            this.staticAnchorHitboxes = builder
                    .comment("When enabled, the lower two blocks of static anchors have a hitbox and collision. Only used when makeAnchorsStatic is true.")
                    .worldRestart()
                    .define("staticAnchorHitboxes", true);
        }
    }
}
