package net.grid.whisperinganchors.client.event;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModItems;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.grid.whisperinganchors.common.world.item.component.KnownAnchors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = WhisperingAnchors.MODID, value = Dist.CLIENT)
public final class KnownAnchorsTooltipHandler {

    private static final int SHOWN_KNOWN_ANCHORS = 3;
    private static final double SCROLL_SPEED = 2.0;

    private static ItemStack shownStack = ItemStack.EMPTY;
    private static int offset;
    private static int maxOffset;
    private static double scrolled;

    @SubscribeEvent
    public static void addKnownAnchors(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (player == null || !stack.is(ModItems.HEEDING_COMPASS)) return;

        KnownAnchors knownAnchors = stack.get(ModDataComponents.KNOWN_ANCHORS);
        if (knownAnchors == null || knownAnchors.anchors().isEmpty()) return;

        List<AnchorAttunement> sorted = knownAnchors.anchors().stream()
                .filter(anchor -> anchor.anchorPos().dimension().equals(player.level().dimension()))
                .sorted(Comparator.comparingDouble(anchor -> getWorldPosition(player.level(), anchor).distanceToSqr(player.position())))
                .toList();
        if (sorted.isEmpty()) return;

        int offset = getOffset(stack, sorted.size() - SHOWN_KNOWN_ANCHORS);
        List<AnchorAttunement> nearest = sorted.subList(offset, Math.min(sorted.size(), offset + SHOWN_KNOWN_ANCHORS));

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchors").withStyle(ChatFormatting.GRAY));

        if (offset > 0) {
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.closer_known_anchors", offset).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        for (AnchorAttunement anchor : nearest) {
            double distance = getWorldPosition(player.level(), anchor).distanceTo(player.position());
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor", anchor.getDisplayName(), formatDistance(distance).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY));
        }

        int hidden = knownAnchors.anchors().size() - offset - nearest.size();
        if (hidden > 0) {
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.more_known_anchors", hidden).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        event.getToolTip().addAll(1, lines);
    }

    private static Vec3 getWorldPosition(Level level, AnchorAttunement anchor) {
        return SubLevelHandler.INSTANCE.toWorldPosition(level, Vec3.atCenterOf(anchor.anchorPos().pos()));
    }

    private static MutableComponent formatDistance(double distance) {
        return distance < 1000 ? Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor.meters", Mth.floor(distance)) : Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor.kilometers", String.format(Locale.ROOT, "%.1f", distance / 1000));
    }

    private static int getOffset(ItemStack stack, int max) {
        if (!ItemStack.isSameItemSameComponents(stack, shownStack)) {
            shownStack = stack.copy();
            offset = 0;
            scrolled = 0;
        }

        maxOffset = Math.max(0, max);
        offset = Mth.clamp(offset, 0, maxOffset);
        return offset;
    }

    @SubscribeEvent
    public static void scrollTooltip(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !screen.getMenu().getCarried().isEmpty()) return;

        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !slot.getItem().is(ModItems.HEEDING_COMPASS) || !ItemStack.isSameItemSameComponents(slot.getItem(), shownStack)) return;
        if (maxOffset <= 0 || event.getScrollDeltaY() == 0) return;

        scrolled -= event.getScrollDeltaY();
        int steps = (int) (scrolled / SCROLL_SPEED);
        scrolled -= steps * SCROLL_SPEED;
        offset = Mth.clamp(offset + steps, 0, maxOffset);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void resetOnClose(ScreenEvent.Closing event) {
        shownStack = ItemStack.EMPTY;
        offset = 0;
        maxOffset = 0;
        scrolled = 0;
    }
}
