package world.landfall.verbatim.test;

import world.landfall.verbatim.NameStyle;
import world.landfall.verbatim.context.ChatFormatter;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;

/**
 * Mock ChatFormatter for unit testing.
 * Returns simple text components without actual color parsing.
 */
public class MockChatFormatter implements ChatFormatter {

    @Override
    public GameComponent parseColors(String text) {
        // Strip color codes for testing (& followed by any char)
        String stripped = text.replaceAll("&[0-9a-fk-or]", "");
        return new MockGameComponent(stripped);
    }

    @Override
    public GameComponent parseColorsWithPermissions(String text, GamePlayer player) {
        return parseColors(text);
    }

    @Override
    public GameComponent parsePlayerInputWithPermissions(String channelBaseColor, String playerInput, GamePlayer player) {
        return new MockGameComponent(playerInput);
    }

    @Override
    public GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle) {
        return new MockGameComponent(player.getDisplayName());
    }

    @Override
    public GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM) {
        return new MockGameComponent(player.getDisplayName());
    }

    @Override
    public String createDiscordPlayerName(GamePlayer player, NameStyle nameStyle) {
        return player.getDisplayName();
    }

    @Override
    public String createDiscordPlayerName(GamePlayer player) {
        return player.getDisplayName();
    }

    @Override
    public GameComponent makeLinksClickable(String text, GameComponent baseStyleComponent) {
        return new MockGameComponent(text);
    }
}
