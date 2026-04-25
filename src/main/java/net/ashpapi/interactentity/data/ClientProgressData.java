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
    }

    public static void updateQuest(QuestState quest) {
        quests.put(quest.getId(), quest);
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

    public static void clear() {
        visitedNodes.clear();
        history.clear();
        quests.clear();
        completedDialogues.clear();
        npcNotifications.clear();
    }
}