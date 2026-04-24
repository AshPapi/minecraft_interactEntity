package net.ashpapi.interactentity.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side cache: entityId → dialogue info, populated via NpcSyncPacket. */
public class ClientNpcRegistry {
    public static class Entry {
        public final String dialogueId;
        public final String entryNodeId;
        public Entry(String dialogueId, String entryNodeId) {
            this.dialogueId = dialogueId;
            this.entryNodeId = entryNodeId;
        }
    }

    private static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();

    public static void set(int entityId, String dialogueId, String entryNodeId) {
        ENTRIES.put(entityId, new Entry(dialogueId, entryNodeId));
    }

    public static void remove(int entityId) {
        ENTRIES.remove(entityId);
    }

    public static Entry get(int entityId) {
        return ENTRIES.get(entityId);
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
