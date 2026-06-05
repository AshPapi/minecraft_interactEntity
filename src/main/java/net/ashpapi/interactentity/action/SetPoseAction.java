package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.ashpapi.interactentity.entity.CustomNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class SetPoseAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        if (entity == null) return;

        // Apply to NBT (works for all entities)
        if (params.has("pose")) {
            String pose = params.get("pose").getAsString();
            entity.getPersistentData().putString("InteractEntity_Pose", pose);
            
            // Also apply to CustomNpcEntity if applicable
            if (entity instanceof CustomNpcEntity customNpc) {
                customNpc.setCustomPose(pose);
            }
        }

        if (params.has("is_moving")) {
            boolean isMoving = params.get("is_moving").getAsBoolean();
            entity.getPersistentData().putBoolean("InteractEntity_IsMoving", isMoving);
            
            // Also apply to CustomNpcEntity if applicable
            if (entity instanceof CustomNpcEntity customNpc) {
                customNpc.setMovementEnabled(isMoving);
            }
        }
    }
}
