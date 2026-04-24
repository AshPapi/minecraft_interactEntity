package net.ashpapi.interactentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.ClientNpcRegistry;
import net.ashpapi.interactentity.data.ClientProgressData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MobIconRenderer {

    private static final double VISIBLE_RANGE_SQ = 256.0; // 16 блоков

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        if (Minecraft.getInstance().player == null) return;

        double distSq = Minecraft.getInstance().player.distanceToSqr(entity);
        if (distSq > VISIBLE_RANGE_SQ) return;

        ClientNpcRegistry.Entry npc = ClientNpcRegistry.get(entity.getId());
        if (npc == null) return;
        String dialogueId = npc.dialogueId;
        String entryNodeId = npc.entryNodeId;

        boolean visited = entryNodeId != null && ClientProgressData.hasVisited(dialogueId, entryNodeId);

        String icon;
        int color;
        boolean completed = ClientProgressData.isCompleted(dialogueId);
        boolean hasActiveQuest = ClientProgressData.getActiveQuests().stream()
                .anyMatch(q -> "active".equals(q.getStatus()));

        if (hasActiveQuest) {
            icon = "!";
            color = 0xFFFFDD00;
        } else if (!visited && !completed) {
            icon = "?";
            color = 0xFFFFDD00;
        } else {
            return;
        }

        renderIcon(event.getPoseStack(), event.getMultiBufferSource(), entity, icon, color);
    }

    private static void renderIcon(PoseStack poseStack, MultiBufferSource buffers,
                                   LivingEntity entity, String icon, int color) {
        Minecraft mc = Minecraft.getInstance();

        poseStack.pushPose();

        float yOffset = entity.getBbHeight() + 0.95f;
        poseStack.translate(0, yOffset, 0);
        poseStack.mulPose(mc.getEntityRenderDispatcher().camera.rotation());
        poseStack.scale(-0.04f, -0.04f, 0.04f);

        Component text = Component.literal(icon);
        int textWidth = mc.font.width(text);
        float x = -textWidth / 2f;
        Matrix4f matrix = poseStack.last().pose();

        // Кастомная плашка без «лишнего» правого столбца
        float bgR = 0f, bgG = 0f, bgB = 0f, bgA = 0.5f;
        float left   = x - 1f;
        float right  = x + textWidth;
        float top    = -1f;
        float bottom = 9f;
        VertexConsumer vc = buffers.getBuffer(RenderType.textBackgroundSeeThrough());
        vc.vertex(matrix, left,  bottom, 0f).color(bgR, bgG, bgB, bgA).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vc.vertex(matrix, right, bottom, 0f).color(bgR, bgG, bgB, bgA).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vc.vertex(matrix, right, top,    0f).color(bgR, bgG, bgB, bgA).uv2(LightTexture.FULL_BRIGHT).endVertex();
        vc.vertex(matrix, left,  top,    0f).color(bgR, bgG, bgB, bgA).uv2(LightTexture.FULL_BRIGHT).endVertex();

        mc.font.drawInBatch(text, x, 0, color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }
}