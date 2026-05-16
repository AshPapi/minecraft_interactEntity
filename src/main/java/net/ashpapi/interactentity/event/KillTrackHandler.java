package net.ashpapi.interactentity.event;

import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.QuestUpdatePacket;
import net.ashpapi.interactentity.quest.QuestState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KillTrackHandler {
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        String typeId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();

        DialogueSavedData data = DialogueSavedData.get(player.serverLevel());
        data.addKill(typeId);

        for (QuestState quest : data.getActiveQuests()) {
            if (typeId.equals(quest.getKillEntityType())) {
                quest.addKillProgress(1);
                data.setDirty();
                ModNetwork.sendToAll(new QuestUpdatePacket(quest));
            }
        }
    }
}
