package net.grid.whisperinganchors.common.world.structure.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.grid.whisperinganchors.common.core.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;

public class WeatheringProcessor extends StructureProcessor {

    public static final MapCodec<WeatheringProcessor> CODEC = Rule.CODEC.listOf()
            .fieldOf("rules")
            .xmap(WeatheringProcessor::new, processor -> processor.rules);

    private final List<Rule> rules;

    public WeatheringProcessor(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    @SuppressWarnings("deprecation")
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo blockInfo, StructureTemplate.StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings) {
        BlockState state = relativeBlockInfo.state();

        for (Rule rule : this.rules) {
            if (!state.is(rule.input())) continue;

            RandomSource random = settings.getRandom(relativeBlockInfo.pos());
            if (random.nextFloat() >= rule.chance()) return relativeBlockInfo;

            return new StructureTemplate.StructureBlockInfo(relativeBlockInfo.pos(), rule.output().withPropertiesOf(state), relativeBlockInfo.nbt());
        }

        return relativeBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessors.WEATHERING.get();
    }

    public record Rule(Block input, Block output, float chance) {

        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("input").forGetter(Rule::input),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("output").forGetter(Rule::output),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Rule::chance)
        ).apply(instance, Rule::new));
    }
}
