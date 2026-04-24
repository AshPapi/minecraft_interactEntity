package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.network.CameraShakePacket;
import net.ashpapi.interactentity.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class CameraShakeAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        float intensity = params.has("intensity") ? params.get("intensity").getAsFloat() : 1.0f;
        int duration = params.has("duration") ? params.get("duration").getAsInt() : 20;
        ModNetwork.sendToPlayer(player, new CameraShakePacket(intensity, duration));
    }
}
