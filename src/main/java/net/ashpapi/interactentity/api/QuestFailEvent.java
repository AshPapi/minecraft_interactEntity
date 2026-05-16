package net.ashpapi.interactentity.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class QuestFailEvent extends Event {
    private final ServerPlayer player;
    private final String questId;
    private final String scope;

    public QuestFailEvent(ServerPlayer player, String questId, String scope) {
        this.player = player;
        this.questId = questId;
        this.scope = scope;
    }

    public ServerPlayer getPlayer() { return player; }
    public String getQuestId() { return questId; }
    public String getScope() { return scope; }
}
