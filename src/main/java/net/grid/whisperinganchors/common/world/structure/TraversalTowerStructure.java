package net.grid.whisperinganchors.common.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TraversalTowerStructure extends Structure {

    public static final MapCodec<TraversalTowerStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Codec.intRange(0, 64).fieldOf("ground_level").forGetter(structure -> structure.groundLevel),
            Codec.intRange(-2048, 2048).fieldOf("min_scan_y").forGetter(structure -> structure.minScanY),
            Codec.intRange(-2048, 2048).fieldOf("max_scan_y").forGetter(structure -> structure.maxScanY),
            Codec.intRange(1, 512).fieldOf("scan_step").forGetter(structure -> structure.scanStep),
            Codec.intRange(1, 128).fieldOf("footprint").forGetter(structure -> structure.footprint),
            Codec.intRange(0, 128).fieldOf("max_terrain_difference").forGetter(structure -> structure.maxTerrainDifference),
            IntProvider.codec(0, 32).fieldOf("pillagers").forGetter(structure -> structure.pillagers),
            IntProvider.codec(0, 32).fieldOf("vindicators").forGetter(structure -> structure.vindicators),
            Codec.floatRange(0.0F, 1.0F).fieldOf("captain_chance").forGetter(structure -> structure.captainChance),
            TagKey.codec(Registries.BLOCK).fieldOf("spawn_floors").forGetter(structure -> structure.spawnFloors)
    ).apply(instance, TraversalTowerStructure::new));

    private static final int[][] CORNER_SAMPLES = {{0, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    private final Holder<StructureTemplatePool> startPool;
    private final int groundLevel;
    private final int minScanY;
    private final int maxScanY;
    private final int scanStep;
    private final int footprint;
    private final int maxTerrainDifference;
    private final IntProvider pillagers;
    private final IntProvider vindicators;
    private final float captainChance;
    private final TagKey<Block> spawnFloors;

    public TraversalTowerStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, int groundLevel, int minScanY, int maxScanY, int scanStep, int footprint, int maxTerrainDifference, IntProvider pillagers, IntProvider vindicators, float captainChance, TagKey<Block> spawnFloors) {
        super(settings);
        this.startPool = startPool;
        this.groundLevel = groundLevel;
        this.minScanY = minScanY;
        this.maxScanY = maxScanY;
        this.scanStep = scanStep;
        this.footprint = footprint;
        this.maxTerrainDifference = maxTerrainDifference;
        this.pillagers = pillagers;
        this.vindicators = vindicators;
        this.captainChance = captainChance;
        this.spawnFloors = spawnFloors;
    }

    @Override
    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        return this.coversBiomeBelow(context) ? this.findGenerationPoint(context) : Optional.empty();
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        if (!this.isBuildableSurface(context, chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ())) {
            return Optional.empty();
        }

        WorldgenRandom random = context.random();
        Rotation rotation = Rotation.getRandom(random);
        StructurePoolElement element = this.startPool.value().getRandomTemplate(random);
        if (element == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        }

        StructureTemplateManager templates = context.structureTemplateManager();
        BlockPos origin = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        BoundingBox box = element.getBoundingBox(templates, origin, rotation);
        int surface = context.chunkGenerator().getFirstOccupiedHeight((box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());

        PoolElementStructurePiece piece = new PoolElementStructurePiece(templates, element, origin, this.groundLevel, rotation, box, LiquidSettings.APPLY_WATERLOGGING);
        piece.move(0, surface - this.groundLevel, 0);

        return Optional.of(new GenerationStub(new BlockPos(chunkPos.getMiddleBlockX(), surface, chunkPos.getMiddleBlockZ()), builder -> {
            builder.addPiece(piece);
            this.addAttachedPieces(context, builder, piece);
        }));
    }

    private void addAttachedPieces(GenerationContext context, StructurePiecesBuilder builder, PoolElementStructurePiece parent) {
        Registry<StructureTemplatePool> pools = context.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        StructureTemplateManager templates = context.structureTemplateManager();
        WorldgenRandom random = context.random();

        for (StructureTemplate.StructureBlockInfo jigsaw : parent.getElement().getShuffledJigsawBlocks(templates, parent.getPosition(), parent.getRotation(), random)) {
            CompoundTag tag = jigsaw.nbt();
            if (tag == null) continue;

            StructureTemplatePool pool = pools.getOptional(Pools.parseKey(tag.getString("pool"))).orElse(null);
            if (pool == null) continue;

            for (StructurePoolElement element : pool.getShuffledTemplates(random)) {
                if (element != EmptyPoolElement.INSTANCE && attach(templates, builder, parent, jigsaw, element, random)) break;
            }
        }
    }

    private static boolean attach(StructureTemplateManager templates, StructurePiecesBuilder builder, PoolElementStructurePiece parent, StructureTemplate.StructureBlockInfo jigsaw, StructurePoolElement element, RandomSource random) {
        Direction facing = JigsawBlock.getFrontFacing(jigsaw.state());
        BlockPos attachment = jigsaw.pos().relative(facing);
        int jigsawHeight = jigsaw.pos().getY() - parent.getBoundingBox().minY();

        for (Rotation rotation : Rotation.getShuffled(random)) {
            for (StructureTemplate.StructureBlockInfo target : element.getShuffledJigsawBlocks(templates, BlockPos.ZERO, rotation, random)) {
                if (!JigsawBlock.canAttach(jigsaw, target)) continue;

                BlockPos origin = attachment.subtract(target.pos());
                int groundLevelDelta = parent.getGroundLevelDelta() - (jigsawHeight - target.pos().getY() + facing.getStepY());
                builder.addPiece(new PoolElementStructurePiece(templates, element, origin, groundLevelDelta, rotation, element.getBoundingBox(templates, origin, rotation), LiquidSettings.APPLY_WATERLOGGING));

                return true;
            }
        }

        return false;
    }

    private boolean coversBiomeBelow(GenerationContext context) {
        BiomeSource source = context.chunkGenerator().getBiomeSource();
        Climate.Sampler sampler = context.randomState().sampler();
        int quartX = QuartPos.fromBlock(context.chunkPos().getMiddleBlockX());
        int quartZ = QuartPos.fromBlock(context.chunkPos().getMiddleBlockZ());

        for (int y = this.minScanY; y <= this.maxScanY; y += this.scanStep) {
            if (context.validBiome().test(source.getNoiseBiome(quartX, QuartPos.fromBlock(y), quartZ, sampler))) return true;
        }

        return false;
    }

    private boolean isBuildableSurface(GenerationContext context, int x, int z) {
        ChunkGenerator generator = context.chunkGenerator();
        int step = Math.max(this.footprint / 2, 1);
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int[] sample : CORNER_SAMPLES) {
            int sampleX = x + sample[0] * step;
            int sampleZ = z + sample[1] * step;
            int surface = generator.getFirstOccupiedHeight(sampleX, sampleZ, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            int floor = generator.getFirstOccupiedHeight(sampleX, sampleZ, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            if (surface != floor) return false;

            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
        }

        return highest - lowest <= this.maxTerrainDifference;
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox boundingBox, ChunkPos chunkPos, PiecesContainer pieces) {
        BoundingBox structureBox = pieces.calculateBoundingBox();
        BlockPos center = structureBox.getCenter();
        if (SectionPos.blockToSectionCoord(center.getX()) != chunkPos.x || SectionPos.blockToSectionCoord(center.getZ()) != chunkPos.z) return;

        List<BlockPos> floors = this.findSpawnFloors(level, structureBox, chunkPos);
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
