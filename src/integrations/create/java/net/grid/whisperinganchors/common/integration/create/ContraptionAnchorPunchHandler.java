package net.grid.whisperinganchors.common.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.MutablePair;

import java.lang.ref.WeakReference;
import java.util.Optional;

public class ContraptionAnchorPunchHandler {

    private static boolean punchedThisClick;
    public static void punchAnchor(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || minecraft.level == null) return;

        Vec3 origin = player.getEyePosition();
        double reach = player.entityInteractionRange();
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS) {
            reach = Math.min(reach, minecraft.hitResult.getLocation().distanceTo(origin));
        }
        Vec3 target = origin.add(player.getViewVector(1.0F).scale(reach));

        AbstractContraptionEntity bestContraption = null;
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (WeakReference<AbstractContraptionEntity> reference : ContraptionHandler.loadedContraptions.get(minecraft.level).values()) {
            AbstractContraptionEntity contraption = reference.get();
            if (contraption == null) continue;

            Vec3 localOrigin = contraption.toLocalVector(origin, 1.0F);
            Vec3 localTarget = contraption.toLocalVector(target, 1.0F);

            for (MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor : contraption.getContraption().getActors()) {
                if (actor.right == null || !(actor.right.temporaryData instanceof ContraptionAnchor anchor)) continue;

                Vec3 offset = Vec3.atLowerCornerOf(actor.left.pos());
                Vec3 start = localOrigin.subtract(offset);
                Optional<Vec3> hit = anchor.getSwing().getSwingBody().clip(start, localTarget.subtract(offset));
                if (hit.isEmpty()) continue;

                double distance = hit.get().distanceToSqr(start);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestContraption = contraption;
                    bestPos = actor.left.pos();
                }
            }
        }

        if (bestContraption == null) return;

        event.setCanceled(true);
        if (punchedThisClick) return;

        punchedThisClick = true;
        player.resetAttackStrengthTicker();
        PacketDistributor.sendToServer(new PunchContraptionAnchorPayload(bestContraption.getId(), bestPos));
    }

    public static void resetClick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().options.keyAttack.isDown()) {
            punchedThisClick = false;
        }
    }
}
