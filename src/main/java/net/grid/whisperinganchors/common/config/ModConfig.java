package net.grid.whisperinganchors.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {

    public static final ModConfigSpec serverSpec;
    public static final Server SERVER;

    static {
        var serverPair = new ModConfigSpec.Builder().configure(Server::new);
        serverSpec = serverPair.getRight();
        SERVER = serverPair.getLeft();
    }

    public static boolean areAnchorsStatic() {
        return serverSpec.isLoaded() && SERVER.makeAnchorsStatic.get();
    }

    public static boolean hasStaticAnchorHitboxes() {
        return areAnchorsStatic() && SERVER.staticAnchorHitboxes.get();
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
