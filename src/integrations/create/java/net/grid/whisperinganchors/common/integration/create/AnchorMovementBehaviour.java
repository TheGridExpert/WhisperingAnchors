package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.grid.whisperinganchors.common.config.ModConfig;
import net.grid.whisperinganchors.common.core.ModBlocks;
import net.grid.whisperinganchors.common.world.block.AnchorPart;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.grid.whisperinganchors.common.world.entity.AnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AnchorMovementBehaviour implements MovementBehaviour {

    public static void register() {
        MovementBehaviour.REGISTRY.register(ModBlocks.WHISPERING_ANCHOR.get(), new AnchorMovementBehaviour());
        BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) -> state.getBlock() instanceof WhisperingAnchorBlock ? CheckResult.SUCCESS : CheckResult.PASS);
        BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> state.getBlock() instanceof WhisperingAnchorBlock ? CheckResult.of(isAttachedTowards(state, direction)) : CheckResult.PASS);
        BlockMovementChecks.registerBrittleCheck(state -> state.getBlock() instanceof WhisperingAnchorBlock ? CheckResult.SUCCESS : CheckResult.PASS);
    }

    private static boolean isAttachedTowards(BlockState state, Direction direction) {
        return direction == Direction.UP || direction == Direction.DOWN && state.getValue(WhisperingAnchorBlock.PART) != AnchorPart.BOTTOM;
    }

    private static boolean isSwinging(BlockState state) {
        return WhisperingAnchorBlock.isPart(state, AnchorPart.TOP) && !ModConfig.areAnchorsStatic();
    }

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public void startMoving(MovementContext context) {
        if (context.world.isClientSide() || !WhisperingAnchorBlock.isPart(context.state, AnchorPart.TOP)) return;

        BlockPos top = context.contraption.anchor.offset(context.localPos);
        AnchorEntity.findBoundTo(context.world, top).forEach(Entity::discard);
    }

    @Override
    public void tick(MovementContext context) {
        if (!isSwinging(context.state)) return;

        ContraptionAnchor.of(context).tick(context);
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context.world.isClientSide() || !isSwinging(context.state) || !(context.temporaryData instanceof ContraptionAnchor anchor)) return;

        anchor.transferSwing(context);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (!isSwinging(context.state) || !(context.temporaryData instanceof ContraptionAnchor anchor)) return;

        ContraptionAnchorRenderer.render(context, anchor, matrices, buffer);
    }
}
