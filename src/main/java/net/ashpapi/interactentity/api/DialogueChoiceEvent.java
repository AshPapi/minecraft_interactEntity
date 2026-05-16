package net.ashpapi.interactentity.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Fired when a player selects a dialogue option, or when a fire_event action runs.
 *  source = "option" for player choices, "action" for fire_event-triggered events.
 *  tag = custom payload (option index "option_N" for choices, user-defined for fire_event). */
public class DialogueChoiceEvent extends Event {
    private final ServerPlayer player;
    private final LivingEntity npc;
    private final String dialogueId;
    private final String nodeId;
    private final String source;
    private final String tag;

    public DialogueChoiceEvent(ServerPlayer player, LivingEntity npc, String dialogueId, String nodeId, String source, String tag) {
        this.player = player;
        this.npc = npc;
        this.dialogueId = dialogueId;
        this.nodeId = nodeId;
        this.source = source;
        this.tag = tag;
    }

    public ServerPlayer getPlayer() { return player; }
    public LivingEntity getNpc() { return npc; }
    public String getDialogueId() { return dialogueId; }
    public String getNodeId() { return nodeId; }
    public String getSource() { return source; }
    public String getTag() { return tag; }
}
