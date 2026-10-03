package net.grid.whisperinganchors.common.world.item;

import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.core.ModDataComponents;
import net.grid.whisperinganchors.common.core.ModStructures;
import net.grid.whisperinganchors.common.integration.SubLevelHandler;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.item.component.AnchorAttunement;
import net.grid.whisperinganchors.common.world.item.component.KnownAnchors;
import net.grid.whisperinganchors.misc.mixin.accessor.SinglePoolElementAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class HeedingCompassItem extends AttunableItem {

    public static final int USE_DURATION = 60;

    private static final int SEARCH_RADIUS = 100;

    private static final int SHOWN_KNOWN_ANCHORS = 3;

    private final Map<LivingEntity, Optional<AnchorAttunement>> pendingSearches = new WeakHashMap<>();

    public HeedingCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    protected @Nullable AnchorAttunement onAttunementLost(ServerLevel level, Entity holder, ItemStack stack, AnchorAttunement lost) {
        ServerLevel anchorLevel = level.getServer().getLevel(lost.anchorPos().dimension());
        if (anchorLevel == null || !anchorLevel.isLoaded(lost.anchorPos().pos())) return lost;

        BlockPos anchorPos = findUnknownAnchor(level, holder.blockPosition(), stack.getOrDefault(ModDataComponents.KNOWN_ANCHORS, KnownAnchors.EMPTY));
        return anchorPos == null ? null : attunementAt(level, anchorPos);
    }

    @Override
    protected InteractionResult useOnAnchor(UseOnContext context, BlockState state) {
        Level level = context.getLevel();

        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            KnownAnchors knownAnchors = stack.getOrDefault(ModDataComponents.KNOWN_ANCHORS, KnownAnchors.EMPTY);
            stack.set(ModDataComponents.KNOWN_ANCHORS, knownAnchors.with(AnchorAttunement.of(level, context.getClickedPos(), state)));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (level instanceof ServerLevel serverLevel) {
            BlockPos anchorPos = findUnknownAnchor(serverLevel, player.blockPosition(), stack.getOrDefault(ModDataComponents.KNOWN_ANCHORS, KnownAnchors.EMPTY));
            this.pendingSearches.put(player, Optional.ofNullable(anchorPos).map(pos -> attunementAt(serverLevel, pos)));
        }

        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!this.pendingSearches.containsKey(livingEntity)) return stack;

        Optional<AnchorAttunement> result = this.pendingSearches.remove(livingEntity);

        if (result.isEmpty()) {
            if (livingEntity instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.whispering_anchors.heeding_compass.cannot_find"), true);
            }
        } else {
            stack.set(ModDataComponents.ANCHOR_ATTUNEMENT, result.get());
        }

        return stack;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        this.pendingSearches.remove(entity);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CUSTOM;
    }

    private static @Nullable BlockPos findUnknownAnchor(ServerLevel level, BlockPos origin, KnownAnchors knownAnchors) {
        Holder<Structure> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ModStructures.TRAVERSAL_TOWER).orElse(null);
        if (structure == null) return null;

        Set<BlockPos> known = knownAnchors.anchors().stream()
                .map(AnchorAttunement::anchorPos)
                .filter(pos -> pos.dimension().equals(level.dimension()))
                .map(GlobalPos::pos)
                .collect(Collectors.toSet());

        ChunkGeneratorStructureState generatorState = level.getChunkSource().getGeneratorState();
        BlockPos closest = null;

        for (StructurePlacement placement : generatorState.getPlacementsForStructure(structure)) {
            if (!(placement instanceof RandomSpreadStructurePlacement spread)) continue;

            BlockPos found = searchSpread(level, structure, spread, generatorState.getLevelSeed(), origin, known);
            if (found != null && (closest == null || horizontalDistSqr(found, origin) < horizontalDistSqr(closest, origin))) {
                closest = found;
            }
        }

        return closest;
    }

    private static @Nullable BlockPos searchSpread(ServerLevel level, Holder<Structure> structure, RandomSpreadStructurePlacement spread, long seed, BlockPos origin, Set<BlockPos> known) {
        int spacing = spread.spacing();
        int originX = SectionPos.blockToSectionCoord(origin.getX());
        int originZ = SectionPos.blockToSectionCoord(origin.getZ());
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int ring = 0; ring <= SEARCH_RADIUS; ring++) {
            double ringDistance = SectionPos.sectionToBlockCoord(Math.max(0, (ring - 1) * spacing - 1));
            if (ringDistance * ringDistance > closestDistance) break;

            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.abs(x) != ring && Math.abs(z) != ring) continue;

                    ChunkPos chunkPos = spread.getPotentialStructureChunk(seed, originX + spacing * x, originZ + spacing * z);
                    if (level.structureManager().checkStructurePresence(chunkPos, structure.value(), spread, false) == StructureCheckResult.START_NOT_PRESENT) continue;

                    BlockPos anchor = findAnchorInStructure(level, structure, chunkPos, origin, known);
                    if (anchor == null) continue;

                    double distance = horizontalDistSqr(anchor, origin);
                    if (distance < closestDistance) {
                        closest = anchor;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }

    private static @Nullable BlockPos findAnchorInStructure(ServerLevel level, Holder<Structure> structure, ChunkPos chunkPos, BlockPos origin, Set<BlockPos> known) {
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), structure.value(), chunk);
        if (start == null || !start.isValid()) return null;

        StructureTemplateManager templates = level.getStructureManager();
        BlockPos closest = null;

        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece poolPiece) || !(poolPiece.getElement() instanceof SinglePoolElement element)) continue;

            StructureTemplate template = ((SinglePoolElementAccessor) element).whispering_anchors$getTemplate(templates);
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(poolPiece.getRotation());

            for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(poolPiece.getPosition(), settings, ModBlocks.WHISPERING_ANCHOR.get())) {
                BlockPos top = WhisperingAnchorBlock.getTopPos(info.pos(), info.state());
                if (known.contains(top) || level.isLoaded(top) && !level.getBlockState(top).is(ModBlocks.WHISPERING_ANCHOR)) continue;

                if (closest == null || horizontalDistSqr(top, origin) < horizontalDistSqr(closest, origin)) {
                    closest = top;
                }
            }
        }

        return closest;
    }

    private static AnchorAttunement attunementAt(ServerLevel level, BlockPos anchorPos) {
        return new AnchorAttunement(GlobalPos.of(level.dimension(), anchorPos), Optional.empty(), Optional.empty());
    }

    private static double horizontalDistSqr(BlockPos a, BlockPos b) {
        double x = a.getX() - b.getX();
        double z = a.getZ() - b.getZ();
        return x * x + z * z;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        KnownAnchors knownAnchors = stack.get(ModDataComponents.KNOWN_ANCHORS);
        if (knownAnchors == null || knownAnchors.anchors().isEmpty()) return;

        List<AnchorAttunement> nearest = knownAnchors.anchors().stream()
                .filter(anchor -> anchor.anchorPos().dimension().equals(player.level().dimension()))
                .sorted(Comparator.comparingDouble(anchor -> getWorldPosition(player.level(), anchor).distanceToSqr(player.position())))
                .limit(SHOWN_KNOWN_ANCHORS)
                .toList();
        if (nearest.isEmpty()) return;

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchors").withStyle(ChatFormatting.GRAY));

        for (AnchorAttunement anchor : nearest) {
            double distance = getWorldPosition(player.level(), anchor).distanceTo(player.position());
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor", anchor.getDisplayName(), formatDistance(distance).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY));
        }

        int hidden = knownAnchors.anchors().size() - nearest.size();
        if (hidden > 0) {
            lines.add(Component.translatable("tooltip.whispering_anchors.heeding_compass.more_known_anchors", hidden).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        tooltipComponents.addAll(1, lines);
    }

    private static Vec3 getWorldPosition(Level level, AnchorAttunement anchor) {
        return SubLevelHandler.INSTANCE.toWorldPosition(level, Vec3.atCenterOf(anchor.anchorPos().pos()));
    }

    private static MutableComponent formatDistance(double distance) {
        return distance < 1000 ? Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor.meters", Mth.floor(distance)) : Component.translatable("tooltip.whispering_anchors.heeding_compass.known_anchor.kilometers", String.format(Locale.ROOT, "%.1f", distance / 1000));
    }
}
