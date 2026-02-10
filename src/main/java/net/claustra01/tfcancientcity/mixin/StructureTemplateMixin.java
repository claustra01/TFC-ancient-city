package net.claustra01.tfcancientcity.mixin;

import java.util.List;
import javax.annotation.Nullable;
import net.claustra01.tfcancientcity.TfcBlockReplacementProcessor;
import net.claustra01.tfcancientcity.mixin.accessor.StructureTemplateIdAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin implements StructureTemplateIdAccessor {
    @Unique
    @Nullable
    private ResourceLocation tfcancientcity$templateId;

    @Override
    @Nullable
    public ResourceLocation tfcancientcity$getTemplateId() {
        return this.tfcancientcity$templateId;
    }

    @Override
    public void tfcancientcity$setTemplateId(ResourceLocation id) {
        this.tfcancientcity$templateId = id;
    }

    // NeoForge runtime uses official names; we don't generate a refmap, so disable remapping.
    @Inject(method = "placeInWorld", at = @At("HEAD"), remap = false)
    private void tfcancientcity$injectTfcBlockReplacementProcessor(
        ServerLevelAccessor level,
        BlockPos offset,
        BlockPos pos,
        StructurePlaceSettings settings,
        RandomSource random,
        int flags,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ResourceLocation id = this.tfcancientcity$templateId;
        if (id == null || !"minecraft".equals(id.getNamespace()) || !id.getPath().startsWith("ancient_city/")) {
            return;
        }

        List<StructureProcessor> processors = settings.getProcessors();
        if (processors.contains(TfcBlockReplacementProcessor.INSTANCE)) {
            return;
        }

        // Run after vanilla processors to avoid breaking their block predicates.
        settings.addProcessor(TfcBlockReplacementProcessor.INSTANCE);
    }
}
