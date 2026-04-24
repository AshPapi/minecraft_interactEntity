package net.ashpapi.interactentity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.ashpapi.interactentity.InteractEntityMod;
import net.ashpapi.interactentity.data.DialogueSavedData;
import net.ashpapi.interactentity.dialogue.DialogueManager;
import net.ashpapi.interactentity.dialogue.DialogueSession;
import net.ashpapi.interactentity.dialogue.DialogueTree;
import net.ashpapi.interactentity.network.ModNetwork;
import net.ashpapi.interactentity.network.SyncProgressPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = InteractEntityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DialogueCommand {

    private static final SuggestionProvider<CommandSourceStack> DIALOGUE_IDS = (ctx, builder) -> {
        DialogueManager mgr = DialogueManager.get();
        if (mgr != null) {
            for (String id : mgr.getLoadedIds()) builder.suggest(id);
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("dialogue")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(ctx -> reloadAll(ctx.getSource()))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(DIALOGUE_IDS)
                        .executes(ctx -> reloadOne(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                    )
                )
                .then(Commands.literal("test")
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(DIALOGUE_IDS)
                        .executes(ctx -> testDialogue(ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"), null))
                        .then(Commands.argument("node", StringArgumentType.string())
                            .executes(ctx -> testDialogue(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id"),
                                    StringArgumentType.getString(ctx, "node")))
                        )
                    )
                )
                .then(Commands.literal("goto")
                    .then(Commands.argument("node", StringArgumentType.string())
                        .executes(ctx -> gotoNode(ctx.getSource(),
                                StringArgumentType.getString(ctx, "node")))
                    )
                )
        );
    }

    private static int reloadAll(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds())
              .thenRun(() -> src.sendSuccess(
                  () -> Component.translatable("command.interactentity.reloaded"), true));
        return 1;
    }

    private static int testDialogue(CommandSourceStack src, String id, String startNode) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Only players can test"));
            return 0;
        }
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) { src.sendFailure(Component.literal("Manager not ready")); return 0; }
        DialogueTree tree = mgr.getDialogueById(id);
        if (tree == null) { src.sendFailure(Component.literal("Unknown dialogue: " + id)); return 0; }
        AABB box = new AABB(src.getPosition().subtract(8, 8, 8), src.getPosition().add(8, 8, 8));
        List<LivingEntity> near = src.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof ServerPlayer));
        if (near.isEmpty()) { src.sendFailure(Component.literal("No entity within 8 blocks")); return 0; }
        LivingEntity target = near.get(0);
        if (DialogueSession.hasActiveSession(player)) DialogueSession.endSession(player);
        DialogueSession.startSessionFromNode(player, target, tree,
                startNode != null ? startNode : tree.getEntryNodeId());
        return 1;
    }

    private static int gotoNode(CommandSourceStack src, String node) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("Only players"));
            return 0;
        }
        if (!DialogueSession.hasActiveSession(player)) {
            src.sendFailure(Component.literal("No active dialogue"));
            return 0;
        }
        DialogueSession.jumpToNode(player, node);
        return 1;
    }

    private static int reloadOne(CommandSourceStack src, String id) {
        DialogueManager mgr = DialogueManager.get();
        if (mgr == null) {
            src.sendFailure(Component.literal("DialogueManager not ready"));
            return 0;
        }
        MinecraftServer server = src.getServer();
        boolean ok = mgr.reloadOne(id);
        if (ok) {
            // Полный сброс прогресса для этого диалога + всех квестов (режим тестирования)
            DialogueSavedData data = DialogueSavedData.get(server.overworld());
            data.resetDialogue(id);
            data.clearAllQuests();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ModNetwork.sendToPlayer(p, new SyncProgressPacket(data));
            }
            src.sendSuccess(() -> Component.literal("Reloaded and reset: " + id), true);
            return 1;
        } else {
            src.sendFailure(Component.literal("Failed to reload dialogue: " + id));
            return 0;
        }
    }
}
