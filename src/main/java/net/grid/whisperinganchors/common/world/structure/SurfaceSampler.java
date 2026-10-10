package net.grid.whisperinganchors.common.world.structure;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.structure.Structure;

interface SurfaceSampler {

    int WATER = Integer.MIN_VALUE;

    int sample(int x, int z);

    static SurfaceSampler create(Structure.GenerationContext context) {
        if (context.chunkGenerator() instanceof NoiseBasedChunkGenerator generator) {
            return new Noise(context, generator);
        }

        return new Heightmaps(context);
    }

    class Noise implements SurfaceSampler {

        private static final double PRELIMINARY_THRESHOLD = 0.390625;

        private final DensityFunction finalDensity;
        private final DensityFunction initialDensity;
        private final int minY;
        private final int maxY;
        private final int cellHeight;
        private final int seaLevel;

        private Noise(Structure.GenerationContext context, NoiseBasedChunkGenerator generator) {
            NoiseRouter router = context.randomState().router();
            NoiseSettings settings = generator.generatorSettings().value().noiseSettings().clampToHeightAccessor(context.heightAccessor());
            this.finalDensity = router.finalDensity().mapAll(Noise::cacheColumns);
            this.initialDensity = router.initialDensityWithoutJaggedness().mapAll(Noise::cacheColumns);
            this.minY = settings.minY();
            this.maxY = settings.minY() + settings.height();
            this.cellHeight = settings.getCellHeight();
            this.seaLevel = generator.getSeaLevel();
        }

        @Override
        public int sample(int x, int z) {
            int corner = Math.floorDiv(findPreliminarySurface(x, z), this.cellHeight) * this.cellHeight;
            double density = computeDensity(x, corner, z);

            if (density > 0.0) {
                double above = computeDensity(x, corner + this.cellHeight, z);
                while (above > 0.0 && corner + this.cellHeight < this.maxY) {
                    corner += this.cellHeight;
                    density = above;
                    above = computeDensity(x, corner + this.cellHeight, z);
                }

                return above > 0.0 ? corner : interpolateSurface(corner, density, above);
            }

            double below = density;
            while (below <= 0.0 && corner > this.minY) {
                corner -= this.cellHeight;
                density = below;
                below = computeDensity(x, corner, z);
            }

            return below > 0.0 ? interpolateSurface(corner, below, density) : this.minY;
        }

        private int interpolateSurface(int corner, double solid, double air) {
            int surface = corner + (int) (this.cellHeight * solid / (solid - air));
            return surface + 1 < this.seaLevel ? WATER : surface;
        }

        private int findPreliminarySurface(int x, int z) {
            for (int y = this.maxY; y >= this.minY; y -= this.cellHeight) {
                if (this.initialDensity.compute(new DensityFunction.SinglePointContext(x, y, z)) > PRELIMINARY_THRESHOLD) return y;
            }

            return this.minY;
        }

        private double computeDensity(int x, int y, int z) {
            return this.finalDensity.compute(new DensityFunction.SinglePointContext(x, y, z));
        }

        private static DensityFunction cacheColumns(DensityFunction function) {
            if (!(function instanceof DensityFunctions.MarkerOrMarked marker)) return function;

            String type = ((StringRepresentable) marker.type()).getSerializedName();
            return type.equals("flat_cache") || type.equals("cache_2d") ? new ColumnCache(marker.wrapped()) : marker.wrapped();
        }
    }

    class Heightmaps implements SurfaceSampler {

        private final Structure.GenerationContext context;
        private final ChunkGenerator generator;

        private Heightmaps(Structure.GenerationContext context) {
            this.context = context;
            this.generator = context.chunkGenerator();
        }

        @Override
        public int sample(int x, int z) {
            int surface = this.generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, this.context.heightAccessor(), this.context.randomState());
            if (surface >= this.generator.getSeaLevel()) return surface;

            int floor = this.generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, this.context.heightAccessor(), this.context.randomState());
            return floor < surface ? WATER : surface;
        }
    }

    class ColumnCache implements DensityFunction {

        private final DensityFunction wrapped;
        private long lastColumn = ChunkPos.INVALID_CHUNK_POS;
        private double lastValue;

        private ColumnCache(DensityFunction wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public double compute(FunctionContext context) {
            long column = ChunkPos.asLong(context.blockX(), context.blockZ());
            if (column != this.lastColumn) {
                this.lastColumn = column;
                this.lastValue = this.wrapped.compute(context);
            }

            return this.lastValue;
        }

        @Override
        public void fillArray(double[] array, ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(array, this);
        }

        @Override
        public DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(new ColumnCache(this.wrapped.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return this.wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return this.wrapped.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.wrapped.codec();
        }
    }
}
