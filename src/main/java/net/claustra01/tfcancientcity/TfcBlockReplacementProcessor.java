package net.claustra01.tfcancientcity;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class TfcBlockReplacementProcessor extends StructureProcessor {
    public static final TfcBlockReplacementProcessor INSTANCE = new TfcBlockReplacementProcessor();
    public static final MapCodec<TfcBlockReplacementProcessor> CODEC = MapCodec.unit(INSTANCE);

    private record Replacement(ResourceLocation to, boolean clearNbt) {}

    private static final String NS_TFC = "tfc";
    private static final ResourceLocation TFC_FIREPIT = tfc("firepit");

    private TfcBlockReplacementProcessor() {}

    // Intentionally small and specific: only replace blocks that are present in vanilla Ancient City structure templates
    // and have clear 1:1 TFC equivalents. Everything else stays vanilla.
    private static final Map<Block, Replacement> REPLACEMENTS = Map.ofEntries(
        Map.entry(Blocks.DARK_OAK_LOG, new Replacement(tfc("wood/log/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_PLANKS, new Replacement(tfc("wood/planks/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_SLAB, new Replacement(tfc("wood/planks/blackwood_slab"), false)),
        Map.entry(Blocks.DARK_OAK_STAIRS, new Replacement(tfc("wood/planks/blackwood_stairs"), false)),
        Map.entry(Blocks.DARK_OAK_FENCE, new Replacement(tfc("wood/fence/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_FENCE_GATE, new Replacement(tfc("wood/fence_gate/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_DOOR, new Replacement(tfc("wood/door/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_TRAPDOOR, new Replacement(tfc("wood/trapdoor/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_BUTTON, new Replacement(tfc("wood/button/blackwood"), false)),
        Map.entry(Blocks.DARK_OAK_PRESSURE_PLATE, new Replacement(tfc("wood/pressure_plate/blackwood"), false)),

        Map.entry(Blocks.CHEST, new Replacement(tfc("wood/chest/blackwood"), false)),
        Map.entry(Blocks.TRAPPED_CHEST, new Replacement(tfc("wood/trapped_chest/blackwood"), false)),
        Map.entry(Blocks.LECTERN, new Replacement(tfc("wood/lectern/blackwood"), false)),

        Map.entry(Blocks.CANDLE, new Replacement(tfc("candle"), false)),
        Map.entry(Blocks.WHITE_CANDLE, new Replacement(tfc("candle/white"), false)),
        Map.entry(Blocks.TORCH, new Replacement(tfc("torch"), false)),
        Map.entry(Blocks.WALL_TORCH, new Replacement(tfc("wall_torch"), false)),

        // Vanilla camp structures include furnaces/campfires. TFC uses a firepit instead.
        // We clear block entity NBT because furnace/campfire tags don't make sense on a firepit.
        Map.entry(Blocks.FURNACE, new Replacement(TFC_FIREPIT, true)),
        Map.entry(Blocks.CAMPFIRE, new Replacement(TFC_FIREPIT, true)),
        Map.entry(Blocks.SOUL_CAMPFIRE, new Replacement(TFC_FIREPIT, true)),
        Map.entry(Blocks.SMOKER, new Replacement(TFC_FIREPIT, true)),
        Map.entry(Blocks.BLAST_FURNACE, new Replacement(TFC_FIREPIT, true)),

        Map.entry(Blocks.IRON_TRAPDOOR, new Replacement(tfc("metal/trapdoor/wrought_iron"), false))
    );

    private static final Map<ResourceLocation, Optional<Block>> OUT_BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> MISSING_OUT_BLOCKS = ConcurrentHashMap.newKeySet();

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
        ServerLevelAccessor level,
        BlockPos offset,
        BlockPos pos,
        List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
        List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
        StructurePlaceSettings settings
    ) {
        if (processedBlockInfos.isEmpty()) {
            return processedBlockInfos;
        }

        List<StructureTemplate.StructureBlockInfo> out = new ArrayList<>(processedBlockInfos.size());
        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            BlockState in = info.state();
            Replacement replacement = REPLACEMENTS.get(in.getBlock());
            if (replacement == null) {
                out.add(info);
                continue;
            }

            Optional<Block> maybeOutBlock = OUT_BLOCK_CACHE.computeIfAbsent(replacement.to(), BuiltInRegistries.BLOCK::getOptional);
            if (maybeOutBlock.isEmpty()) {
                if (MISSING_OUT_BLOCKS.add(replacement.to())) {
                    TFCAncientCity.LOGGER.warn(
                        "Missing target block '{}'; leaving vanilla block '{}'",
                        replacement.to(),
                        BuiltInRegistries.BLOCK.getKey(in.getBlock())
                    );
                }
                out.add(info);
                continue;
            }

            BlockState replaced = copyPropertiesByName(in, maybeOutBlock.get().defaultBlockState());
            if (TFC_FIREPIT.equals(replacement.to())) {
                replaced = applyFirepitAxisFromFacing(in, replaced);
            }
            out.add(new StructureTemplate.StructureBlockInfo(info.pos(), replaced, replacement.clearNbt() ? null : info.nbt()));
        }

        return out;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TFCAncientCity.TFC_BLOCK_REPLACEMENT.get();
    }

    private static ResourceLocation tfc(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS_TFC, path);
    }

    private static BlockState applyFirepitAxisFromFacing(BlockState from, BlockState firepit) {
        if (!from.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return firepit;
        }

        Direction facing = from.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction.Axis axis = facing.getAxis();
        if (axis != Direction.Axis.X && axis != Direction.Axis.Z) {
            return firepit;
        }

        if (firepit.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            return firepit.setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
        }
        if (firepit.hasProperty(BlockStateProperties.AXIS)) {
            return firepit.setValue(BlockStateProperties.AXIS, axis);
        }
        return firepit;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyPropertiesByName(BlockState from, BlockState to) {
        StateDefinition<Block, BlockState> def = to.getBlock().getStateDefinition();
        for (Property<?> fromProp : from.getProperties()) {
            Property<?> toProp = def.getProperty(fromProp.getName());
            if (toProp == null) {
                continue;
            }

            Comparable value = from.getValue((Property) fromProp);
            if (!((Property) toProp).getPossibleValues().contains(value)) {
                continue;
            }

            try {
                to = to.setValue((Property) toProp, value);
            } catch (Exception ignored) {
                // Defensive: if a property value can't be applied, just skip it.
            }
        }
        return to;
    }
}
