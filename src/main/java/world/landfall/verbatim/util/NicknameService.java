package world.landfall.verbatim.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.ChatFormattingUtils;
import world.landfall.verbatim.Verbatim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NicknameService {

    // Permission node for using /nick command
    public static final String PERM_NICK = "verbatim.nick";

    // NBT key for storing nickname data
    private static final String NBT_NICKNAME_KEY = "verbatim_nickname";

    // Runtime cache for quick access
    private static final Map<UUID, String> nicknameCache = new HashMap<>();

    /**
     * Sets a player's nickname with color code support and permission checks.
     *
     * @param player The player setting the nickname
     * @param nickname The nickname with potential color codes (null to clear)
     * @return The processed nickname that was actually set, or null if cleared
     */
    public static String setNickname(ServerPlayer player, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            // Clear nickname
            clearNickname(player);
            return null;
        }

        // Process color codes based on permissions
        String processedNickname;
        if (Verbatim.permissionService.hasPermission(player, ChatFormattingUtils.PERM_CHAT_COLOR, 2) ||
            Verbatim.permissionService.hasPermission(player, ChatFormattingUtils.PERM_CHAT_FORMAT, 2)) {
            // Player has formatting permissions, keep the raw nickname with codes
            processedNickname = nickname;
        } else {
            // Strip all formatting codes if no permissions
            processedNickname = ChatFormattingUtils.stripFormattingCodes(nickname);
        }

        // Store in player's persistent data
        CompoundTag persistentData = player.getPersistentData();
        persistentData.putString(NBT_NICKNAME_KEY, processedNickname);

        // Update cache
        nicknameCache.put(player.getUUID(), processedNickname);

        Verbatim.LOGGER.debug("Set nickname for player {} to: {}", player.getName().getString(), processedNickname);
        return processedNickname;
    }

    /**
     * Gets a player's current nickname.
     *
     * @param player The player to get the nickname for
     * @return The player's nickname, or null if not set
     */
    public static String getNickname(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Check cache first
        if (nicknameCache.containsKey(playerId)) {
            return nicknameCache.get(playerId);
        }

        // Load from persistent data
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(NBT_NICKNAME_KEY)) {
            String nickname = persistentData.getString(NBT_NICKNAME_KEY);
            nicknameCache.put(playerId, nickname);
            return nickname;
        }

        return null;
    }

    /**
     * Clears a player's nickname.
     *
     * @param player The player to clear the nickname for
     */
    public static void clearNickname(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Remove from persistent data
        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(NBT_NICKNAME_KEY);

        // Remove from cache
        nicknameCache.remove(playerId);

        Verbatim.LOGGER.debug("Cleared nickname for player {}", player.getName().getString());
    }

    /**
     * Checks if a player has a nickname set.
     *
     * @param player The player to check
     * @return true if the player has a nickname set
     */
    public static boolean hasNickname(ServerPlayer player) {
        return getNickname(player) != null;
    }

    /**
     * Called when a player logs out to clean up cache.
     *
     * @param playerId The UUID of the player logging out
     */
    public static void onPlayerLogout(UUID playerId) {
        nicknameCache.remove(playerId);
    }

    /**
     * Gets the appropriate name for a player based on the specified NameStyle.
     *
     * @param player The player to get the name for
     * @param nameStyle The style of name to retrieve
     * @return The name string according to the specified style
     */
    public static String getNameForStyle(ServerPlayer player, world.landfall.verbatim.NameStyle nameStyle) {
        switch (nameStyle) {
            case USERNAME:
                return player.getName().getString();

            case NICKNAME:
                String nickname = getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                }
                // Fall back to username if no nickname is set
                return player.getName().getString();

            case DISPLAY_NAME:
            default:
                return ChatFormattingUtils.stripFormattingCodes(player.getDisplayName().getString());
        }
    }

    /**
     * Gets the raw name (without processing color codes) for a player based on the specified NameStyle.
     * Used for situations where we need the raw text with color codes intact.
     *
     * @param player The player to get the name for
     * @param nameStyle The style of name to retrieve
     * @return The raw name string according to the specified style
     */
    public static String getRawNameForStyle(ServerPlayer player, world.landfall.verbatim.NameStyle nameStyle) {
        switch (nameStyle) {
            case USERNAME:
                return player.getName().getString();

            case NICKNAME:
                String nickname = getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                }
                // Fall back to username if no nickname is set
                return player.getName().getString();

            case DISPLAY_NAME:
            default:
                return player.getDisplayName().getString();
        }
    }
}