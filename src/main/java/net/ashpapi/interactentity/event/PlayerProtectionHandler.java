package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.trade.TradeSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerProtectionHandler {
    private static final Map<UUID, Boolean> previousInvulnerableState = new HashMap<>();

    public static void protect(ServerPlayer player) {
        previousInvulnerableState.put(player.getUUID(), player.isInvulnerable());
        player.setInvulnerable(true);
    }

    public static void unprotect(ServerPlayer player) {
        Boolean prev = previousInvulnerableState.remove(player.getUUID());
        if (prev != null) {
            player.setInvulnerable(prev);
        } else {
            player.setInvulnerable(false);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InteractEntityMod.LOGGER.debug("[Event] Player logged out – ending session");
            // Закрываем торговлю ДО диалога: TradeSession.close восстанавливает NPC
            // в состояние «во время диалога» (то, что она застала при открытии торга),
            // а endSession уже доводит NPC до исходного состояния. В обратном порядке
            // close перезаписал бы результат endSession и оставил NPC замороженным.
            // Пакет клиенту не шлём — игрок уже отключился, сессию чистим на сервере.
            if (TradeSession.hasActive(player)) {
                TradeSession.close(player);
            }
            DialogueSession.endSession(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (DialogueSession.hasActiveSession(player)) {
                InteractEntityMod.LOGGER.debug("[Event] Player died – ending session");
                DialogueSession.endSession(player);
            }
        }
    }
}