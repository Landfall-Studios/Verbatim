package world.landfall.verbatim.util;

import world.landfall.verbatim.context.GamePlayer;

import java.util.Collections;
import java.util.List;

/**
 * Abstract prefix service that platform implementations extend.
 * Provides player prefix/group information from permission systems.
 */
public abstract class PrefixService {

    /**
     * Returns whether a prefix system is available.
     */
    public abstract boolean isPrefixSystemAvailable();

    /**
     * Get the display prefix for a player.
     * @return The prefix string, or empty string if none
     */
    public abstract String getPlayerPrefix(GamePlayer player);

    /**
     * Get the player's primary group name.
     * @return The group name, or empty string if none
     */
    public abstract String getPlayerPrimaryGroup(GamePlayer player);

    /**
     * Get all groups the player belongs to.
     * @return List of group names, or empty list if none
     */
    public abstract List<String> getPlayerGroups(GamePlayer player);

    /**
     * Get the display name for a group.
     * @return The display name, or the group name itself if no display name
     */
    public abstract String getGroupDisplayName(String groupName);

    /**
     * Get a tooltip for the player's prefix.
     * @return The tooltip string, or null if none
     */
    public abstract String getPrefixTooltip(GamePlayer player);

    /**
     * Default implementation that returns no prefix info.
     * Used when no prefix system is available.
     */
    public static class NoPrefixService extends PrefixService {
        @Override
        public boolean isPrefixSystemAvailable() {
            return false;
        }

        @Override
        public String getPlayerPrefix(GamePlayer player) {
            return "";
        }

        @Override
        public String getPlayerPrimaryGroup(GamePlayer player) {
            return "";
        }

        @Override
        public List<String> getPlayerGroups(GamePlayer player) {
            return Collections.emptyList();
        }

        @Override
        public String getGroupDisplayName(String groupName) {
            return groupName != null ? groupName : "";
        }

        @Override
        public String getPrefixTooltip(GamePlayer player) {
            return null;
        }
    }
}
