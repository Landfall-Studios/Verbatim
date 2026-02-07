package world.landfall.verbatim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NameStyle enum.
 */
class NameStyleTest {

    @Test
    void fromConfigValueParsesDisplayName() {
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue("displayName"));
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue("DISPLAYNAME"));
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue("DisplayName"));
    }

    @Test
    void fromConfigValueParsesUsername() {
        assertEquals(NameStyle.USERNAME, NameStyle.fromConfigValue("username"));
        assertEquals(NameStyle.USERNAME, NameStyle.fromConfigValue("USERNAME"));
        assertEquals(NameStyle.USERNAME, NameStyle.fromConfigValue("Username"));
    }

    @Test
    void fromConfigValueParsesNickname() {
        assertEquals(NameStyle.NICKNAME, NameStyle.fromConfigValue("nickname"));
        assertEquals(NameStyle.NICKNAME, NameStyle.fromConfigValue("NICKNAME"));
        assertEquals(NameStyle.NICKNAME, NameStyle.fromConfigValue("Nickname"));
    }

    @Test
    void fromConfigValueReturnsDefaultForNull() {
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue(null));
    }

    @Test
    void fromConfigValueReturnsDefaultForEmpty() {
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue(""));
    }

    @Test
    void fromConfigValueReturnsDefaultForInvalid() {
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue("invalid"));
        assertEquals(NameStyle.DISPLAY_NAME, NameStyle.fromConfigValue("unknown"));
    }

    @Test
    void getConfigValueReturnsCorrectString() {
        assertEquals("displayName", NameStyle.DISPLAY_NAME.getConfigValue());
        assertEquals("username", NameStyle.USERNAME.getConfigValue());
        assertEquals("nickname", NameStyle.NICKNAME.getConfigValue());
    }
}
