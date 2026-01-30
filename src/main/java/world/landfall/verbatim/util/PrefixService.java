package world.landfall.verbatim.util;

import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.Verbatim;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class PrefixService {
    private Object luckPermsApi;
    private Boolean luckPermsAvailable;

    public PrefixService() {
        this.luckPermsApi = null;
        this.luckPermsAvailable = null;
        Verbatim.LOGGER.info("[Verbatim PrefixService] PrefixService initialized. LuckPerms availability will be checked when first needed.");
    }

    /**
     * Lazy-load LuckPerms API. This is called on first prefix check.
     * Uses reflection to avoid NoClassDefFoundError when LuckPerms is not installed.
     */
    private void ensureLuckPermsChecked() {
        if (this.luckPermsAvailable == null) {
            try {
                Class<?> luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = luckPermsProviderClass.getMethod("get");
                this.luckPermsApi = getMethod.invoke(null);
                this.luckPermsAvailable = true;
                Verbatim.LOGGER.info("[Verbatim PrefixService] LuckPerms API found and loaded. Prefixes will be handled by LuckPerms.");
            } catch (ClassNotFoundException e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.info("[Verbatim PrefixService] LuckPerms classes not found. Prefixes will not be displayed.");
            } catch (Exception e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.info("[Verbatim PrefixService] LuckPerms API not available: {}. Prefixes will not be displayed.", e.getMessage());
            }
        }
    }

    public boolean isLuckPermsAvailable() {
        ensureLuckPermsChecked();
        return this.luckPermsAvailable;
    }

    /**
     * Gets the player's prefix from LuckPerms.
     *
     * @param player The player to get the prefix for
     * @return The prefix string, or empty string if none found
     */
    public String getPlayerPrefix(ServerPlayer player) {
        if (player == null) {
            return "";
        }

        ensureLuckPermsChecked();

        if (!this.luckPermsAvailable || this.luckPermsApi == null) {
            return "";
        }

        try {
            Object user = getUser(player);
            if (user != null) {
                String prefix = getPrefixFromUser(user);
                return prefix != null ? prefix : "";
            } else {
                // Try to load the user synchronously
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        String prefix = getPrefixFromUser(loadedUser);
                        return prefix != null ? prefix : "";
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for prefix: {}",
                                        Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting prefix for player '{}': {}",
                                Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
        }

        return "";
    }

    /**
     * Gets the player's primary group from LuckPerms.
     *
     * @param player The player to get the primary group for
     * @return The primary group name, or empty string if none found
     */
    public String getPlayerPrimaryGroup(ServerPlayer player) {
        if (player == null) {
            return "";
        }

        ensureLuckPermsChecked();

        if (!this.luckPermsAvailable || this.luckPermsApi == null) {
            return "";
        }

        try {
            Object user = getUser(player);
            if (user != null) {
                Method getPrimaryGroupMethod = user.getClass().getMethod("getPrimaryGroup");
                String primaryGroup = (String) getPrimaryGroupMethod.invoke(user);
                return primaryGroup != null ? primaryGroup : "";
            } else {
                // Try to load the user synchronously
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        Method getPrimaryGroupMethod = loadedUser.getClass().getMethod("getPrimaryGroup");
                        String primaryGroup = (String) getPrimaryGroupMethod.invoke(loadedUser);
                        return primaryGroup != null ? primaryGroup : "";
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for primary group: {}",
                                        Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting primary group for player '{}': {}",
                                Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
        }

        return "";
    }

    /**
     * Gets all groups that the player is a member of.
     *
     * @param player The player to get groups for
     * @return List of group names the player is in
     */
    public List<String> getPlayerGroups(ServerPlayer player) {
        List<String> groups = new ArrayList<>();

        if (player == null) {
            return groups;
        }

        ensureLuckPermsChecked();

        if (!this.luckPermsAvailable || this.luckPermsApi == null) {
            return groups;
        }

        try {
            Object user = getUser(player);
            if (user != null) {
                extractGroupsFromUser(user, groups);
            } else {
                // Try to load the user synchronously
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        extractGroupsFromUser(loadedUser, groups);
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for groups: {}",
                                        Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting groups for player '{}': {}",
                                Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
        }

        return groups;
    }

    /**
     * Gets the display name for a group from LuckPerms.
     *
     * @param groupName The name of the group
     * @return The display name of the group, or the group name if no display name is set
     */
    public String getGroupDisplayName(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return "";
        }

        ensureLuckPermsChecked();

        if (!this.luckPermsAvailable || this.luckPermsApi == null) {
            return groupName;
        }

        try {
            Method getGroupManagerMethod = this.luckPermsApi.getClass().getMethod("getGroupManager");
            Object groupManager = getGroupManagerMethod.invoke(this.luckPermsApi);

            Method getGroupMethod = groupManager.getClass().getMethod("getGroup", String.class);
            Object group = getGroupMethod.invoke(groupManager, groupName);

            if (group != null) {
                Object metaData = getMetaDataFromHolder(group);
                if (metaData != null) {
                    Method getMetaValueMethod = metaData.getClass().getMethod("getMetaValue", String.class);
                    String displayName = (String) getMetaValueMethod.invoke(metaData, "displayname");
                    return displayName != null ? displayName : groupName;
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting display name for group '{}': {}",
                                groupName, e.getMessage());
        }

        return groupName;
    }

    /**
     * Gets the prefix tooltip from LuckPerms meta data.
     * Looks for a meta key like "prefix_tooltip.0" that corresponds to the prefix.
     *
     * @param player The player to get the prefix tooltip for
     * @return The tooltip text, or null if no tooltip is defined
     */
    public String getPrefixTooltip(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        ensureLuckPermsChecked();

        if (!this.luckPermsAvailable || this.luckPermsApi == null) {
            return null;
        }

        try {
            Object user = getUser(player);
            if (user != null) {
                String tooltip = findPrefixTooltip(user);
                if (tooltip != null) {
                    return tooltip;
                }
            } else {
                // Try to load the user synchronously
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        String tooltip = findPrefixTooltip(loadedUser);
                        if (tooltip != null) {
                            return tooltip;
                        }
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for prefix tooltip: {}",
                                        Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting prefix tooltip for player '{}': {}",
                                Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
        }

        return null;
    }

    // ===== Helper methods for reflection-based LuckPerms access =====

    /**
     * Gets a User object from LuckPerms for the given player.
     */
    private Object getUser(ServerPlayer player) throws Exception {
        Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
        Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

        Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
        return getUserMethod.invoke(userManager, Verbatim.gameContext.getPlayerUUID(player));
    }

    /**
     * Loads a User object from LuckPerms for the given player (synchronous).
     */
    private Object loadUser(ServerPlayer player) throws Exception {
        Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
        Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

        Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
        Object completableFuture = loadUserMethod.invoke(userManager, Verbatim.gameContext.getPlayerUUID(player));

        Method getMethod = completableFuture.getClass().getMethod("get");
        return getMethod.invoke(completableFuture);
    }

    /**
     * Gets the CachedMetaData from a permission holder (User or Group).
     */
    private Object getMetaDataFromHolder(Object holder) throws Exception {
        Method getCachedDataMethod = holder.getClass().getMethod("getCachedData");
        Object cachedData = getCachedDataMethod.invoke(holder);

        Method getMetaDataMethod = cachedData.getClass().getMethod("getMetaData");
        return getMetaDataMethod.invoke(cachedData);
    }

    /**
     * Gets the prefix from a User object.
     */
    private String getPrefixFromUser(Object user) throws Exception {
        Object metaData = getMetaDataFromHolder(user);
        if (metaData != null) {
            Method getPrefixMethod = metaData.getClass().getMethod("getPrefix");
            return (String) getPrefixMethod.invoke(metaData);
        }
        return null;
    }

    /**
     * Extracts group names from a User's nodes.
     */
    @SuppressWarnings("unchecked")
    private void extractGroupsFromUser(Object user, List<String> groups) throws Exception {
        Method getNodesMethod = user.getClass().getMethod("getNodes");
        Object nodesCollection = getNodesMethod.invoke(user);

        if (nodesCollection instanceof Collection) {
            for (Object node : (Collection<?>) nodesCollection) {
                Method getTypeMethod = node.getClass().getMethod("getType");
                Object nodeType = getTypeMethod.invoke(node);

                Method nameMethod = nodeType.getClass().getMethod("name");
                String typeName = (String) nameMethod.invoke(nodeType);

                if ("INHERITANCE".equals(typeName)) {
                    Method getKeyMethod = node.getClass().getMethod("getKey");
                    String key = (String) getKeyMethod.invoke(node);
                    String groupName = key.replace("group.", "");
                    if (!groupName.isEmpty()) {
                        groups.add(groupName);
                    }
                }
            }
        }
    }

    /**
     * Finds a prefix tooltip from a User's meta data.
     */
    private String findPrefixTooltip(Object user) throws Exception {
        Object metaData = getMetaDataFromHolder(user);
        if (metaData != null) {
            Method getMetaValueMethod = metaData.getClass().getMethod("getMetaValue", String.class);

            // Look for prefix_tooltip meta keys (e.g., "prefix_tooltip.0", "prefix_tooltip.1", etc.)
            for (int i = 0; i < 10; i++) {
                String tooltipKey = "prefix_tooltip." + i;
                String tooltip = (String) getMetaValueMethod.invoke(metaData, tooltipKey);
                if (tooltip != null && !tooltip.isEmpty()) {
                    return tooltip;
                }
            }
        }
        return null;
    }
}
