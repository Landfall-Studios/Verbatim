package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.PermissionService;

import java.util.UUID;

/**
 * Hytale-specific permission service using Hytale's native PermissionsModule.
 *
 * Uses PermissionsModule.get().hasPermission(UUID, String) for permission checks.
 */
public class HytalePermissionService extends PermissionService {

    public HytalePermissionService() {
        Verbatim.LOGGER.info("[HytalePermissionService] Initialized. Using Hytale PermissionsModule.");
    }

    @Override
    public boolean isPermissionSystemAvailable() {
        try {
            return PermissionsModule.get() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        UUID playerUuid = player.getUUID();

        try {
            PermissionsModule permissions = PermissionsModule.get();
            if (permissions != null) {
                boolean result = permissions.hasPermission(playerUuid, permissionNode);
                Verbatim.LOGGER.debug("[HytalePermissionService] Check '{}' for '{}': {}",
                    player.getUsername(), permissionNode, result);
                return result;
            }
        } catch (Exception e) {
            Verbatim.LOGGER.warn("[HytalePermissionService] Permission check failed: {}", e.getMessage());
        }

        Verbatim.LOGGER.debug("[HytalePermissionService] Falling back to permission level for '{}'", permissionNode);
        return fallbackPermissionCheck(player, fallbackPermissionLevel);
    }
}
