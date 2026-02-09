package world.landfall.verbatim.platform.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import world.landfall.verbatim.ChatEventHandler;

/**
 * Forge 1.20.1 event handlers that delegate to platform-independent ChatEventHandler.
 */
public class ForgeChatEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChatEventHandler.onPlayerLogin(new ForgeGamePlayer(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChatEventHandler.onPlayerLogout(new ForgeGamePlayer(player));
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String rawMessageText = event.getMessage().getString();
        event.setCanceled(true);
        ChatEventHandler.onChat(new ForgeGamePlayer(sender), rawMessageText);
    }
}
