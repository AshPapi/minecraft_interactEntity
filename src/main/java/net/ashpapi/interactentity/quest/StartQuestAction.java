package net.ashpapi.interactentity.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.action.DialogueAction;
import net.ashpapi.interactentity.data.DialogueDataManager;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class StartQuestAction implements DialogueAction {

    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        JsonObject questJson = params.getAsJsonObject("quest");
        String id = questJson.get("id").getAsString();

        DialogueSavedData data = DialogueDataManager.get(player, params);
        QuestState existing = data.getQuest(id);

        // Исправлено: не запускаем, если квест уже active ИЛИ completed
        if (existing != null && ("active".equals(existing.getStatus()) || "completed".equals(existing.getStatus()))) {
            InteractEntityMod.LOGGER.debug("Quest {} already {} for this world, skipping start", id, existing.getStatus());
            return;
        }

        String title = questJson.get("title").getAsString();
        String description = questJson.has("description") ? questJson.get("description").getAsString() : "";

        List<String> objectives = new ArrayList<>();
        if (questJson.has("objectives")) {
            JsonArray arr = questJson.getAsJsonArray("objectives");
            for (int i = 0; i < arr.size(); i++) {
                objectives.add(arr.get(i).getAsString());
            }
        }

        String giverName = entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getType().getDescription().getString();
        DialogueSession session = DialogueSession.getSession(player);
        if (session != null) {
            giverName = session.getDisplayName();
        }

        QuestState quest = new QuestState(id, title, description, objectives, "active", giverName);
        if (session != null) {
            quest.setDialogueId(session.getDialogueId());
        }

        // Добавление required_item (если есть в JSON)
        if (questJson.has("required_item")) {
            JsonObject reqItem = questJson.getAsJsonObject("required_item");
            String itemId = reqItem.get("id").getAsString();
            int count = reqItem.has("count") ? reqItem.get("count").getAsInt() : 1;
            quest.setRequiredItem(itemId, count);
        }

        // Добавление required_kills (если есть в JSON)
        if (questJson.has("required_kills")) {
            JsonObject rk = questJson.getAsJsonObject("required_kills");
            String entityType = rk.get("entity").getAsString();
            if (rk.has("tag")) {
                entityType = entityType + "#" + rk.get("tag").getAsString();
            }
            int count = rk.get("count").getAsInt();
            int objIndex = rk.has("objective") ? rk.get("objective").getAsInt() : 0;
            quest.setRequiredKills(entityType, count, objIndex);
        }

        // Добавление deadline (если есть в JSON)
        if (questJson.has("deadline")) {
            JsonObject deadlineJson = questJson.getAsJsonObject("deadline");
            String deadlineType = deadlineJson.get("type").getAsString();
            long currentGameTime = player.serverLevel().getGameTime();
            long deadlineTick = 0;

            switch (deadlineType) {
                case "ticks" -> deadlineTick = currentGameTime + deadlineJson.get("value").getAsLong();
                case "game_days" -> deadlineTick = currentGameTime + deadlineJson.get("value").getAsLong() * 24000L;
                case "sunset" -> {
                    long dayTime = player.serverLevel().getDayTime() % 24000L;
                    long ticksUntilSunset = (dayTime <= 12000) ? (12000 - dayTime) : (24000 - dayTime + 12000);
                    deadlineTick = currentGameTime + ticksUntilSunset;
                }
                case "sunrise" -> {
                    long dayTime = player.serverLevel().getDayTime() % 24000L;
                    long ticksUntilSunrise = (dayTime == 0) ? 24000 : (24000 - dayTime);
                    deadlineTick = currentGameTime + ticksUntilSunrise;
                }
            }

            if (deadlineTick > 0) {
                quest.setDeadlineTick(deadlineTick);
                quest.setDeadlineType(deadlineType);
            }
        }

        data.setQuest(quest);

        // === НОВОЕ: проверка, есть ли уже нужное количество предметов ===
        if (quest.getRequiredItemId() != null) {
            int totalInInventory = countItemInInventory(player, quest.getRequiredItemId());
            if (totalInInventory >= quest.getRequiredCount()) {
                quest.markItemCollected();

                quest.completeObjective(0);

                data.setDirty();
                InteractEntityMod.LOGGER.debug("Quest {} item objective auto-completed on start (player already had items)", id);
            }
        }

        // Sync to all players (each gets a personalized merged view).
        for (ServerPlayer p : player.serverLevel().getServer().getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(p, SyncProgressPacket.createFor(p));
        }
    }

    // Вспомогательный метод (дубликат из QuestEventHandler, чтобы не плодить зависимости)
    private static int countItemInInventory(ServerPlayer player, String itemId) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                if (id.equals(itemId)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }
}
