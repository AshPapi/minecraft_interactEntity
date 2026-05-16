package net.ashpapi.interactentity.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Fired when a dialogue session ends. completed=true if reached an end node, false if interrupted (escape/dist). */
public class DialogueEndEvent extends Event {
    private final ServerPlayer player;
    private final LivingEntity npc;
    private final String dialogueId;
    private final String lastNodeId;
    private final boolean completed;

    public DialogueEndEvent(ServerPlayer player, LivingEntity npc, String dialogueId, String lastNodeId, boolean completed) {
        this.player = player;
        this.npc = npc;
        this.dialogueId = dialogueId;
        this.lastNodeId = lastNodeId;
        this.completed = completed;
    }

    public ServerPlayer getPlayer() { return player; }
    public LivingEntity getNpc() { return npc; }
    public String getDialogueId() { return dialogueId; }
    public String getLastNodeId() { return lastNodeId; }
    public boolean isCompleted() { return completed; }
}
