package net.ashpapi.interactentity.action;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class RunCommandAction implements DialogueAction {
    @Override
    public void execute(ServerPlayer player, LivingEntity entity, JsonObject params) {
        String command = params.get("command").getAsString();

        // source.getEntity() == player, поэтому @s в команде ссылается на игрока.
        // Не заменяем @s текстом — это сломало бы большинство команд.
        CommandSourceStack source = player.getServer().createCommandSourceStack()
                .withEntity(player)
                .withPosition(player.position())
                .withPermission(2);

        player.getServer().getCommands().performPrefixedCommand(source, command);
    }
}
