package net.grid.whisperinganchors.common.world.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class RadialFadeProcessor extends StructureProcessor {

    public static final MapCodec<RadialFadeProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RuleTest.CODEC.fieldOf("input_predicate").forGetter(processor -> processor.inputPredicate),
            Codec.INT.fieldOf("center_x").forGetter(processor -> processor.centerX),
            Codec.INT.fieldOf("center_z").forGetter(processor -> processor.centerZ),
            Codec.floatRange(0.0F, 512.0F).fieldOf("inner_radius").forGetter(processor -> processor.innerRadius),
            Codec.floatRange(0.0F, 512.0F).fieldOf("outer_radius").forGetter(processor -> processor.outerRadius)
    ).apply(instance, RadialFadeProcessor::new));

    private final RuleTest inputPredicate;
    private final int centerX;
    private final int centerZ;
    private final float innerRadius;
    private final float outerRadius;

    public RadialFadeProcessor(RuleTest inputPredicate, int centerX, int centerZ, float innerRadius, float outerRadius) {
        this.inputPredicate = inputPredicate;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo blockInfo, StructureTemplate.StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings) {
        RandomSource random = settings.getRandom(relativeBlockInfo.pos());
        if (!this.inputPredicate.test(relativeBlockInfo.state(), random)) {
            return relativeBlockInfo;
        }

        return random.nextFloat() < this.survivalChance(blockInfo.pos()) ? relativeBlockInfo : null;
    }

    private float survivalChance(BlockPos localPos) {
        if (this.outerRadius <= this.innerRadius) return 1.0F;

        float dx = localPos.getX() - this.centerX;
        float dz = localPos.getZ() - this.centerZ;
        float distance = Mth.sqrt(dx * dx + dz * dz);

        return 1.0F - Mth.clamp((distance - this.innerRadius) / (this.outerRadius - this.innerRadius), 0.0F, 1.0F);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessors.RADIAL_FADE.get();
    }
}
