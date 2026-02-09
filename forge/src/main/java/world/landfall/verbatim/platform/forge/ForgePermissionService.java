package world.landfall.verbatim.platform.forge;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.PermissionService;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Forge 1.20.1 permission service.
 *
 * Checks LuckPerms first (via reflection), falling back to vanilla OP levels
 * if LuckPerms is not available.
 */
public class ForgePermissionService extends PermissionService {

    private Object luckPermsApi;
    private Boolean luckPermsAvailable;

    public ForgePermissionService() {
        this.luckPermsApi = null;
        this.luckPermsAvailable = null;
        Verbatim.LOGGER.info("[ForgePermissionService] Initialized. LuckPerms availability will be checked on first use.");
    }

    private void ensureLuckPermsChecked() {
        if (this.luckPermsAvailable == null) {
            try {
                Class<?> luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = luckPermsProviderClass.getMethod("get");
                this.luckPermsApi = getMethod.invoke(null);
                this.luckPermsAvailable = true;
                Verbatim.LOGGER.info("[ForgePermissionService] LuckPerms API found. Using LuckPerms for permissions.");
            } catch (ClassNotFoundException e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.warn("[ForgePermissionService] LuckPerms not found. Using vanilla OP levels as fallback.");
            } catch (Exception e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.warn("[ForgePermissionService] LuckPerms API not available: {}. Using vanilla OP levels as fallback.", e.getMessage());
            }
        }
    }

    @Override
    public boolean isPermissionSystemAvailable() {
        ensureLuckPermsChecked();
        return this.luckPermsAvailable;
    }

    @Override
    protected boolean checkPermission(GamePlayer player, String permissionNode, int fallbackPermissionLevel) {
        ensureLuckPermsChecked();

        if (this.luckPermsAvailable && this.luckPermsApi != null) {
            try {
                Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
                Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

                Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
                Object user = getUserMethod.invoke(userManager, player.getUUID());

                if (user != null) {
                    boolean result = checkPermissionFromUser(user, permissionNode);
                    Verbatim.LOGGER.debug("[ForgePermissionService] LuckPerms check for '{}', node '{}': {}",
                        player.getUsername(), permissionNode, result);
                    return result;
                } else {
                    Verbatim.LOGGER.warn("[ForgePermissionService] User '{}' not cached in LuckPerms, attempting load...",
                        player.getUsername());
                    try {
                        Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
                        Object completableFuture = loadUserMethod.invoke(userManager, player.getUUID());
                        Method getMethod = completableFuture.getClass().getMethod("get");
                        Object loadedUser = getMethod.invoke(completableFuture);

                        if (loadedUser != null) {
                            boolean result = checkPermissionFromUser(loadedUser, permissionNode);
                            Verbatim.LOGGER.info("[ForgePermissionService] After loading user '{}', permission check for node '{}': {}",
                                player.getUsername(), permissionNode, result);
                            return result;
                        } else {
                            Verbatim.LOGGER.warn("[ForgePermissionService] Loaded user '{}' was null. Falling back to OP check.",
                                player.getUsername());
                        }
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        if (e.getCause() instanceof IllegalStateException) {
                            Verbatim.LOGGER.warn("[ForgePermissionService] LuckPerms capability not available for '{}' during load ({}). Falling back to OP check.",
                                player.getUsername(), e.getCause().getMessage());
                        } else {
                            Verbatim.LOGGER.error("[ForgePermissionService] Failed to load user '{}': {}",
                                player.getUsername(), e.getMessage());
                        }
                    } catch (Exception e) {
                        Verbatim.LOGGER.error("[ForgePermissionService] Failed to load user '{}': {}",
                            player.getUsername(), e.getMessage());
                    }
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof IllegalStateException) {
                    Verbatim.LOGGER.warn("[ForgePermissionService] LuckPerms capability not available for '{}' ({}). Falling back to OP check.",
                        player.getUsername(), e.getCause().getMessage());
                } else {
                    Verbatim.LOGGER.error("[ForgePermissionService] Error accessing LuckPerms for '{}': {}",
                        player.getUsername(), e.getMessage());
                }
            } catch (Exception e) {
                Verbatim.LOGGER.error("[ForgePermissionService] Error accessing LuckPerms for '{}': {}",
                    player.getUsername(), e.getMessage());
            }
        }

        return fallbackPermissionCheck(player, fallbackPermissionLevel);
    }

    private boolean checkPermissionFromUser(Object user, String permissionNode) throws Exception {
        Method getCachedDataMethod = user.getClass().getMethod("getCachedData");
        Object cachedData = getCachedDataMethod.invoke(user);
        Method getPermissionDataMethod = cachedData.getClass().getMethod("getPermissionData");
        Object permissionData = getPermissionDataMethod.invoke(cachedData);
        Method checkPermissionMethod = permissionData.getClass().getMethod("checkPermission", String.class);
        Object permissionResult = checkPermissionMethod.invoke(permissionData, permissionNode);
        Method asBooleanMethod = permissionResult.getClass().getMethod("asBoolean");
        return (Boolean) asBooleanMethod.invoke(permissionResult);
    }
}
