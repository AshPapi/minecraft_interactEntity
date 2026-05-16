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
        if (isPlayerScope(scope)) {
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

    /** Accept both "player" and "per_player" (and a few common variants); anything else = global. */
    public static boolean isPlayerScope(@Nullable String scope) {
        if (scope == null) return false;
        String s = scope.trim().toLowerCase(java.util.Locale.ROOT);
        return s.equals("player") || s.equals("per_player") || s.equals("perplayer") || s.equals("per-player");
    }

    /** Find which store actually owns a quest. Use for cross-dialogue ops where the
     *  caller's scope may differ from where the quest was originally stored.
     *  Checks player first (if requested), then global. Returns the action's scope store as fallback. */
    public static DialogueSavedData findQuestStore(Player player, @Nullable JsonObject params, String questId) {
        DialogueSavedData primary = get(player, params);
        if (primary.getQuest(questId) != null) return primary;

        DialogueSavedData playerData = getPlayer(player);
        if (playerData != null && playerData != primary && playerData.getQuest(questId) != null) return playerData;

        DialogueSavedData global = getGlobal((ServerLevel) player.level());
        if (global != primary && global.getQuest(questId) != null) return global;

        return primary; // not found anywhere — caller will warn
    }
}
