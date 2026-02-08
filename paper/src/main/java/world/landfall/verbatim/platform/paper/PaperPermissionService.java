package world.landfall.verbatim.platform.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.PermissionService;

/**
 * Paper-specific permission service.
 *
 * Paper/Bukkit has native permission support via Player.hasPermission(String).
 * This works with LuckPerms, Vault, and any other permission plugin
 * that implements the Bukkit permission API.
 * No reflection needed - the Bukkit API handles it natively.
 */
public class PaperPermissionService extends PermissionService {

    public PaperPermissionService() {
        Verbatim.LOGGER.info("[PaperPermissionService] Initialized. Using Bukkit native permission API.");
    }

    @Override
    public boolean isPermissionSystemAvailable() {
        // Bukkit's native permission system is always available.
        // LuckPerms/Vault integration happens transparently through the Bukkit API.
        return true;
    }

    @Override
    protected boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        if (player instanceof PaperGamePlayer paperPlayer) {
            Player bukkitPlayer = paperPlayer.getHandle();
            boolean result = bukkitPlayer.hasPermission(permissionNode);
            Verbatim.LOGGER.debug("[PaperPermissionService] Permission check for '{}', node '{}': {}",
                player.getUsername(), permissionNode, result);
            return result;
        }

        // Fallback: try to get player from Bukkit by UUID
        Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer != null) {
            return bukkitPlayer.hasPermission(permissionNode);
        }

        // Final fallback to permission level
        Verbatim.LOGGER.debug("[PaperPermissionService] Player not found as Bukkit player, using level fallback for '{}'", permissionNode);
        return fallbackPermissionCheck(player, fallbackPermissionLevel);
    }
}
