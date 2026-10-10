package net.grid.whisperinganchors.common.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record FlatSiteSearch(int radius, int footprint, int surroundings, int maxTerrainDifference) {

    public static final Codec<FlatSiteSearch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 4).fieldOf("radius").forGetter(FlatSiteSearch::radius),
            Codec.intRange(1, 16).fieldOf("footprint").forGetter(FlatSiteSearch::footprint),
            Codec.intRange(0, 48).fieldOf("surroundings").forGetter(FlatSiteSearch::surroundings),
            Codec.intRange(0, 128).fieldOf("max_terrain_difference").forGetter(FlatSiteSearch::maxTerrainDifference)
    ).apply(instance, FlatSiteSearch::new));

    private static final int COARSE_STEP = 8;
    private static final int FINE_STEP = 4;
    private static final int REFINED_SITES = 4;
    private static final double CORE_WEIGHT = 2.0;
    private static final double WATER_PENALTY = 16.0;

    public Optional<ChunkPos> findFlattest(Structure.GenerationContext context, Predicate<ChunkPos> eligible) {
        ChunkPos center = context.chunkPos();
        HeightCache heightCache = new HeightCache(context);
        List<Site> sites = new ArrayList<>();

        for (int x = -this.radius; x <= this.radius; x++) {
            for (int z = -this.radius; z <= this.radius; z++) {
                ChunkPos chunkPos = new ChunkPos(center.x + x, center.z + z);
                if (!eligible.test(chunkPos)) continue;

                int distance = x * x + z * z;
                calculateScore(heightCache, chunkPos, COARSE_STEP).ifPresent(score -> sites.add(new Site(chunkPos, score, distance)));
            }
        }

        return sites.stream()
                .sorted(Site.ORDER)
                .limit(REFINED_SITES)
                .flatMap(site -> calculateScore(heightCache, site.chunkPos(), FINE_STEP).map(score -> new Site(site.chunkPos(), score, site.distance())).stream())
                .min(Site.ORDER)
                .map(Site::chunkPos);
    }

    private Optional<Double> calculateScore(HeightCache heightCache, ChunkPos chunkPos, int coreStep) {
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int half = this.footprint / 2;
        int coreMinX = floorToStep(centerX - half, coreStep);
        int coreMaxX = ceilToStep(centerX + half, coreStep);
        int coreMinZ = floorToStep(centerZ - half, coreStep);
        int coreMaxZ = ceilToStep(centerZ + half, coreStep);

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        long sum = 0;
        int count = 0;

        for (int x = coreMinX; x <= coreMaxX; x += coreStep) {
            for (int z = coreMinZ; z <= coreMaxZ; z += coreStep) {
                int height = heightCache.getHeight(x, z);
                if (height == SurfaceSampler.WATER) return Optional.empty();

                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
                sum += height;
                count++;
            }
        }

        int range = highest - lowest;
        if (range > this.maxTerrainDifference) return Optional.empty();

        double base = (double) sum / count;
        double deviation = 0.0;
        int samples = 0;
        int reach = half + this.surroundings;

        for (int x = floorToStep(centerX - reach, COARSE_STEP); x <= ceilToStep(centerX + reach, COARSE_STEP); x += COARSE_STEP) {
            for (int z = floorToStep(centerZ - reach, COARSE_STEP); z <= ceilToStep(centerZ + reach, COARSE_STEP); z += COARSE_STEP) {
                if (x >= coreMinX && x <= coreMaxX && z >= coreMinZ && z <= coreMaxZ) continue;

                int height = heightCache.getHeight(x, z);
                deviation += height == SurfaceSampler.WATER ? WATER_PENALTY : Math.abs(height - base);
                samples++;
            }
        }

        return Optional.of(range * CORE_WEIGHT + (samples == 0 ? 0.0 : deviation / samples));
    }

    private static int floorToStep(int value, int step) {
        return Math.floorDiv(value, step) * step;
    }

    private static int ceilToStep(int value, int step) {
        return -Math.floorDiv(-value, step) * step;
    }

    private record Site(ChunkPos chunkPos, double score, int distance) {

        private static final Comparator<Site> ORDER = Comparator.comparingDouble(Site::score).thenComparingInt(Site::distance);
    }

    private static class HeightCache {

        private static final int UNKNOWN = Integer.MAX_VALUE;

        private final SurfaceSampler sampler;
        private final Long2IntOpenHashMap heights = new Long2IntOpenHashMap();

        private HeightCache(Structure.GenerationContext context) {
            this.sampler = SurfaceSampler.create(context);
            this.heights.defaultReturnValue(UNKNOWN);
        }

        private int getHeight(int x, int z) {
            long key = ChunkPos.asLong(x, z);
            int height = this.heights.get(key);
            if (height == UNKNOWN) {
                height = this.sampler.sample(x, z);
                this.heights.put(key, height);
            }

            return height;
        }
    }
}
