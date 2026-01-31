package world.landfall.verbatim.util;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;

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

    public String getPlayerPrefix(GamePlayer player) {
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
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        String prefix = getPrefixFromUser(loadedUser);
                        return prefix != null ? prefix : "";
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for prefix: {}",
                                        player.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting prefix for player '{}': {}",
                                player.getUsername(), e.getMessage());
        }

        return "";
    }

    public String getPlayerPrimaryGroup(GamePlayer player) {
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
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        Method getPrimaryGroupMethod = loadedUser.getClass().getMethod("getPrimaryGroup");
                        String primaryGroup = (String) getPrimaryGroupMethod.invoke(loadedUser);
                        return primaryGroup != null ? primaryGroup : "";
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for primary group: {}",
                                        player.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting primary group for player '{}': {}",
                                player.getUsername(), e.getMessage());
        }

        return "";
    }

    public List<String> getPlayerGroups(GamePlayer player) {
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
                try {
                    Object loadedUser = loadUser(player);
                    if (loadedUser != null) {
                        extractGroupsFromUser(loadedUser, groups);
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for groups: {}",
                                        player.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting groups for player '{}': {}",
                                player.getUsername(), e.getMessage());
        }

        return groups;
    }

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

    public String getPrefixTooltip(GamePlayer player) {
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
                                        player.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting prefix tooltip for player '{}': {}",
                                player.getUsername(), e.getMessage());
        }

        return null;
    }

    // ===== Helper methods for reflection-based LuckPerms access =====

    private Object getUser(GamePlayer player) throws Exception {
        Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
        Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

        Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
        return getUserMethod.invoke(userManager, player.getUUID());
    }

    private Object loadUser(GamePlayer player) throws Exception {
        Method getUserManagerMethod = this.luckPermsApi.getClass().getMethod("getUserManager");
        Object userManager = getUserManagerMethod.invoke(this.luckPermsApi);

        Method loadUserMethod = userManager.getClass().getMethod("loadUser", UUID.class);
        Object completableFuture = loadUserMethod.invoke(userManager, player.getUUID());

        Method getMethod = completableFuture.getClass().getMethod("get");
        return getMethod.invoke(completableFuture);
    }

    private Object getMetaDataFromHolder(Object holder) throws Exception {
        Method getCachedDataMethod = holder.getClass().getMethod("getCachedData");
        Object cachedData = getCachedDataMethod.invoke(holder);

        Method getMetaDataMethod = cachedData.getClass().getMethod("getMetaData");
        return getMetaDataMethod.invoke(cachedData);
    }

    private String getPrefixFromUser(Object user) throws Exception {
        Object metaData = getMetaDataFromHolder(user);
        if (metaData != null) {
            Method getPrefixMethod = metaData.getClass().getMethod("getPrefix");
            return (String) getPrefixMethod.invoke(metaData);
        }
        return null;
    }

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

    private String findPrefixTooltip(Object user) throws Exception {
        Object metaData = getMetaDataFromHolder(user);
        if (metaData != null) {
            Method getMetaValueMethod = metaData.getClass().getMethod("getMetaValue", String.class);

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
