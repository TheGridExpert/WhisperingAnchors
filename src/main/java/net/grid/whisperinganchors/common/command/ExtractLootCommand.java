package net.grid.whisperinganchors.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.grid.whisperinganchors.misc.mixin.accessor.BrushableBlockEntityAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ExtractLootCommand {

    private static final int MAX_VOLUME = 32768;
    private static final int SINGLE_CHEST_SIZE = 27;

    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((max, specified) -> Component.translatable("command.whispering_anchors.extract_loot.too_large", max, specified));
    private static final SimpleCommandExceptionType ERROR_NO_LOOT = new SimpleCommandExceptionType(Component.translatable("command.whispering_anchors.extract_loot.no_loot"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whisperinganchors")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("extractloot")
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .executes(context -> extractLoot(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to")))))));
    }

    private static int extractLoot(CommandSourceStack source, BlockPos from, BlockPos to) throws CommandSyntaxException {
        BoundingBox box = BoundingBox.fromCorners(from, to);
        int volume = box.getXSpan() * box.getYSpan() * box.getZSpan();
        if (volume > MAX_VOLUME) {
            throw ERROR_AREA_TOO_LARGE.create(MAX_VOLUME, volume);
        }

        ServerLevel level = source.getLevel();
        List<ItemStack> loot = new ArrayList<>();
        int blocks = 0;

        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (!(level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable)) continue;

            List<ItemStack> extracted = extract(level, brushable, source.getEntity());
            if (extracted.isEmpty()) continue;

            extracted.forEach(stack -> merge(loot, stack));
            blocks++;
        }

        if (loot.isEmpty()) {
            throw ERROR_NO_LOOT.create();
        }

        BlockPos chestPos = BlockPos.containing(source.getPosition());
        Direction facing = source.getEntity() != null ? source.getEntity().getDirection().getOpposite() : Direction.NORTH;
        placeChests(level, chestPos, facing, loot);

        int items = loot.stream().mapToInt(ItemStack::getCount).sum();
        int extractedBlocks = blocks;
        source.sendSuccess(() -> Component.translatable("command.whispering_anchors.extract_loot.success", items, extractedBlocks), true);

        return extractedBlocks;
    }

    private static List<ItemStack> extract(ServerLevel level, BrushableBlockEntity brushable, @Nullable Entity entity) {
        BrushableBlockEntityAccessor accessor = (BrushableBlockEntityAccessor) brushable;
        List<ItemStack> extracted = new ArrayList<>();

        if (!brushable.getItem().isEmpty()) {
            extracted.add(brushable.getItem().copy());
        }

        ResourceKey<LootTable> lootTable = accessor.whispering_anchors$getLootTable();
        if (lootTable != null) {
            LootParams.Builder params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(brushable.getBlockPos())).withOptionalParameter(LootContextParams.THIS_ENTITY, entity);
            extracted.addAll(level.getServer().reloadableRegistries().getLootTable(lootTable).getRandomItems(params.create(LootContextParamSets.CHEST), accessor.whispering_anchors$getLootTableSeed()));
        }

        //noinspection ConstantConditions
        brushable.setLootTable(null, 0L);
        accessor.whispering_anchors$setItem(ItemStack.EMPTY);
        brushable.setChanged();

        return extracted;
    }

    private static void merge(List<ItemStack> loot, ItemStack stack) {
        for (ItemStack existing : loot) {
            if (stack.isEmpty()) return;
            if (!ItemStack.isSameItemSameComponents(existing, stack) || existing.getCount() >= existing.getMaxStackSize()) continue;

            int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            stack.shrink(moved);
        }

        if (!stack.isEmpty()) {
            loot.add(stack);
        }
    }

    private static void placeChests(ServerLevel level, BlockPos origin, Direction facing, List<ItemStack> loot) {
        int index = 0;

        for (int layer = 0; index < loot.size(); layer++) {
            BlockPos pos = origin.above(layer);
            boolean isDouble = loot.size() - index > SINGLE_CHEST_SIZE;
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);

            if (isDouble) {
                level.setBlockAndUpdate(pos, chest.setValue(ChestBlock.TYPE, ChestType.LEFT));
                level.setBlockAndUpdate(pos.relative(facing.getClockWise()), chest.setValue(ChestBlock.TYPE, ChestType.RIGHT));
                index = fill(level, pos, loot, index);
                index = fill(level, pos.relative(facing.getClockWise()), loot, index);
            } else {
                level.setBlockAndUpdate(pos, chest);
                index = fill(level, pos, loot, index);
            }
        }
    }

    private static int fill(ServerLevel level, BlockPos pos, List<ItemStack> loot, int index) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return index;

        for (int slot = 0; slot < chest.getContainerSize() && index < loot.size(); slot++) {
            chest.setItem(slot, loot.get(index++));
        }

        return index;
    }
}
