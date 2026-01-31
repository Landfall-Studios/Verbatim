package world.landfall.verbatim.platform.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import world.landfall.verbatim.ChatEventHandler;

/**
 * NeoForge-specific event handlers that delegate to platform-independent ChatEventHandler.
 */
public class NeoForgeChatEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChatEventHandler.onPlayerLogin(new NeoForgeGamePlayer(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChatEventHandler.onPlayerLogout(new NeoForgeGamePlayer(player));
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String rawMessageText = event.getMessage().getString();
        event.setCanceled(true);
        ChatEventHandler.onChat(new NeoForgeGamePlayer(sender), rawMessageText);
    }
}
