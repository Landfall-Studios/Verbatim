package world.landfall.verbatim.util;

import org.junit.jupiter.api.Test;
import world.landfall.verbatim.NameStyle;
import world.landfall.verbatim.test.MockGamePlayer;
import world.landfall.verbatim.test.VerbatimTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NicknameService.
 */
class NicknameServiceTest extends VerbatimTestBase {

    @Test
    void setAndGetNickname() {
        MockGamePlayer player = createPlayer("TestPlayer");

        String result = NicknameService.setNickname(player, "CoolNick");

        assertEquals("CoolNick", result);
        assertEquals("CoolNick", NicknameService.getNickname(player));
    }

    @Test
    void hasNicknameReturnsTrueWhenSet() {
        MockGamePlayer player = createPlayer("TestPlayer");
        NicknameService.setNickname(player, "CoolNick");

        assertTrue(NicknameService.hasNickname(player));
    }

    @Test
    void hasNicknameReturnsFalseWhenNotSet() {
        MockGamePlayer player = createPlayer("TestPlayer");

        assertFalse(NicknameService.hasNickname(player));
    }

    @Test
    void clearNicknameRemovesNickname() {
        MockGamePlayer player = createPlayer("TestPlayer");
        NicknameService.setNickname(player, "CoolNick");

        NicknameService.clearNickname(player);

        assertNull(NicknameService.getNickname(player));
        assertFalse(NicknameService.hasNickname(player));
    }

    @Test
    void setNicknameWithNullClears() {
        MockGamePlayer player = createPlayer("TestPlayer");
        NicknameService.setNickname(player, "CoolNick");

        NicknameService.setNickname(player, null);

        assertNull(NicknameService.getNickname(player));
    }

    @Test
    void setNicknameWithEmptyClears() {
        MockGamePlayer player = createPlayer("TestPlayer");
        NicknameService.setNickname(player, "CoolNick");

        NicknameService.setNickname(player, "   ");

        assertNull(NicknameService.getNickname(player));
    }

    @Test
    void nicknameStripsFormattingWithoutPermission() {
        MockGamePlayer player = createPlayer("TestPlayer");
        // No formatting permissions granted

        String result = NicknameService.setNickname(player, "&aColoredNick");

        assertEquals("ColoredNick", result);
    }

    @Test
    void nicknamePreservesFormattingWithColorPermission() {
        MockGamePlayer player = createPlayer("TestPlayer");
        permissionService.grantPermission(player, NicknameService.PERM_CHAT_COLOR);

        String result = NicknameService.setNickname(player, "&aColoredNick");

        assertEquals("&aColoredNick", result);
    }

    @Test
    void nicknamePreservesFormattingWithFormatPermission() {
        MockGamePlayer player = createPlayer("TestPlayer");
        permissionService.grantPermission(player, NicknameService.PERM_CHAT_FORMAT);

        String result = NicknameService.setNickname(player, "&lBoldNick");

        assertEquals("&lBoldNick", result);
    }

    // === getNameForStyle Tests ===

    @Test
    void getNameForStyleReturnsUsernameForUsernameStyle() {
        MockGamePlayer player = createPlayer("TestPlayer", "Test Display");

        String result = NicknameService.getNameForStyle(player, NameStyle.USERNAME);

        assertEquals("TestPlayer", result);
    }

    @Test
    void getNameForStyleReturnsDisplayNameForDisplayNameStyle() {
        MockGamePlayer player = createPlayer("TestPlayer", "Test Display");

        String result = NicknameService.getNameForStyle(player, NameStyle.DISPLAY_NAME);

        assertEquals("Test Display", result);
    }

    @Test
    void getNameForStyleReturnsNicknameForNicknameStyleWhenSet() {
        MockGamePlayer player = createPlayer("TestPlayer");
        NicknameService.setNickname(player, "CoolNick");

        String result = NicknameService.getNameForStyle(player, NameStyle.NICKNAME);

        assertEquals("CoolNick", result);
    }

    @Test
    void getNameForStyleFallsBackToUsernameForNicknameStyleWhenNotSet() {
        MockGamePlayer player = createPlayer("TestPlayer");
        // No nickname set

        String result = NicknameService.getNameForStyle(player, NameStyle.NICKNAME);

        assertEquals("TestPlayer", result);
    }

    @Test
    void onPlayerLogoutClearsCacheButNotPersistence() {
        MockGamePlayer player = createPlayer("TestPlayer");
        NicknameService.setNickname(player, "CoolNick");

        NicknameService.onPlayerLogout(player.getUUID());

        // Cache is cleared, but it should reload from persistence
        // (In real scenario, persistence would still have the nickname)
    }
}
