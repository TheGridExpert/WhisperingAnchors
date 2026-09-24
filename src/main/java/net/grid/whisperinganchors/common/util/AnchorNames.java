package net.grid.whisperinganchors.common.util;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.List;

public final class AnchorNames {

    private static final List<String> STEMS = List.of(
            "Ash", "Barrow", "Bleak", "Bone", "Cinder", "Drear", "Dread", "Dusk",
            "Elder", "Ember", "Fen", "Gaunt", "Gloam", "Grim", "Hollow", "Iron",
            "Mire", "Morrow", "Mourn", "Murk", "Night", "Pale", "Quiet", "Rime",
            "Rust", "Sable", "Salt", "Shroud", "Silt", "Sorrow", "Stone", "Tarn",
            "Thorn", "Umbra", "Vane", "Vesper", "Wane", "Weald", "Wither", "Wraith"
    );

    private static final List<String> ENDINGS = List.of(
            "bell", "bough", "brand", "cairn", "chime", "crag", "deep", "fall",
            "fold", "gate", "glen", "hallow", "hold", "knell", "marrow", "mere",
            "moor", "reach", "rune", "shade", "spire", "thicket", "tide", "toll",
            "vale", "veil", "wake", "ward", "watch", "well", "wend", "wynd"
    );

    private static final List<String> EPITHETS = List.of(
            "Vigil", "Requiem", "Dirge", "Hymn", "Lament", "Echo", "Whisper", "Chant",
            "Elegy", "Psalm", "Verse", "Warden", "Sentinel", "Rest", "Rite", "Oath",
            "Vow", "Song", "Call", "Cry", "Hush", "Lull", "Murmur", "Peal",
            "Refrain", "Threnody", "Tremor", "Sigh", "Keen", "Omen", "Portent", "Litany",
            "Reverie", "Solace", "Augur", "Beacon", "Tocsin", "Descant", "Cadence", "Antiphon"
    );

    private static final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;

    public static Component generate(GlobalPos pos) {
        RandomSource random = RandomSource.create(mix(pos.pos().asLong() ^ pos.dimension().location().hashCode() * GOLDEN_RATIO));
        String stem = pick(random, STEMS);
        String ending = pick(random, ENDINGS);

        if (stem.charAt(stem.length() - 1) == ending.charAt(0)) {
            ending = ending.substring(1);
        }

        return Component.literal(stem + ending + " " + pick(random, EPITHETS));
    }

    private static long mix(long value) {
        value = (value ^ value >>> 33) * 0xFF51AFD7ED558CCDL;
        value = (value ^ value >>> 33) * 0xC4CEB9FE1A85EC53L;

        return value ^ value >>> 33;
    }

    private static String pick(RandomSource random, List<String> words) {
        return words.get(random.nextInt(words.size()));
    }
}