package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.event.NPCJoinHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetHomeAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        int radius = params.has("radius") ? params.get("radius").getAsInt() : 16;

        BlockPos pos;
        if (params.has("x") && params.has("y") && params.has("z")) {
            pos = new BlockPos(params.get("x").getAsInt(), params.get("y").getAsInt(), params.get("z").getAsInt());
        } else {
            pos = entity.blockPosition();
        }

        NPCJoinHandler.setHome(entity, pos, radius);
    }
}
