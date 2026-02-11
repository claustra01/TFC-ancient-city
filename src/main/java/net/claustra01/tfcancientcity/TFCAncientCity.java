package net.claustra01.tfcancientcity;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TFCAncientCity.MODID)
public class TFCAncientCity {
    public static final String MODID = "tfcancientcity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MODID);

    public static final DeferredItem<Item> DEEPSLATE_LOOSE_ROCK_ITEM = ITEMS.register("rock/loose/deepslate",
        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEEPSLATE_BRICK_ITEM = ITEMS.register("brick/deepslate",
        () -> new Item(new Item.Properties()));

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<TfcBlockReplacementProcessor>> TFC_BLOCK_REPLACEMENT =
        PROCESSORS.register("tfc_block_replacement", () -> () -> TfcBlockReplacementProcessor.CODEC);

    public TFCAncientCity(IEventBus modEventBus) {
        modEventBus.addListener(this::onBuildCreativeTabContents);
        ITEMS.register(modEventBus);
        PROCESSORS.register(modEventBus);
    }

    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.INGREDIENTS.equals(event.getTabKey())) {
            event.accept(DEEPSLATE_LOOSE_ROCK_ITEM.get());
            event.accept(DEEPSLATE_BRICK_ITEM.get());
        } else if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            event.accept(DEEPSLATE_BRICK_ITEM.get());
        } else if (CreativeModeTabs.NATURAL_BLOCKS.equals(event.getTabKey())) {
            event.accept(DEEPSLATE_LOOSE_ROCK_ITEM.get());
        }
    }
}
