package net.ashpapi.interactentity.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CustomNpcModel extends GeoModel<CustomNpcEntity> {

    @Override
    public ResourceLocation getModelResource(CustomNpcEntity entity) {
        return entity.getModelLocation();
    }

    @Override
    public ResourceLocation getTextureResource(CustomNpcEntity entity) {
        return entity.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(CustomNpcEntity entity) {
        String modelId = entity.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            // Конвенция: анимация рядом с моделью, .geo.json -> .animation.json
            String animPath = modelId.replace(".geo.json", ".animation.json");
            return new ResourceLocation(animPath);
        }
        return new ResourceLocation("interactentity", "animations/custom_npc_default.animation.json");
    }
}
