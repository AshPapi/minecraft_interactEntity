package net.ashpapi.interactentity.render;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.ClientNpcRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NpcNameTagHandler {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        ClientNpcRegistry.Entry npc = ClientNpcRegistry.get(event.getEntity().getId());
        if (npc != null && hidesName(event.getEntity(), npc)) {
            event.setResult(Event.Result.DENY);
        }
    }

    static boolean hidesName(Entity entity, ClientNpcRegistry.Entry npc) {
        return switch (npc.nameTag) {
            case "hidden" -> true;
            case "hover" -> {
                Minecraft minecraft = Minecraft.getInstance();
                yield minecraft.crosshairPickEntity != entity
                        || !(minecraft.getCameraEntity() instanceof LivingEntity viewer)
                        || !viewer.hasLineOfSight(entity);
            }
            default -> false;
        };
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNpcRegistry.clear();
    }
}
