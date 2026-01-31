package world.landfall.verbatim.util;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;

import java.lang.reflect.Method;
import java.util.UUID;

public class PermissionService {
    private Object luckPermsApi;
    private Boolean luckPermsAvailable;

    public PermissionService() {
        this.luckPermsApi = null;
        this.luckPermsAvailable = null;
        Verbatim.LOGGER.info("[Verbatim PermissionService] PermissionService initialized. LuckPerms availability will be checked when first needed.");
    }

    private void ensureLuckPermsChecked() {
        if (this.luckPermsAvailable == null) {
            try {
                Class<?> luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = luckPermsProviderClass.getMethod("get");
                this.luckPermsApi = getMethod.invoke(null);
                this.luckPermsAvailable = true;
                Verbatim.LOGGER.info("[Verbatim PermissionService] LuckPerms API found and loaded. Permissions will be handled by LuckPerms.");
            } catch (ClassNotFoundException e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.warn("[Verbatim PermissionService] LuckPerms classes not found. Permissions will use vanilla OP levels as fallback.");
            } catch (Exception e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.warn("[Verbatim PermissionService] LuckPerms API not available: {}. Permissions will use vanilla OP levels as fallback.", e.getMessage());
            }
        }
    }

    public boolean isLuckPermsAvailable() {
        ensureLuckPermsChecked();
        return this.luckPermsAvailable;
    }

    public boolean hasPermission(GamePlayer player, String permissionNode, int opLevelIfLuckPermsAbsent) {
        if (player == null) {
            Verbatim.LOGGER.warn("[Verbatim PermissionService] Attempted to check permission for a null player. Denying.");
            return false;
        }
        if (permissionNode == null || permissionNode.isEmpty()) {
            Verbatim.LOGGER.warn("[Verbatim PermissionService] Attempted to check a null or empty permissionNode for player {}. Denying.", player.getUsername());
            return false;
        }

        ensureLuckPermsChecked();

        if (this.luckPermsAvailable && this.luckPermsApi != null) {
            try {
                Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
                Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

                Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
                Object user = getUserMethod.invoke(userManager, player.getUUID());

                if (user != null) {
                    Method getCachedDataMethod = user.getClass().getMethod("getCachedData");
                    Object cachedData = getCachedDataMethod.invoke(user);
                    Method getPermissionDataMethod = cachedData.getClass().getMethod("getPermissionData");
                    Object permissionData = getPermissionDataMethod.invoke(cachedData);
                    Method checkPermissionMethod = permissionData.getClass().getMethod("checkPermission", String.class);
                    Object permissionResult = checkPermissionMethod.invoke(permissionData, permissionNode);
                    Method asBooleanMethod = permissionResult.getClass().getMethod("asBoolean");
                    boolean checkResult = (Boolean) asBooleanMethod.invoke(permissionResult);

                    Verbatim.LOGGER.debug("[Verbatim PermissionService] LuckPerms check for player '{}', node '{}': {} (UUID: {})",
                                       player.getUsername(), permissionNode, checkResult, player.getUUID());
                    return checkResult;
                } else {
                    Verbatim.LOGGER.warn("[Verbatim PermissionService] LuckPerms available, but user '{}' (UUID: {}) not found by LuckPerms. Attempting to load user...",
                                       player.getUsername(), player.getUUID());
                    try {
                        Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
                        Object completableFuture = loadUserMethod.invoke(userManager, player.getUUID());
                        Method getMethod = completableFuture.getClass().getMethod("get");
                        Object loadedUser = getMethod.invoke(completableFuture);

                        if (loadedUser != null) {
                            Method getCachedDataMethod = loadedUser.getClass().getMethod("getCachedData");
                            Object cachedData = getCachedDataMethod.invoke(loadedUser);
                            Method getPermissionDataMethod = cachedData.getClass().getMethod("getPermissionData");
                            Object permissionData = getPermissionDataMethod.invoke(cachedData);
                            Method checkPermissionMethod = permissionData.getClass().getMethod("checkPermission", String.class);
                            Object permissionResult = checkPermissionMethod.invoke(permissionData, permissionNode);
                            Method asBooleanMethod = permissionResult.getClass().getMethod("asBoolean");
                            boolean checkResult = (Boolean) asBooleanMethod.invoke(permissionResult);

                            Verbatim.LOGGER.info("[Verbatim PermissionService] After loading user '{}', permission check for node '{}': {}",
                                               player.getUsername(), permissionNode, checkResult);
                            return checkResult;
                        } else {
                            Verbatim.LOGGER.warn("[Verbatim PermissionService] Loaded user '{}' was null. Falling back to OP check for permission '{}'.",
                                               player.getUsername(), permissionNode);
                        }
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        if (e.getCause() instanceof IllegalStateException) {
                            Verbatim.LOGGER.warn("[Verbatim PermissionService] LuckPerms capability not available for player '{}' during user load ({}). This can happen during respawn. Falling back to OP level check.",
                                               player.getUsername(), e.getCause().getMessage());
                        } else {
                            Verbatim.LOGGER.error("[Verbatim PermissionService] Failed to load user '{}' from LuckPerms via reflection (InvocationTargetException): {}. Cause: {}. Falling back to OP check.",
                                                player.getUsername(), e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "null");
                        }
                    } catch (Exception e) {
                        Verbatim.LOGGER.error("[Verbatim PermissionService] Failed to load user '{}' from LuckPerms via reflection (General Exception): {}. Falling back to OP check.",
                                            player.getUsername(), e.getMessage());
                    }
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof IllegalStateException) {
                    Verbatim.LOGGER.warn("[Verbatim PermissionService] LuckPerms capability not available for player '{}' ({}). This can happen during respawn. Falling back to OP level check.",
                                       player.getUsername(), e.getCause().getMessage());
                } else {
                    Verbatim.LOGGER.error("[Verbatim PermissionService] Error accessing LuckPerms API via reflection (InvocationTargetException for {}): {}. Cause: {}. Falling back to OP check.",
                                        player.getUsername(), e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "null");
                }
            } catch (Exception e) {
                Verbatim.LOGGER.error("[Verbatim PermissionService] Error accessing LuckPerms API via reflection (General Exception for {}): {}. Falling back to OP check.",
                                    player.getUsername(), e.getMessage());
            }
        }

        boolean opCheckResult = Verbatim.gameContext.hasPermissionLevel(player, opLevelIfLuckPermsAbsent);
        Verbatim.LOGGER.info("[Verbatim PermissionService] Vanilla OP check for player '{}', level {}: {} (for permission '{}')",
                            player.getUsername(), opLevelIfLuckPermsAbsent, opCheckResult, permissionNode);
        return opCheckResult;
    }
}
