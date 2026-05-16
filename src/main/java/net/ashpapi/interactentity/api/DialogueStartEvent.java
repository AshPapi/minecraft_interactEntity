package net.ashpapi.interactentity.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Fired on the Forge event bus when a dialogue session starts. */
public class DialogueStartEvent extends Event {
    private final ServerPlayer player;
    private final LivingEntity npc;
    private final String dialogueId;
    private final String startNodeId;

    public DialogueStartEvent(ServerPlayer player, LivingEntity npc, String dialogueId, String startNodeId) {
        this.player = player;
        this.npc = npc;
        this.dialogueId = dialogueId;
        this.startNodeId = startNodeId;
    }

    public ServerPlayer getPlayer() { return player; }
    public LivingEntity getNpc() { return npc; }
    public String getDialogueId() { return dialogueId; }
    public String getStartNodeId() { return startNodeId; }
}
