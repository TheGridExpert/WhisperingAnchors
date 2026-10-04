package net.grid.whisperinganchors.api;

public class AnchorFeatures {

    private static volatile boolean movingAnchorsHitEntities = true;
    private static volatile boolean anchorsStealable = true;

    public static boolean doMovingAnchorsHitEntities() {
        return movingAnchorsHitEntities;
    }

    public static void setMovingAnchorsHitEntities(boolean enabled) {
        movingAnchorsHitEntities = enabled;
    }

    public static boolean areAnchorsStealable() {
        return anchorsStealable;
    }

    public static void setAnchorsStealable(boolean enabled) {
        anchorsStealable = enabled;
    }
}
