package net.grid.whisperinganchors.client.event;

import net.grid.whisperinganchors.WhisperingAnchors;
import net.grid.whisperinganchors.common.core.ModItems;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = WhisperingAnchors.MODID, value = Dist.CLIENT)
public final class KnownAnchorsScroll {

    private static final double SCROLL_SPEED = 2.0;

    private static ItemStack shownStack = ItemStack.EMPTY;
    private static int offset;
    private static int maxOffset;
    private static double scrolled;

    public static int getOffset(ItemStack stack, int max) {
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
