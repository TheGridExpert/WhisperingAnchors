package net.grid.whisperinganchors.misc.mixin.accessor;

import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EndDragonFight.class)
public interface EndDragonFightAccessor {

    @Accessor("dragonKilled")
    boolean whispering_anchors$isDragonKilled();
}
