package world.landfall.verbatim.platform.hytale;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.PermissionService;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Hytale-specific permission service using PermissionsPlus.
 * Falls back to permission level checks if PermissionsPlus is not available.
 */
public class HytalePermissionService extends PermissionService {

    private Object permissionsPlusApi;
    private Boolean permissionsPlusAvailable;

    public HytalePermissionService() {
        this.permissionsPlusApi = null;
        this.permissionsPlusAvailable = null;
        Verbatim.LOGGER.info("[HytalePermissionService] Initialized. PermissionsPlus availability will be checked on first use.");
    }

    private void ensurePermissionsPlusChecked() {
        if (this.permissionsPlusAvailable == null) {
            try {
                // Try to load PermissionsPlus API via reflection
                Class<?> permissionsPlusClass = Class.forName("com.hypixel.hytale.permissionsplus.PermissionsPlus");
                Method getInstanceMethod = permissionsPlusClass.getMethod("getInstance");
                this.permissionsPlusApi = getInstanceMethod.invoke(null);
                this.permissionsPlusAvailable = true;
                Verbatim.LOGGER.info("[HytalePermissionService] PermissionsPlus API found and loaded.");
            } catch (ClassNotFoundException e) {
                this.permissionsPlusApi = null;
                this.permissionsPlusAvailable = false;
                Verbatim.LOGGER.warn("[HytalePermissionService] PermissionsPlus not found. Using fallback permission checks.");
            } catch (Exception e) {
                this.permissionsPlusApi = null;
                this.permissionsPlusAvailable = false;
                Verbatim.LOGGER.warn("[HytalePermissionService] PermissionsPlus unavailable: {}. Using fallback.", e.getMessage());
            }
        }
    }

    @Override
    public boolean isPermissionSystemAvailable() {
        ensurePermissionsPlusChecked();
        return this.permissionsPlusAvailable;
    }

    @Override
    protected boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        ensurePermissionsPlusChecked();

        if (this.permissionsPlusAvailable && this.permissionsPlusApi != null) {
            try {
                // PermissionsPlus API: hasPermission(UUID playerUuid, String permission)
                Method hasPermissionMethod = this.permissionsPlusApi.getClass()
                    .getMethod("hasPermission", UUID.class, String.class);
                Boolean result = (Boolean) hasPermissionMethod.invoke(
                    this.permissionsPlusApi, player.getUUID(), permissionNode);

                Verbatim.LOGGER.debug("[HytalePermissionService] PermissionsPlus check for '{}', node '{}': {}",
                    player.getUsername(), permissionNode, result);
                return result != null && result;

            } catch (Exception e) {
                Verbatim.LOGGER.warn("[HytalePermissionService] PermissionsPlus check failed for '{}': {}. Using fallback.",
                    player.getUsername(), e.getMessage());
            }
        }

        return fallbackPermissionCheck(player, fallbackPermissionLevel);
    }
}
