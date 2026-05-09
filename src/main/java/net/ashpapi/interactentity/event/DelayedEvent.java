package net.ashpapi.interactentity.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DelayedEvent {
    private final String id;
    private final long fireTick;
    private final List<JsonObject> actions;
    private final UUID playerUuid;

    public DelayedEvent(String id, long fireTick, List<JsonObject> actions, UUID playerUuid) {
        this.id = id;
        this.fireTick = fireTick;
        this.actions = actions;
        this.playerUuid = playerUuid;
    }

    public String getId() { return id; }
    public long getFireTick() { return fireTick; }
    public List<JsonObject> getActions() { return actions; }
    public UUID getPlayerUuid() { return playerUuid; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putLong("fireTick", fireTick);
        if (playerUuid != null) {
            tag.putUUID("playerUuid", playerUuid);
        }
        ListTag actionsTag = new ListTag();
        for (JsonObject action : actions) {
            actionsTag.add(StringTag.valueOf(action.toString()));
        }
        tag.put("actions", actionsTag);
        return tag;
    }

    public static DelayedEvent load(CompoundTag tag) {
        String id = tag.getString("id");
        long fireTick = tag.getLong("fireTick");
        UUID playerUuid = tag.hasUUID("playerUuid") ? tag.getUUID("playerUuid") : null;
        ListTag actionsTag = tag.getList("actions", Tag.TAG_STRING);
        List<JsonObject> actions = new ArrayList<>();
        for (int i = 0; i < actionsTag.size(); i++) {
            actions.add(JsonParser.parseString(actionsTag.getString(i)).getAsJsonObject());
        }
        return new DelayedEvent(id, fireTick, actions, playerUuid);
    }
}
