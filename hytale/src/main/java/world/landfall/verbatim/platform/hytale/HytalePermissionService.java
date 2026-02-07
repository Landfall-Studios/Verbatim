package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.PermissionService;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Hytale-specific permission service.
 *
 * Checks LuckPerms first (via reflection), falling back to Hytale's native
 * PermissionsModule if LuckPerms is not available.
 */
public class HytalePermissionService extends PermissionService {

    private Object luckPermsApi;
    private Boolean luckPermsAvailable;

    public HytalePermissionService() {
        this.luckPermsApi = null;
        this.luckPermsAvailable = null;
        Verbatim.LOGGER.info("[HytalePermissionService] Initialized. LuckPerms availability will be checked on first use.");
    }

    private void ensureLuckPermsChecked() {
        if (this.luckPermsAvailable == null) {
            try {
                Class<?> luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = luckPermsProviderClass.getMethod("get");
                this.luckPermsApi = getMethod.invoke(null);
                this.luckPermsAvailable = true;
                Verbatim.LOGGER.info("[HytalePermissionService] LuckPerms API found. Using LuckPerms for permissions.");
            } catch (ClassNotFoundException e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.info("[HytalePermissionService] LuckPerms not found. Using Hytale native permissions.");
            } catch (Exception e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.info("[HytalePermissionService] LuckPerms API not available: {}. Using Hytale native permissions.", e.getMessage());
            }
        }
    }

    @Override
    public boolean isPermissionSystemAvailable() {
        ensureLuckPermsChecked();
        // Either LuckPerms or Hytale native permissions are available
        return this.luckPermsAvailable || isHytalePermissionsAvailable();
    }

    private boolean isHytalePermissionsAvailable() {
        try {
            return PermissionsModule.get() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        ensureLuckPermsChecked();

        // Try LuckPerms first
        if (this.luckPermsAvailable && this.luckPermsApi != null) {
            Boolean lpResult = checkLuckPermsPermission(player, permissionNode);
            if (lpResult != null) {
                return lpResult;
            }
            // LuckPerms check failed, fall through to Hytale native
        }

        // Try Hytale native PermissionsModule
        Boolean hytaleResult = checkHytaleNativePermission(player, permissionNode);
        if (hytaleResult != null) {
            return hytaleResult;
        }

        // Final fallback to permission level
        Verbatim.LOGGER.debug("[HytalePermissionService] All permission checks failed for '{}', using level fallback", permissionNode);
        return fallbackPermissionCheck(player, fallbackPermissionLevel);
    }

    private Boolean checkLuckPermsPermission(GamePlayer player, String permissionNode) {
        try {
            Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
            Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

            Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
            Object user = getUserMethod.invoke(userManager, player.getUUID());

            if (user == null) {
                // Try to load user
                Verbatim.LOGGER.debug("[HytalePermissionService] User '{}' not cached in LuckPerms, attempting load...", player.getUsername());
                try {
                    Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
                    Object completableFuture = loadUserMethod.invoke(userManager, player.getUUID());
                    Method getMethod = completableFuture.getClass().getMethod("get", long.class, TimeUnit.class);
                    user = getMethod.invoke(completableFuture, 2L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[HytalePermissionService] Failed to load user '{}': {}", player.getUsername(), e.getMessage());
                    return null;
                }
            }

            if (user != null) {
                Method getCachedDataMethod = user.getClass().getMethod("getCachedData");
                Object cachedData = getCachedDataMethod.invoke(user);
                Method getPermissionDataMethod = cachedData.getClass().getMethod("getPermissionData");
                Object permissionData = getPermissionDataMethod.invoke(cachedData);
                Method checkPermissionMethod = permissionData.getClass().getMethod("checkPermission", String.class);
                Object permissionResult = checkPermissionMethod.invoke(permissionData, permissionNode);
                Method asBooleanMethod = permissionResult.getClass().getMethod("asBoolean");
                boolean result = (Boolean) asBooleanMethod.invoke(permissionResult);

                Verbatim.LOGGER.debug("[HytalePermissionService] LuckPerms check for '{}', node '{}': {}",
                    player.getUsername(), permissionNode, result);
                return result;
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[HytalePermissionService] LuckPerms permission check failed: {}", e.getMessage());
        }
        return null;
    }

    private Boolean checkHytaleNativePermission(GamePlayer player, String permissionNode) {
        try {
            PermissionsModule permissions = PermissionsModule.get();
            if (permissions != null) {
                boolean result = permissions.hasPermission(player.getUUID(), permissionNode);
                Verbatim.LOGGER.debug("[HytalePermissionService] Hytale native check for '{}', node '{}': {}",
                    player.getUsername(), permissionNode, result);
                return result;
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[HytalePermissionService] Hytale native permission check failed: {}", e.getMessage());
        }
        return null;
    }
}
