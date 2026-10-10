package net.grid.whisperinganchors.common.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TraversalTowerStructure extends Structure {

    public static final MapCodec<TraversalTowerStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
            HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
            Codec.intRange(0, JigsawStructure.MAX_DEPTH).fieldOf("size").forGetter(structure -> structure.maxDepth),
            Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter),
            Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(structure -> structure.poolAliases),
            Codec.intRange(-2048, 2048).fieldOf("min_scan_y").forGetter(structure -> structure.minScanY),
            Codec.intRange(-2048, 2048).fieldOf("max_scan_y").forGetter(structure -> structure.maxScanY),
            Codec.intRange(1, 512).fieldOf("scan_step").forGetter(structure -> structure.scanStep),
            FlatSiteSearch.CODEC.fieldOf("site_search").forGetter(structure -> structure.siteSearch),
            IntProvider.codec(0, 32).fieldOf("pillagers").forGetter(structure -> structure.pillagers),
            IntProvider.codec(0, 32).fieldOf("vindicators").forGetter(structure -> structure.vindicators),
            Codec.floatRange(0.0F, 1.0F).fieldOf("captain_chance").forGetter(structure -> structure.captainChance),
            TagKey.codec(Registries.BLOCK).fieldOf("spawn_floors").forGetter(structure -> structure.spawnFloors)
    ).apply(instance, TraversalTowerStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final HeightProvider startHeight;
    private final int maxDepth;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final int minScanY;
    private final int maxScanY;
    private final int scanStep;
    private final FlatSiteSearch siteSearch;
    private final IntProvider pillagers;
    private final IntProvider vindicators;
    private final float captainChance;
    private final TagKey<Block> spawnFloors;

    public TraversalTowerStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, HeightProvider startHeight, int maxDepth, int maxDistanceFromCenter, List<PoolAliasBinding> poolAliases, int minScanY, int maxScanY, int scanStep, FlatSiteSearch siteSearch, IntProvider pillagers, IntProvider vindicators, float captainChance, TagKey<Block> spawnFloors) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.startHeight = startHeight;
        this.maxDepth = maxDepth;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.minScanY = minScanY;
        this.maxScanY = maxScanY;
        this.scanStep = scanStep;
        this.siteSearch = siteSearch;
        this.pillagers = pillagers;
        this.vindicators = vindicators;
        this.captainChance = captainChance;
        this.spawnFloors = spawnFloors;
    }

    @Override
    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        return findGenerationPoint(context);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return this.siteSearch.findFlattest(context, chunkPos -> coversBiomeBelow(context, chunkPos)).flatMap(chunkPos -> createStub(context, chunkPos));
    }

    private Optional<GenerationStub> createStub(GenerationContext context, ChunkPos chunkPos) {
        int height = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos origin = new BlockPos(chunkPos.getMiddleBlockX(), height, chunkPos.getMiddleBlockZ());

        return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName, this.maxDepth, origin, true, Optional.of(Heightmap.Types.WORLD_SURFACE_WG), this.maxDistanceFromCenter, PoolAliasLookup.create(this.poolAliases, origin, context.seed()), DimensionPadding.ZERO, LiquidSettings.APPLY_WATERLOGGING);
    }

    private boolean coversBiomeBelow(GenerationContext context, ChunkPos chunkPos) {
        BiomeSource source = context.chunkGenerator().getBiomeSource();
        Climate.Sampler sampler = context.randomState().sampler();
        int quartX = QuartPos.fromBlock(chunkPos.getMiddleBlockX());
        int quartZ = QuartPos.fromBlock(chunkPos.getMiddleBlockZ());

        for (int y = this.minScanY; y <= this.maxScanY; y += this.scanStep) {
            if (context.validBiome().test(source.getNoiseBiome(quartX, QuartPos.fromBlock(y), quartZ, sampler))) return true;
        }

        return false;
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox boundingBox, ChunkPos chunkPos, PiecesContainer pieces) {
        BoundingBox structureBox = pieces.calculateBoundingBox();
        BlockPos center = pieces.pieces().getFirst().getBoundingBox().getCenter();
        if (SectionPos.blockToSectionCoord(center.getX()) != chunkPos.x || SectionPos.blockToSectionCoord(center.getZ()) != chunkPos.z) return;

        List<BlockPos> floors = findSpawnFloors(level, structureBox, chunkPos);
        if (floors.isEmpty()) return;

        RandomSource spawnRandom = RandomSource.create(level.getSeed()).forkPositional().at(center);
        int pillagerCount = this.pillagers.sample(spawnRandom);
        int vindicatorCount = this.vindicators.sample(spawnRandom);
        boolean captain = spawnRandom.nextFloat() < this.captainChance;

        for (int i = 0; i < pillagerCount && !floors.isEmpty(); i++) {
            spawnRaider(level, EntityType.PILLAGER, removeRandom(floors, spawnRandom), captain && i == 0);
        }

        for (int i = 0; i < vindicatorCount && !floors.isEmpty(); i++) {
            spawnRaider(level, EntityType.VINDICATOR, removeRandom(floors, spawnRandom), false);
        }
    }

    private List<BlockPos> findSpawnFloors(WorldGenLevel level, BoundingBox box, ChunkPos chunkPos) {
        List<BlockPos> floors = new ArrayList<>();
        int minX = Math.max(box.minX(), chunkPos.getMinBlockX());
        int maxX = Math.min(box.maxX(), chunkPos.getMaxBlockX());
        int minZ = Math.max(box.minZ(), chunkPos.getMinBlockZ());
        int maxZ = Math.min(box.maxZ(), chunkPos.getMaxBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = box.minY() + 1; y < box.maxY(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos.below()).is(this.spawnFloors)
                            && level.getBlockState(pos).isAir()
                            && level.getBlockState(pos.above()).isAir()) {
                        floors.add(pos);
                    }
                }
            }
        }

        return floors;
    }

    private static BlockPos removeRandom(List<BlockPos> floors, RandomSource random) {
        return floors.remove(random.nextInt(floors.size()));
    }

    private static void spawnRaider(WorldGenLevel level, EntityType<? extends Raider> type, BlockPos pos, boolean captain) {
        Raider raider = type.create(level.getLevel());
        if (raider == null) return;

        raider.setPersistenceRequired();
        raider.setPatrolLeader(captain);
        raider.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        raider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(raider);
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.TRAVERSAL_TOWER.get();
    }
}
