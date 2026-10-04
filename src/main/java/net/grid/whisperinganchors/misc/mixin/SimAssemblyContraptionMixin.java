package net.grid.whisperinganchors.misc.mixin;

import net.grid.whisperinganchors.api.AnchorFeatures;
import net.grid.whisperinganchors.common.world.block.WhisperingAnchorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.util.assembly.SimAssemblyContraption", remap = false)
public class SimAssemblyContraptionMixin {

    @Inject(method = "movementAllowed", at = @At("HEAD"), cancellable = true, require = 0)
    private void whispering_anchors$keepAnchorsInPlace(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof WhisperingAnchorBlock && !AnchorFeatures.areAnchorsStealable()) {
            cir.setReturnValue(false);
        }
    }
}
