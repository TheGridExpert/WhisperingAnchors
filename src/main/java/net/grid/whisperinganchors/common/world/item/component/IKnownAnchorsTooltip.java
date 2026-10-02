package net.grid.whisperinganchors.common.world.item.component;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public interface IKnownAnchorsTooltip {

    int SHOWN_KNOWN_ANCHORS = 3;

    default void addTooltip(List<Component> tooltips) {
        List<DisplayAnchor> knownAnchors = getDisplayAnchors();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Vec3 centrePos = ignorePlayerPosition() ? Vec3.ZERO : player.position();

        List<DisplayAnchor> nearest = knownAnchors.stream()
                .filter(anchor -> anchor.pos().dimension().equals(player.level().dimension()))
                .sorted(Comparator.comparingDouble(anchor -> anchor.pos().pos().distToCenterSqr(centrePos)))
                .limit(SHOWN_KNOWN_ANCHORS)
                .toList();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchors").withStyle(ChatFormatting.GRAY));

        for (DisplayAnchor anchor : nearest) {
            double distance = Math.sqrt(anchor.pos().pos().distToCenterSqr(centrePos));
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor", anchor.name(), formatDistance(distance).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY));
        }

        int hidden = knownAnchors.size() - nearest.size();
        if (hidden > 0) {
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.more_known_anchors", hidden).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        tooltips.addAll(1, lines);
    }

    private static MutableComponent formatDistance(double distance) {
        return distance < 1000 ? Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor.meters", Mth.floor(distance)) : Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor.kilometers", String.format(Locale.ROOT, "%.1f", distance / 1000));
    }

    List<DisplayAnchor> getDisplayAnchors();

    default boolean ignorePlayerPosition() {
        return false;
    }

    record DisplayAnchor(GlobalPos pos, Component name) {}
}
