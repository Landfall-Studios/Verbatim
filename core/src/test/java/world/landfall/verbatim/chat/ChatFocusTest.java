package world.landfall.verbatim.chat;

import org.junit.jupiter.api.Test;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.test.MockGamePlayer;
import world.landfall.verbatim.test.VerbatimTestBase;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatFocus.
 */
class ChatFocusTest extends VerbatimTestBase {

    private ChatChannelManager.ChannelConfig createChannel(String name, String prefix, String shortcut) {
        return new ChatChannelManager.ChannelConfig(
            name, prefix, shortcut,
            null, -1, "&f", ":", "&f", "&f",
            false, false, null, null
        );
    }

    // === Channel Focus Tests ===

    @Test
    void createChannelFocusSetsCorrectType() {
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        assertEquals(ChatFocus.FocusType.CHANNEL, focus.getType());
    }

    @Test
    void channelFocusReturnsChannelName() {
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        assertEquals("global", focus.getChannelName());
    }

    @Test
    void channelFocusReturnsNullForTargetPlayerId() {
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        assertNull(focus.getTargetPlayerId());
    }

    @Test
    void channelFocusIsValidWhenChannelExists() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g"));
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        assertTrue(focus.isValid());
    }

    @Test
    void channelFocusIsInvalidWhenChannelDoesNotExist() {
        ChatFocus focus = ChatFocus.createChannelFocus("nonexistent");

        assertFalse(focus.isValid());
    }

    @Test
    void channelFocusDisplayNameIncludesPrefix() {
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g"));
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        String displayName = focus.getDisplayName();

        assertTrue(displayName.contains("&7[Global]"));
        assertTrue(displayName.contains("global"));
    }

    // === DM Focus Tests ===

    @Test
    void createDmFocusSetsCorrectType() {
        UUID targetId = UUID.randomUUID();
        ChatFocus focus = ChatFocus.createDmFocus(targetId);

        assertEquals(ChatFocus.FocusType.DM, focus.getType());
    }

    @Test
    void dmFocusReturnsTargetPlayerId() {
        UUID targetId = UUID.randomUUID();
        ChatFocus focus = ChatFocus.createDmFocus(targetId);

        assertEquals(targetId, focus.getTargetPlayerId());
    }

    @Test
    void dmFocusReturnsNullForChannelName() {
        UUID targetId = UUID.randomUUID();
        ChatFocus focus = ChatFocus.createDmFocus(targetId);

        assertNull(focus.getChannelName());
    }

    @Test
    void dmFocusIsValidWhenPlayerIsOnline() {
        MockGamePlayer target = createPlayer("Target");
        ChatFocus focus = ChatFocus.createDmFocus(target.getUUID());

        assertTrue(focus.isValid());
    }

    @Test
    void dmFocusIsInvalidWhenPlayerIsOffline() {
        UUID offlinePlayerId = UUID.randomUUID();
        ChatFocus focus = ChatFocus.createDmFocus(offlinePlayerId);

        assertFalse(focus.isValid());
    }

    @Test
    void dmFocusDisplayNameIncludesPlayerName() {
        MockGamePlayer target = createPlayer("TargetPlayer");
        ChatFocus focus = ChatFocus.createDmFocus(target.getUUID());

        String displayName = focus.getDisplayName();

        assertTrue(displayName.contains("DM with"));
        assertTrue(displayName.contains("TargetPlayer"));
    }

    @Test
    void dmFocusDisplayNameHandlesOfflinePlayer() {
        UUID offlinePlayerId = UUID.randomUUID();
        ChatFocus focus = ChatFocus.createDmFocus(offlinePlayerId);

        String displayName = focus.getDisplayName();

        assertTrue(displayName.contains("offline"));
    }

    // === Equality Tests ===

    @Test
    void channelFocusEqualsWithSameChannel() {
        ChatFocus focus1 = ChatFocus.createChannelFocus("global");
        ChatFocus focus2 = ChatFocus.createChannelFocus("global");

        assertEquals(focus1, focus2);
        assertEquals(focus1.hashCode(), focus2.hashCode());
    }

    @Test
    void channelFocusNotEqualsWithDifferentChannel() {
        ChatFocus focus1 = ChatFocus.createChannelFocus("global");
        ChatFocus focus2 = ChatFocus.createChannelFocus("local");

        assertNotEquals(focus1, focus2);
    }

    @Test
    void dmFocusEqualsWithSameTarget() {
        UUID targetId = UUID.randomUUID();
        ChatFocus focus1 = ChatFocus.createDmFocus(targetId);
        ChatFocus focus2 = ChatFocus.createDmFocus(targetId);

        assertEquals(focus1, focus2);
        assertEquals(focus1.hashCode(), focus2.hashCode());
    }

    @Test
    void dmFocusNotEqualsWithDifferentTarget() {
        ChatFocus focus1 = ChatFocus.createDmFocus(UUID.randomUUID());
        ChatFocus focus2 = ChatFocus.createDmFocus(UUID.randomUUID());

        assertNotEquals(focus1, focus2);
    }

    @Test
    void channelFocusNotEqualsDmFocus() {
        ChatFocus channelFocus = ChatFocus.createChannelFocus("global");
        ChatFocus dmFocus = ChatFocus.createDmFocus(UUID.randomUUID());

        assertNotEquals(channelFocus, dmFocus);
    }

    @Test
    void focusNotEqualsNull() {
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        assertNotEquals(focus, null);
    }

    @Test
    void focusNotEqualsOtherType() {
        ChatFocus focus = ChatFocus.createChannelFocus("global");

        assertNotEquals(focus, "global");
    }
}
