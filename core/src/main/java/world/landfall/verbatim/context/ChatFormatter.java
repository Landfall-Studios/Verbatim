package world.landfall.verbatim.context;

import world.landfall.verbatim.NameStyle;

/**
 * Platform-independent interface for chat formatting operations.
 */
public interface ChatFormatter {
    GameComponent parseColors(String text);
    GameComponent parseColorsWithPermissions(String text, GamePlayer player);
    GameComponent parsePlayerInputWithPermissions(String channelBaseColor, String playerInput, GamePlayer player);
    GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle);
    GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM);
    GameComponent createFavoriteNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle, int gradientStartRgb, int gradientEndRgb);
    String createDiscordPlayerName(GamePlayer player, NameStyle nameStyle);
    String createDiscordPlayerName(GamePlayer player);
    GameComponent makeLinksClickable(String text, GameComponent baseStyleComponent);
}
