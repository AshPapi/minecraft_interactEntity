package net.ashpapi.interactentity.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CustomNpcRenderer extends GeoEntityRenderer<CustomNpcEntity> {

    public CustomNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CustomNpcModel());
    }
}
