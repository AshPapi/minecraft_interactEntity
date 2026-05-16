package net.ashpapi.interactentity.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ashpapi.interactentity.render.MobIconRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CustomNpcRenderer extends GeoEntityRenderer<CustomNpcEntity> {

    public CustomNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CustomNpcModel());
    }

    @Override
    public void render(CustomNpcEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        MobIconRenderer.tryRender(entity, poseStack, bufferSource);
    }
}
