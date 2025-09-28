package world.landfall.verbatim.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.ChatFormattingUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class PrefixService {
    private LuckPerms luckPermsApi;
    private Boolean luckPermsAvailable;

    public PrefixService() {
        this.luckPermsApi = null;
        this.luckPermsAvailable = null;
        Verbatim.LOGGER.info("[Verbatim PrefixService] PrefixService initialized. LuckPerms availability will be checked when first needed.");
    }

    /**
     * Lazy-load LuckPerms API. This is called on first prefix check.
     */
    private void ensureLuckPermsChecked() {
        if (this.luckPermsAvailable == null) {
            try {
                this.luckPermsApi = LuckPermsProvider.get();
                this.luckPermsAvailable = true;
                Verbatim.LOGGER.info("[Verbatim PrefixService] LuckPerms API found and loaded. Prefixes will be handled by LuckPerms.");
            } catch (IllegalStateException e) {
                this.luckPermsApi = null;
                this.luckPermsAvailable = false;
                Verbatim.LOGGER.info("[Verbatim PrefixService] LuckPerms API not found. Prefixes will not be displayed.");
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
            User user = this.luckPermsApi.getUserManager().getUser(player.getUUID());
            if (user != null) {
                CachedMetaData metaData = user.getCachedData().getMetaData();
                String prefix = metaData.getPrefix();
                return prefix != null ? prefix : "";
            } else {
                // Try to load the user synchronously
                try {
                    User loadedUser = this.luckPermsApi.getUserManager().loadUser(player.getUUID()).get();
                    if (loadedUser != null) {
                        CachedMetaData metaData = loadedUser.getCachedData().getMetaData();
                        String prefix = metaData.getPrefix();
                        return prefix != null ? prefix : "";
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for prefix: {}", 
                                        player.getName().getString(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting prefix for player '{}': {}", 
                                player.getName().getString(), e.getMessage());
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
            User user = this.luckPermsApi.getUserManager().getUser(player.getUUID());
            if (user != null) {
                String primaryGroup = user.getPrimaryGroup();
                return primaryGroup != null ? primaryGroup : "";
            } else {
                // Try to load the user synchronously
                try {
                    User loadedUser = this.luckPermsApi.getUserManager().loadUser(player.getUUID()).get();
                    if (loadedUser != null) {
                        String primaryGroup = loadedUser.getPrimaryGroup();
                        return primaryGroup != null ? primaryGroup : "";
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for primary group: {}", 
                                        player.getName().getString(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting primary group for player '{}': {}", 
                                player.getName().getString(), e.getMessage());
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
            User user = this.luckPermsApi.getUserManager().getUser(player.getUUID());
            if (user != null) {
                user.getNodes().stream()
                    .filter(node -> node.getType().name().equals("INHERITANCE"))
                    .forEach(node -> {
                        String groupName = node.getKey().replace("group.", "");
                        if (!groupName.isEmpty()) {
                            groups.add(groupName);
                        }
                    });
            } else {
                // Try to load the user synchronously
                try {
                    User loadedUser = this.luckPermsApi.getUserManager().loadUser(player.getUUID()).get();
                    if (loadedUser != null) {
                        loadedUser.getNodes().stream()
                            .filter(node -> node.getType().name().equals("INHERITANCE"))
                            .forEach(node -> {
                                String groupName = node.getKey().replace("group.", "");
                                if (!groupName.isEmpty()) {
                                    groups.add(groupName);
                                }
                            });
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for groups: {}", 
                                        player.getName().getString(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting groups for player '{}': {}", 
                                player.getName().getString(), e.getMessage());
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
            Group group = this.luckPermsApi.getGroupManager().getGroup(groupName);
            if (group != null) {
                CachedMetaData metaData = group.getCachedData().getMetaData();
                String displayName = metaData.getMetaValue("displayname");
                return displayName != null ? displayName : groupName;
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
            User user = this.luckPermsApi.getUserManager().getUser(player.getUUID());
            if (user != null) {
                CachedMetaData metaData = user.getCachedData().getMetaData();
                
                // Look for prefix_tooltip meta keys (e.g., "prefix_tooltip.0", "prefix_tooltip.1", etc.)
                for (int i = 0; i < 10; i++) { // Check first 10 possible tooltip entries
                    String tooltipKey = "prefix_tooltip." + i;
                    String tooltip = metaData.getMetaValue(tooltipKey);
                    if (tooltip != null && !tooltip.isEmpty()) {
                        return tooltip;
                    }
                }
            } else {
                // Try to load the user synchronously
                try {
                    User loadedUser = this.luckPermsApi.getUserManager().loadUser(player.getUUID()).get();
                    if (loadedUser != null) {
                        CachedMetaData metaData = loadedUser.getCachedData().getMetaData();
                        
                        // Look for prefix_tooltip meta keys
                        for (int i = 0; i < 10; i++) {
                            String tooltipKey = "prefix_tooltip." + i;
                            String tooltip = metaData.getMetaValue(tooltipKey);
                            if (tooltip != null && !tooltip.isEmpty()) {
                                return tooltip;
                            }
                        }
                    }
                } catch (Exception e) {
                    Verbatim.LOGGER.debug("[Verbatim PrefixService] Failed to load user '{}' for prefix tooltip: {}", 
                                        player.getName().getString(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.debug("[Verbatim PrefixService] Error getting prefix tooltip for player '{}': {}", 
                                player.getName().getString(), e.getMessage());
        }

        return null;
    }


}