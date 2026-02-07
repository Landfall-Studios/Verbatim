package world.landfall.verbatim;

import org.junit.jupiter.api.Test;
import world.landfall.verbatim.test.MockGamePlayer;
import world.landfall.verbatim.test.VerbatimTestBase;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatChannelManager.
 */
class ChatChannelManagerTest extends VerbatimTestBase {

    private ChatChannelManager.ChannelConfig createChannel(String name, String prefix, String shortcut, boolean alwaysOn) {
        return new ChatChannelManager.ChannelConfig(
            name, prefix, shortcut,
            null,  // permission
            -1,    // range (-1 = global)
            "&f",  // nameColor
            ":",   // separator
            "&f",  // separatorColor
            "&f",  // messageColor
            alwaysOn,
            false, // mature
            null,  // specialChannelType
            null   // nameStyle
        );
    }

    // === Channel Join/Leave Tests ===

    @Test
    void playerCanJoinChannel() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player = createPlayer("TestPlayer");

        boolean joined = ChatChannelManager.joinChannel(player, "global");

        assertTrue(joined);
        assertTrue(ChatChannelManager.isJoined(player, "global"));
    }

    @Test
    void playerCanLeaveChannel() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player = createPlayer("TestPlayer");
        ChatChannelManager.joinChannel(player, "global");

        ChatChannelManager.leaveChannelCmd(player, "global");

        assertFalse(ChatChannelManager.isJoined(player, "global"));
    }

    @Test
    void playerCannotLeaveAlwaysOnChannel() {
        ChatChannelManager.addChannelConfig(createChannel("ooc", "&8[OOC]", "o", true));
        MockGamePlayer player = createPlayer("TestPlayer");
        ChatChannelManager.joinChannel(player, "ooc");

        ChatChannelManager.leaveChannelCmd(player, "ooc");

        // Should still be joined because it's alwaysOn
        assertTrue(ChatChannelManager.isJoined(player, "ooc"));
    }

    @Test
    void joiningNonexistentChannelFails() {
        MockGamePlayer player = createPlayer("TestPlayer");

        boolean joined = ChatChannelManager.joinChannel(player, "nonexistent");

        assertFalse(joined);
    }

    @Test
    void getJoinedChannelsReturnsCorrectSet() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        ChatChannelManager.addChannelConfig(createChannel("local", "&a[Local]", "l", false));
        MockGamePlayer player = createPlayer("TestPlayer");

        ChatChannelManager.joinChannel(player, "global");
        ChatChannelManager.joinChannel(player, "local");

        Set<String> joined = ChatChannelManager.getJoinedChannels(player);
        assertEquals(2, joined.size());
        assertTrue(joined.contains("global"));
        assertTrue(joined.contains("local"));
    }

    // === Channel Focus Tests ===

    @Test
    void playerCanFocusChannel() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player = createPlayer("TestPlayer");

        ChatChannelManager.focusChannel(player, "global");

        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("global", focus.get().name);
    }

    @Test
    void focusingChannelAutoJoins() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player = createPlayer("TestPlayer");

        assertFalse(ChatChannelManager.isJoined(player, "global"));
        ChatChannelManager.focusChannel(player, "global");
        assertTrue(ChatChannelManager.isJoined(player, "global"));
    }

    // === Permission Tests ===

    @Test
    void playerWithPermissionCanJoinRestrictedChannel() {
        ChatChannelManager.ChannelConfig config = new ChatChannelManager.ChannelConfig(
            "staff", "&c[Staff]", "s",
            "verbatim.channel.staff",  // permission required
            -1, "&f", ":", "&f", "&f",
            false, false, null, null
        );
        ChatChannelManager.addChannelConfig(config);
        MockGamePlayer player = createPlayer("StaffMember");
        permissionService.grantPermission(player, "verbatim.channel.staff");

        boolean joined = ChatChannelManager.joinChannel(player, "staff");

        assertTrue(joined);
    }

    @Test
    void playerWithoutPermissionCannotJoinRestrictedChannel() {
        ChatChannelManager.ChannelConfig config = new ChatChannelManager.ChannelConfig(
            "staff", "&c[Staff]", "s",
            "verbatim.channel.staff",  // permission required
            -1, "&f", ":", "&f", "&f",
            false, false, null, null
        );
        ChatChannelManager.addChannelConfig(config);
        MockGamePlayer player = createPlayer("RegularPlayer");
        // No permission granted

        boolean joined = ChatChannelManager.joinChannel(player, "staff");

        assertFalse(joined);
    }

    // === Channel Config Tests ===

    @Test
    void getChannelByShortcutWorks() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));

        var config = ChatChannelManager.getChannelConfigByShortcut("g");

        assertTrue(config.isPresent());
        assertEquals("global", config.get().name);
    }

    @Test
    void getChannelByNameWorks() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));

        var config = ChatChannelManager.getChannelConfigByName("global");

        assertTrue(config.isPresent());
        assertEquals("&7[Global]", config.get().displayPrefix);
    }

    // === DM Focus Tests ===

    @Test
    void playerCanFocusDmByUUID() {
        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer target = createPlayer("Target");

        ChatChannelManager.focusDm(sender, target.getUUID());

        var focus = ChatChannelManager.getFocus(sender);
        assertTrue(focus.isPresent());
    }

    @Test
    void playerCanFocusDmByName() {
        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer target = createPlayer("Target");

        ChatChannelManager.focusDm(sender, "Target");

        var focus = ChatChannelManager.getFocus(sender);
        assertTrue(focus.isPresent());
    }

    @Test
    void focusDmToOfflinePlayerFails() {
        MockGamePlayer sender = createPlayer("Sender");

        ChatChannelManager.focusDm(sender, "OfflinePlayer");

        // Focus should remain empty when target is offline
        var focus = ChatChannelManager.getFocusedChannelConfig(sender);
        assertFalse(focus.isPresent());
    }

    @Test
    void lastIncomingDmSenderTracked() {
        MockGamePlayer recipient = createPlayer("Recipient");
        MockGamePlayer sender = createPlayer("Sender");

        ChatChannelManager.setLastIncomingDmSender(recipient, sender.getUUID());

        var lastSender = ChatChannelManager.getLastIncomingDmSender(recipient);
        assertTrue(lastSender.isPresent());
        assertEquals(sender.getUUID(), lastSender.get());
    }

    // === Admin Functions ===

    @Test
    void adminCanKickPlayerFromChannel() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player = createPlayer("Player");
        MockGamePlayer admin = createPlayer("Admin");
        ChatChannelManager.joinChannel(player, "global");

        boolean kicked = ChatChannelManager.adminKickPlayerFromChannel(player, "global", admin);

        assertTrue(kicked);
        assertFalse(ChatChannelManager.isJoined(player, "global"));
    }

    @Test
    void cannotKickFromAlwaysOnChannel() {
        ChatChannelManager.addChannelConfig(createChannel("ooc", "&8[OOC]", "o", true));
        MockGamePlayer player = createPlayer("Player");
        MockGamePlayer admin = createPlayer("Admin");
        ChatChannelManager.joinChannel(player, "ooc");

        boolean kicked = ChatChannelManager.adminKickPlayerFromChannel(player, "ooc", admin);

        assertFalse(kicked);
        assertTrue(ChatChannelManager.isJoined(player, "ooc"));
    }

    @Test
    void cannotKickPlayerNotInChannel() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player = createPlayer("Player");
        MockGamePlayer admin = createPlayer("Admin");
        // Player never joined

        boolean kicked = ChatChannelManager.adminKickPlayerFromChannel(player, "global", admin);

        assertFalse(kicked);
    }

    // === Get Players In Channel ===

    @Test
    void getPlayersInChannelReturnsJoinedPlayers() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", false));
        MockGamePlayer player1 = createPlayer("Player1");
        MockGamePlayer player2 = createPlayer("Player2");
        MockGamePlayer player3 = createPlayer("Player3");

        ChatChannelManager.joinChannel(player1, "global");
        ChatChannelManager.joinChannel(player2, "global");
        // player3 doesn't join

        var playersInChannel = ChatChannelManager.getPlayersInChannel("global");

        assertEquals(2, playersInChannel.size());
        assertTrue(playersInChannel.contains(player1));
        assertTrue(playersInChannel.contains(player2));
        assertFalse(playersInChannel.contains(player3));
    }

    @Test
    void getPlayersInNonexistentChannelReturnsEmpty() {
        var players = ChatChannelManager.getPlayersInChannel("nonexistent");

        assertTrue(players.isEmpty());
    }

    // === Channel Config Properties ===

    @Test
    void channelConfigParsesAllProperties() {
        ChatChannelManager.ChannelConfig config = new ChatChannelManager.ChannelConfig(
            "test", "&a[Test]", "t",
            "verbatim.test",
            100,           // range
            "&b",          // nameColor
            " says: ",     // separator
            "&7",          // separatorColor
            "&f",          // messageColor
            false,
            true,          // mature
            "local",       // specialChannelType
            "nickname"     // nameStyle
        );

        assertEquals("test", config.name);
        assertEquals("&a[Test]", config.displayPrefix);
        assertEquals("t", config.shortcut);
        assertTrue(config.permission.isPresent());
        assertEquals("verbatim.test", config.permission.get());
        assertEquals(100, config.range);
        assertEquals("&b", config.nameColor);
        assertEquals(" says: ", config.separator);
        assertEquals("&7", config.separatorColor);
        assertEquals("&f", config.messageColor);
        assertFalse(config.alwaysOn);
        assertTrue(config.mature);
        assertTrue(config.specialChannelType.isPresent());
        assertEquals("local", config.specialChannelType.get());
        assertEquals(NameStyle.NICKNAME, config.nameStyle);
    }

    @Test
    void alwaysOnChannelIgnoresPermission() {
        ChatChannelManager.ChannelConfig config = new ChatChannelManager.ChannelConfig(
            "ooc", "&8[OOC]", "o",
            "verbatim.ooc",  // permission specified but should be ignored
            -1, "&f", ":", "&f", "&f",
            true,  // alwaysOn
            false, null, null
        );

        // Permission should be empty when alwaysOn is true
        assertFalse(config.permission.isPresent());
    }
}
