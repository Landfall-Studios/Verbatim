package world.landfall.verbatim.util;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.NameStyle;
import world.landfall.verbatim.context.GamePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NicknameService {

    public static final String PERM_NICK = "verbatim.nick";

    private static final String NBT_NICKNAME_KEY = "verbatim_nickname";

    // Permission constants (referenced from ChatFormatter)
    public static final String PERM_CHAT_COLOR = "verbatim.chatcolor";
    public static final String PERM_CHAT_FORMAT = "verbatim.chatformat";

    private static final Map<UUID, String> nicknameCache = new HashMap<>();

    public static String setNickname(GamePlayer player, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            clearNickname(player);
            return null;
        }

        String processedNickname;
        if (Verbatim.permissionService.hasPermission(player, PERM_CHAT_COLOR, 2) ||
            Verbatim.permissionService.hasPermission(player, PERM_CHAT_FORMAT, 2)) {
            processedNickname = nickname;
        } else {
            processedNickname = FormattingCodeUtils.stripFormattingCodes(nickname);
        }

        Verbatim.gameContext.setPlayerStringData(player, NBT_NICKNAME_KEY, processedNickname);

        nicknameCache.put(player.getUUID(), processedNickname);

        Verbatim.LOGGER.debug("Set nickname for player {} to: {}", player.getUsername(), processedNickname);
        return processedNickname;
    }

    public static String getNickname(GamePlayer player) {
        UUID playerId = player.getUUID();

        if (nicknameCache.containsKey(playerId)) {
            return nicknameCache.get(playerId);
        }

        if (Verbatim.gameContext.hasPlayerData(player, NBT_NICKNAME_KEY)) {
            String nickname = Verbatim.gameContext.getPlayerStringData(player, NBT_NICKNAME_KEY);
            nicknameCache.put(playerId, nickname);
            return nickname;
        }

        return null;
    }

    public static void clearNickname(GamePlayer player) {
        UUID playerId = player.getUUID();

        Verbatim.gameContext.removePlayerData(player, NBT_NICKNAME_KEY);

        nicknameCache.remove(playerId);

        Verbatim.LOGGER.debug("Cleared nickname for player {}", player.getUsername());
    }

    public static boolean hasNickname(GamePlayer player) {
        return getNickname(player) != null;
    }

    public static void onPlayerLogout(UUID playerId) {
        nicknameCache.remove(playerId);
    }

    public static String getNameForStyle(GamePlayer player, NameStyle nameStyle) {
        switch (nameStyle) {
            case USERNAME:
                return player.getUsername();

            case NICKNAME:
                String nickname = getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                }
                return player.getUsername();

            case DISPLAY_NAME:
            default:
                return FormattingCodeUtils.stripFormattingCodes(player.getDisplayName());
        }
    }

    public static String getRawNameForStyle(GamePlayer player, NameStyle nameStyle) {
        switch (nameStyle) {
            case USERNAME:
                return player.getUsername();

            case NICKNAME:
                String nickname = getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                }
                return player.getUsername();

            case DISPLAY_NAME:
            default:
                return player.getDisplayName();
        }
    }
}
