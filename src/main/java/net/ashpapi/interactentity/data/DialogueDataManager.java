package net.ashpapi.interactentity.data;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public class DialogueDataManager {
    private static final String GLOBAL_NAME = "interactentity_progress";
    private static final String PLAYER_NAME_PREFIX = "interactentity_player_";

    public static DialogueSavedData getGlobal(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                DialogueSavedData::load,
                DialogueSavedData::new,
                GLOBAL_NAME
        );
    }

    @Nullable
    public static DialogueSavedData getPlayer(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(
                DialogueSavedData::load,
                DialogueSavedData::new,
                PLAYER_NAME_PREFIX + player.getUUID()
        );
    }

    /** Returns the data store for the given scope. Falls back to global when scope is unknown or player data is unavailable. */
    public static DialogueSavedData get(Player player, @Nullable String scope) {
        if ("player".equalsIgnoreCase(scope)) {
            DialogueSavedData data = getPlayer(player);
            if (data != null) return data;
        }
        return getGlobal((ServerLevel) player.level());
    }

    /** Reads scope from a JSON params block (defaults to global). */
    public static DialogueSavedData get(Player player, @Nullable JsonObject params) {
        String scope = params != null && params.has("scope") ? params.get("scope").getAsString() : "global";
        return get(player, scope);
    }
}
