package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import world.landfall.verbatim.ChatEventHandler;
import world.landfall.verbatim.Verbatim;

/**
 * Hytale-specific event handlers that delegate to platform-independent ChatEventHandler.
 *
 * Events are registered in HytaleEntryPoint via the plugin event registry.
 * Hytale uses PlayerReadyEvent for join (when the player entity is ready in-world)
 * and PlayerDisconnectEvent for leave.
 */
public class HytaleChatEvents {

    /**
     * Handles player ready (join) events.
     * PlayerReadyEvent fires when a player has fully loaded into a world.
     * Loads the player's persisted data from disk before delegating to core login handler.
     */
    @SuppressWarnings("removal")
    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef != null) {
            HytaleGamePlayer gamePlayer = new HytaleGamePlayer(playerRef);

            // Load player data from per-player file and track as online
            if (Verbatim.gameContext instanceof HytaleGameContextImpl ctx) {
                ctx.loadPlayerFromDisk(gamePlayer.getUUID());
                ctx.trackPlayerOnline(gamePlayer.getUUID(), gamePlayer.getUsername());
            }

            ChatEventHandler.onPlayerLogin(gamePlayer);
        }
    }

    /**
     * Handles player disconnect events.
     */
    public static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef != null) {
            HytaleGamePlayer gamePlayer = new HytaleGamePlayer(playerRef);
            Verbatim.LOGGER.debug("[Verbatim] Player disconnect event for: {} ({})",
                gamePlayer.getUsername(), gamePlayer.getUUID());

            ChatEventHandler.onPlayerLogout(gamePlayer);

            // Remove from online tracking after logout handler has saved state
            if (Verbatim.gameContext instanceof HytaleGameContextImpl ctx) {
                ctx.trackPlayerOffline(gamePlayer.getUUID());
            }
        }
    }

    /**
     * Handles chat message events.
     * Cancels the default chat processing and routes through Verbatim's channel system.
     */
    public static void onPlayerChat(PlayerChatEvent event) {
        PlayerRef sender = event.getSender();
        String rawMessageText = event.getContent();

        // Cancel the default chat broadcast; Verbatim handles delivery
        event.setCancelled(true);

        ChatEventHandler.onChat(new HytaleGamePlayer(sender), rawMessageText);
    }
}
