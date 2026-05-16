package net.ashpapi.interactentity.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Public API for KubeJS scripts and other mods to drive InteractEntity.
 * All methods are server-side; pass a ServerPlayer (the player who's the subject of the action).
 */
public final class InteractEntityAPI {
    private InteractEntityAPI() {}

    /** Starts a quest from a JSON definition string. See StartQuestAction for the expected schema. */
    public static boolean startQuest(ServerPlayer player, String questJsonString) {
        try {
            JsonObject obj = JsonParser.parseString(questJsonString).getAsJsonObject();
            return startQuest(player, obj);
        } catch (Exception e) {
            return false;
        }
    }

    /** Starts a quest from a parsed JsonObject. Returns false if the quest is already active or completed. */
    public static boolean startQuest(ServerPlayer player, JsonObject questJson) {
        if (!questJson.has("id")) return false;
        String id = questJson.get("id").getAsString();
        String scope = questJson.has("scope") ? questJson.get("scope").getAsString() : "global";

        DialogueSavedData data = DialogueDataManager.get(player, scope);
        QuestState existing = data.getQuest(id);
        if (existing != null && ("active".equals(existing.getStatus()) || "completed".equals(existing.getStatus()))) {
            return false;
        }

        String title = questJson.has("title") ? questJson.get("title").getAsString() : id;
        String description = questJson.has("description") ? questJson.get("description").getAsString() : "";
        List<String> objectives = new ArrayList<>();
        if (questJson.has("objectives")) {
            questJson.getAsJsonArray("objectives").forEach(el -> objectives.add(el.getAsString()));
        }
        String giverName = questJson.has("giver") ? questJson.get("giver").getAsString() : "";

        QuestState quest = new QuestState(id, title, description, objectives, "active", giverName);
        data.setQuest(quest);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new QuestStartEvent(player, id, scope));
        ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));
        return true;
    }

    /** Marks a quest as completed and broadcasts the update. */
    public static boolean completeQuest(ServerPlayer player, String questId) {
        // Find in either scope.
        DialogueSavedData global = DialogueDataManager.getGlobal(player.serverLevel());
        DialogueSavedData personal = DialogueDataManager.getPlayer(player);
        DialogueSavedData data = null;
        String scope = "global";
        if (global.getQuest(questId) != null) { data = global; scope = "global"; }
        else if (personal != null && personal.getQuest(questId) != null) { data = personal; scope = "player"; }
        if (data == null) return false;

        QuestState quest = data.getQuest(questId);
        quest.setStatus("completed");
        quest.completeAllObjectives();
        data.untrackQuest(questId);
        data.setDirty();
        ModNetwork.sendToAll(new QuestUpdatePacket(quest));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new QuestCompleteEvent(player, questId, scope));
        return true;
    }

    /** Marks a quest as failed. */
    public static boolean failQuest(ServerPlayer player, String questId) {
        DialogueSavedData global = DialogueDataManager.getGlobal(player.serverLevel());
        DialogueSavedData personal = DialogueDataManager.getPlayer(player);
        DialogueSavedData data = null;
        String scope = "global";
        if (global.getQuest(questId) != null) { data = global; scope = "global"; }
        else if (personal != null && personal.getQuest(questId) != null) { data = personal; scope = "player"; }
        if (data == null) return false;

        QuestState quest = data.getQuest(questId);
        quest.setStatus("failed");
        data.untrackQuest(questId);
        data.setDirty();
        ModNetwork.sendToAll(new QuestUpdatePacket(quest));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new QuestFailEvent(player, questId, scope));
        return true;
    }

    /** Adjusts reputation in the given scope ("global" or "player"). */
    public static void addReputation(ServerPlayer player, String factionId, int delta, String scope) {
        DialogueSavedData data = DialogueDataManager.get(player, scope);
        data.addReputation(factionId, delta);
        ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));
    }

    public static int getReputation(ServerPlayer player, String factionId, String scope) {
        return DialogueDataManager.get(player, scope).getReputation(factionId);
    }

    public static void setVar(ServerPlayer player, String name, String value, String scope) {
        DialogueDataManager.get(player, scope).setVar(name, value);
        ModNetwork.sendToPlayer(player, SyncProgressPacket.createFor(player));
    }

    public static String getVar(ServerPlayer player, String name, String scope) {
        return DialogueDataManager.get(player, scope).getVar(name);
    }

    /** Opens a dialogue with the given id. Looks up the bound entity via DialogueManager. */
    public static boolean openDialogue(ServerPlayer player, String dialogueId, @Nullable LivingEntity entity) {
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) return false;
        DialogueTree tree = mgr.getDialogueById(dialogueId);
        if (tree == null) return false;
        if (entity == null) return false;
        DialogueSession.startSession(player, entity, tree);
        return true;
    }
}
