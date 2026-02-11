package net.claustra01.tfcancientcity.accessor;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public interface StructureTemplateIdAccessor {
    @Nullable
    ResourceLocation tfcancientcity$getTemplateId();

    void tfcancientcity$setTemplateId(ResourceLocation id);
}

