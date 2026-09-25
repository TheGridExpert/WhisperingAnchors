package net.grid.whisperinganchors.common.world.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

public enum AnchorPart implements StringRepresentable {
    TOP("top", 0),
    MIDDLE("middle", 1),
    BOTTOM("bottom", 2);

    private final String name;
    private final int offsetFromTop;

    AnchorPart(String name, int offsetFromTop) {
        this.name = name;
        this.offsetFromTop = offsetFromTop;
    }

    public int offsetFromTop() {
        return this.offsetFromTop;
    }

    public @Nullable AnchorPart above() {
        return switch (this) {
            case TOP -> null;
            case MIDDLE -> TOP;
            case BOTTOM -> MIDDLE;
        };
    }

    public @Nullable AnchorPart below() {
        return switch (this) {
            case TOP -> MIDDLE;
            case MIDDLE -> BOTTOM;
            case BOTTOM -> null;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
