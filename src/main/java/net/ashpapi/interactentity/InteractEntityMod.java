package net.ashpapi.interactentity;

import com.mojang.logging.LogUtils;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.summon.SummonScheduler;
import net.ashpapi.interactentity.action.ActionRegistry;
import net.ashpapi.interactentity.condition.ConditionRegistry;
import net.ashpapi.interactentity.entity.ModEntities;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.skin.SkinManager;
import net.ashpapi.interactentity.trade.TradeCatalogManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(InteractEntityMod.MOD_ID)
public class InteractEntityMod {
    public static final String MOD_ID = "interactentity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InteractEntityMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        ModEntities.ENTITIES.register(modBus);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("InteractEntity mod initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetwork.register();
        ActionRegistry.init();
        ConditionRegistry.init();
        LOGGER.info("InteractEntity common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        SummonScheduler.clearAll();
        DialogueManager manager = new DialogueManager();
        DialogueManager.setInstance(manager);
        manager.loadAll();
        TradeCatalogManager tradeManager = new TradeCatalogManager();
        TradeCatalogManager.setInstance(tradeManager);
        tradeManager.loadAll();
        SkinManager.loadAll(event.getServer());
    }
}