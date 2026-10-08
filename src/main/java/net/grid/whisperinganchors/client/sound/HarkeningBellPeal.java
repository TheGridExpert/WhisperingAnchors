package net.grid.whisperinganchors.client.sound;

import net.grid.whisperinganchors.common.core.ModConfigs;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

public class HarkeningBellPeal {

    private static final int LEAD_TICKS = 5;
    private static final int FINAL_TICKS = 9;
    private static final int[] SEGMENT_TICKS = {17, 21, 22, 23, 25, 38};
    private static final int ALL_SEGMENT_TICKS = Arrays.stream(SEGMENT_TICKS).sum();
    private static final float[][] SEGMENT_RINGS = {
            {1.0F, 8.0F},
            {4.0F, 11.5F},
            {5.0F, 13.0F},
            {5.0F, 14.0F},
            {4.0F, 12.0F},
            {3.9F}
    };
    private static final float FINAL_RING = 4.2F;
    private static final int FINAL_SEGMENT = SEGMENT_TICKS.length;
    private static final int[] DUPLICATION_ORDER = orderFromMiddle();
    private static final int[] REMOVAL_ORDER = orderForRemoval();

    private static @Nullable HarkeningBellPeal current;

    private final int duration;
    private final int[] segments;
    private final int[] startTicks;
    private final float[] ringTicks;

    private HarkeningBellPeal(int duration) {
        this.duration = duration;

        int fill = duration - FINAL_TICKS;
        int[] copies = countCopies(fill - LEAD_TICKS >= ALL_SEGMENT_TICKS ? fill - LEAD_TICKS : fill);
        List<Integer> played = new ArrayList<>();
        for (int segment = 0; segment < SEGMENT_TICKS.length; segment++) {
            for (int copy = 0; copy < copies[segment]; copy++) {
                played.add(segment);
            }
        }

        int filled = 0;
        for (int segment : played) {
            filled += SEGMENT_TICKS[segment];
        }
        int lead = Math.min(LEAD_TICKS, fill - filled);
        int spare = fill - lead - filled;
        int gaps = Math.max(1, played.size());

        this.segments = new int[played.size() + 1];
        this.startTicks = new int[played.size() + 1];
        List<Float> rings = new ArrayList<>();
        int tick = lead;
        for (int entry = 0; entry < played.size(); entry++) {
            int segment = played.get(entry);
            this.segments[entry] = segment;
            this.startTicks[entry] = tick;
            for (float ring : SEGMENT_RINGS[segment]) {
                rings.add(tick + ring);
            }
            tick += SEGMENT_TICKS[segment] + spare / gaps + (entry < spare % gaps ? 1 : 0);
        }

        if (played.isEmpty()) {
            tick += spare;
        }
        this.segments[played.size()] = FINAL_SEGMENT;
        this.startTicks[played.size()] = tick;
        if (rings.isEmpty()) {
            rings.add(tick + FINAL_RING);
        }

        this.ringTicks = new float[rings.size()];
        for (int i = 0; i < rings.size(); i++) {
            this.ringTicks[i] = rings.get(i);
        }
    }

    public static HarkeningBellPeal get() {
        int duration = ModConfigs.getBellUseDuration(true);
        if (current == null || current.duration != duration) {
            current = new HarkeningBellPeal(duration);
        }

        return current;
    }

    public int getDuration() {
        return duration;
    }

    public float[] getRingTicks() {
        return ringTicks;
    }

    public OptionalInt findSegmentStartingAt(int tick) {
        for (int entry = 0; entry < startTicks.length; entry++) {
            if (startTicks[entry] == tick) {
                return OptionalInt.of(segments[entry]);
            }
        }

        return OptionalInt.empty();
    }

    private static int[] countCopies(int fill) {
        int[] copies = new int[SEGMENT_TICKS.length];
        int total = 0;
        for (int segment = 0; segment < SEGMENT_TICKS.length; segment++) {
            copies[segment] = 1;
            total += SEGMENT_TICKS[segment];
        }

        for (int step = 0; step < REMOVAL_ORDER.length && total > fill; step++) {
            int segment = REMOVAL_ORDER[step];
            copies[segment] = 0;
            total -= SEGMENT_TICKS[segment];
        }

        for (int step = 0; total + SEGMENT_TICKS[DUPLICATION_ORDER[step % DUPLICATION_ORDER.length]] <= fill; step++) {
            int segment = DUPLICATION_ORDER[step % DUPLICATION_ORDER.length];
            copies[segment]++;
            total += SEGMENT_TICKS[segment];
        }

        return copies;
    }

    private static int[] orderFromMiddle() {
        int count = SEGMENT_TICKS.length - 2;
        int[] order = new int[count];
        for (int i = 0; i < count; i++) {
            int step = (i + 1) / 2;
            order[i] = 1 + (count - 1) / 2 + (i % 2 == 0 ? -step : step);
        }

        return order;
    }

    private static int[] orderForRemoval() {
        int[] order = new int[SEGMENT_TICKS.length];
        System.arraycopy(DUPLICATION_ORDER, 0, order, 0, DUPLICATION_ORDER.length);
        order[DUPLICATION_ORDER.length] = SEGMENT_TICKS.length - 1;
        order[DUPLICATION_ORDER.length + 1] = 0;

        return order;
    }
}
