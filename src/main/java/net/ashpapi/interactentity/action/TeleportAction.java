package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class TeleportAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        double x = params.has("x") ? params.get("x").getAsDouble() : player.getX();
        double y = params.has("y") ? params.get("y").getAsDouble() : player.getY();
        double z = params.has("z") ? params.get("z").getAsDouble() : player.getZ();
        float yaw = params.has("yaw") ? params.get("yaw").getAsFloat() : player.getYRot();
        float pitch = params.has("pitch") ? params.get("pitch").getAsFloat() : player.getXRot();
        if ("relative".equals(params.has("mode") ? params.get("mode").getAsString() : "absolute")) {
            x += player.getX(); y += player.getY(); z += player.getZ();
        }
        player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch);
    }
}
