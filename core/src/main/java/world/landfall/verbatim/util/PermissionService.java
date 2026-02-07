package world.landfall.verbatim.util;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;

/**
 * Abstract permission service that platform implementations extend.
 * Provides common validation logic; subclasses implement platform-specific permission checks.
 */
public abstract class PermissionService {

    /**
     * Returns whether a platform-specific permission system is available.
     * (e.g., LuckPerms for NeoForge, PermissionsPlus for Hytale)
     */
    public abstract boolean isPermissionSystemAvailable();

    /**
     * Check if a player has a specific permission node.
     *
     * @param player The player to check
     * @param permissionNode The permission node (e.g., "verbatim.channel.staff")
     * @param fallbackPermissionLevel The permission level to require if no permission system is available
     * @return true if the player has the permission
     */
    public boolean hasPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        if (player == null) {
            Verbatim.LOGGER.warn("[PermissionService] Attempted to check permission for null player. Denying.");
            return false;
        }
        if (permissionNode == null || permissionNode.isEmpty()) {
            Verbatim.LOGGER.warn("[PermissionService] Attempted to check null/empty permission for player {}. Denying.",
                player.getUsername());
            return false;
        }

        return checkPermission(player, permissionNode, fallbackPermissionLevel);
    }

    /**
     * Platform-specific permission check implementation.
     * Called after basic validation passes.
     */
    protected abstract boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel);

    /**
     * Fallback permission check using vanilla permission levels.
     * Subclasses can call this when their permission system is unavailable.
     */
    protected boolean fallbackPermissionCheck(GamePlayer player, int permissionLevel) {
        boolean result = Verbatim.gameContext.hasPermissionLevel(player, permissionLevel);
        Verbatim.LOGGER.debug("[PermissionService] Fallback check for player '{}', level {}: {}",
            player.getUsername(), permissionLevel, result);
        return result;
    }
}
