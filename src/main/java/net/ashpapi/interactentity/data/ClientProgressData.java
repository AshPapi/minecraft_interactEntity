package net.ashpapi.interactentity.data;

import net.ashpapi.interactentity.history.DialogueHistoryEntry;
import net.ashpapi.interactentity.quest.QuestState;

import java.util.*;

public class ClientProgressData {

    private static final Map<String, Set<String>> visitedNodes = new HashMap<>();
    private static final List<DialogueHistoryEntry> history = new ArrayList<>();
    private static final Map<String, QuestState> quests = new HashMap<>();
    private static final Set<String> completedDialogues = new HashSet<>();
    private static final Set<String> npcNotifications = new HashSet<>();
    private static final Map<String, Integer> reputation = new HashMap<>();
    private static final LinkedHashSet<String> trackedQuests = new LinkedHashSet<>();

    public static void setCompletedDialogues(Set<String> ids) {
        completedDialogues.clear();
        completedDialogues.addAll(ids);
    }
    public static boolean isCompleted(String dialogueId) {
        return completedDialogues.contains(dialogueId);
    }

    // ==================== Visited nodes ====================

    public static void setVisitedNodes(Map<String, Set<String>> nodes) {
        visitedNodes.clear();
        visitedNodes.putAll(nodes);
    }

    public static boolean hasVisited(String dialogueId, String nodeId) {
        Set<String> nodes = visitedNodes.get(dialogueId);
        return nodes != null && nodes.contains(nodeId);
    }

    public static boolean hasStartedDialogue(String dialogueId) {
        return visitedNodes.containsKey(dialogueId);
    }

    // ==================== History ====================

    public static void setHistory(List<DialogueHistoryEntry> entries) {
        history.clear();
        history.addAll(entries);
    }

    public static List<DialogueHistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    // ==================== Quests ====================

    public static void setQuests(Map<String, QuestState> questMap) {
        quests.clear();
        quests.putAll(questMap);
        pruneTrackedQuests();
    }

    public static void updateQuest(QuestState quest) {
        quests.put(quest.getId(), quest);
        pruneTrackedQuests();
    }

    public static Map<String, QuestState> getAllQuests() {
        return Collections.unmodifiableMap(quests);
    }

    /** Новый метод — нужен для иконки "?" над головой */
    public static Set<String> getAllQuestIds() {
        return quests.keySet();
    }

    public static List<QuestState> getActiveQuests() {
        return quests.values().stream()
                .filter(q -> "active".equals(q.getStatus()))
                .toList();
    }

    public static void setTrackedQuests(Collection<String> questIds) {
        trackedQuests.clear();
        for (String questId : questIds) {
            QuestState quest = quests.get(questId);
            if (quest != null && "active".equals(quest.getStatus()) && trackedQuests.size() < 3) {
                trackedQuests.add(questId);
            }
        }
    }

    public static Set<String> getTrackedQuestIds() {
        return Collections.unmodifiableSet(trackedQuests);
    }

    public static boolean isQuestTracked(String questId) {
        return trackedQuests.contains(questId);
    }

    public static boolean toggleTrackedQuest(String questId) {
        if (trackedQuests.remove(questId)) {
            return true;
        }

        QuestState quest = quests.get(questId);
        if (quest == null || !"active".equals(quest.getStatus()) || trackedQuests.size() >= 3) {
            return false;
        }

        trackedQuests.add(questId);
        return true;
    }

    public static List<QuestState> getTrackedActiveQuests() {
        List<QuestState> result = new ArrayList<>();
        for (String questId : trackedQuests) {
            QuestState quest = quests.get(questId);
            if (quest != null && "active".equals(quest.getStatus())) {
                result.add(quest);
            }
        }
        return result;
    }

    private static void pruneTrackedQuests() {
        trackedQuests.removeIf(questId -> {
            QuestState quest = quests.get(questId);
            return quest == null || !"active".equals(quest.getStatus());
        });
        while (trackedQuests.size() > 3) {
            Iterator<String> it = trackedQuests.iterator();
            if (!it.hasNext()) break;
            it.next();
            it.remove();
        }
    }

    public static String getQuestStatus(String questId) {
        QuestState quest = quests.get(questId);
        return quest != null ? quest.getStatus() : "none";
    }

    public static void setNotifications(Set<String> ids) {
        npcNotifications.clear();
        npcNotifications.addAll(ids);
    }
    public static boolean hasNotification(String dialogueId) {
        return npcNotifications.contains(dialogueId);
    }
    public static void removeNotification(String dialogueId) {
        npcNotifications.remove(dialogueId);
    }

    // ==================== Reputation ====================

    public static void setReputation(Map<String, Integer> rep) {
        reputation.clear();
        reputation.putAll(rep);
    }

    public static int getReputation(String id) {
        return reputation.getOrDefault(id, 0);
    }

    public static Map<String, Integer> getAllReputation() {
        return Collections.unmodifiableMap(reputation);
    }

    public static void clear() {
        visitedNodes.clear();
        history.clear();
        quests.clear();
        completedDialogues.clear();
        npcNotifications.clear();
        reputation.clear();
        trackedQuests.clear();
    }
}
