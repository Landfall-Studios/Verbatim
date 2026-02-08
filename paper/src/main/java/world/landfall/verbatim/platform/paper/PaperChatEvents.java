package world.landfall.verbatim.platform.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import world.landfall.verbatim.ChatEventHandler;
import world.landfall.verbatim.Verbatim;

/**
 * Paper-specific event handlers that delegate to platform-independent ChatEventHandler.
 *
 * Registered as a Bukkit Listener in PaperEntryPoint.
 * Uses Paper's AsyncChatEvent (not the deprecated PlayerChatEvent).
 */
public class PaperChatEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PaperGamePlayer gamePlayer = new PaperGamePlayer(player);

        // Load player data from per-player file and track as online
        if (Verbatim.gameContext instanceof PaperGameContextImpl ctx) {
            ctx.loadPlayerFromDisk(gamePlayer.getUUID());
            ctx.trackPlayerOnline(gamePlayer.getUUID(), gamePlayer.getUsername());
        }

        ChatEventHandler.onPlayerLogin(gamePlayer);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PaperGamePlayer gamePlayer = new PaperGamePlayer(player);
        Verbatim.LOGGER.debug("[Verbatim] Player disconnect event for: {} ({})",
            gamePlayer.getUsername(), gamePlayer.getUUID());

        ChatEventHandler.onPlayerLogout(gamePlayer);

        // Remove from online tracking after logout handler has saved state
        if (Verbatim.gameContext instanceof PaperGameContextImpl ctx) {
            ctx.trackPlayerOffline(gamePlayer.getUUID());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String rawMessageText = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Cancel the default chat broadcast; Verbatim handles delivery
        event.setCancelled(true);

        ChatEventHandler.onChat(new PaperGamePlayer(sender), rawMessageText);
    }
}
