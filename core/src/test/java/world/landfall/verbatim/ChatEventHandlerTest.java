package world.landfall.verbatim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import world.landfall.verbatim.chat.ChatFocus;
import world.landfall.verbatim.test.MockGamePlayer;
import world.landfall.verbatim.test.VerbatimTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatEventHandler.
 */
class ChatEventHandlerTest extends VerbatimTestBase {

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

    @BeforeEach
    public void setUpChannels() {
        // Set up some default channels for testing
        ChatChannelManager.addChannelConfig(createChannel("global", "&7[Global]", "g", true));
        ChatChannelManager.addChannelConfig(createChannel("local", "&a[Local]", "l", false));
        ChatChannelManager.addChannelConfig(createChannel("staff", "&c[Staff]", "s", false));
        gameConfig.setDefaultChannelName("global");
    }

    // === Message Routing Without Shortcuts ===

    @Test
    void chatMessageRoutesToFocusedChannel() {
        MockGamePlayer player = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");
        ChatChannelManager.joinChannel(player, "global");
        ChatChannelManager.joinChannel(recipient, "global");
        ChatChannelManager.focusChannel(player, "global");

        ChatEventHandler.onChat(player, "Hello world!");

        // Both sender and recipient should have received the message
        assertTrue(gameContext.getSentMessages().size() >= 1);
    }

    @Test
    void chatMessageWithNoFocusUsesDefaultChannel() {
        MockGamePlayer player = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");
        ChatChannelManager.joinChannel(recipient, "global");
        // Player has no focus set

        ChatEventHandler.onChat(player, "Hello world!");

        // Should have been focused to default and message sent
        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("global", focus.get().name);
    }

    // === Channel Shortcut Detection ===

    @Test
    void colonShortcutSwitchesFocusAndSendsMessage() {
        MockGamePlayer player = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");
        ChatChannelManager.joinChannel(player, "global");
        ChatChannelManager.joinChannel(player, "local");
        ChatChannelManager.joinChannel(recipient, "local");
        ChatChannelManager.focusChannel(player, "global");

        // Use l: shortcut to send to local
        ChatEventHandler.onChat(player, "l:Hello local!");

        // Focus should now be on local
        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("local", focus.get().name);
    }

    @Test
    void semicolonShortcutAlsoWorks() {
        MockGamePlayer player = createPlayer("Sender");
        ChatChannelManager.joinChannel(player, "global");
        ChatChannelManager.joinChannel(player, "local");
        ChatChannelManager.focusChannel(player, "global");

        // Use l; shortcut
        ChatEventHandler.onChat(player, "l;Hello local!");

        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("local", focus.get().name);
    }

    @Test
    void gShortcutUsesDefaultChannel() {
        MockGamePlayer player = createPlayer("Sender");
        ChatChannelManager.joinChannel(player, "local");
        ChatChannelManager.focusChannel(player, "local");

        // Use g: shortcut to go back to global (default)
        ChatEventHandler.onChat(player, "g:Hello global!");

        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("global", focus.get().name);
    }

    @Test
    void emptyMessageAfterShortcutOnlySwitchesFocus() {
        MockGamePlayer player = createPlayer("Sender");
        ChatChannelManager.joinChannel(player, "global");
        ChatChannelManager.joinChannel(player, "local");
        ChatChannelManager.focusChannel(player, "global");

        int messageCountBefore = gameContext.getSentMessages().size();

        // Just the shortcut with no message
        ChatEventHandler.onChat(player, "l:");

        // Focus should switch but no chat message sent to channel
        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("local", focus.get().name);
    }

    // === DM Handling ===

    @Test
    void dShortcutFocusesDmWithLastSender() {
        MockGamePlayer player = createPlayer("Player");
        MockGamePlayer sender = createPlayer("Sender");
        ChatChannelManager.setLastIncomingDmSender(player, sender.getUUID());

        ChatEventHandler.onChat(player, "d:Hello back!");

        var focus = ChatChannelManager.getFocus(player);
        assertTrue(focus.isPresent());
        assertTrue(focus.get() instanceof ChatFocus);
        assertEquals(ChatFocus.FocusType.DM, ((ChatFocus) focus.get()).getType());
    }

    @Test
    void dShortcutWithNoLastSenderShowsWarning() {
        MockGamePlayer player = createPlayer("Player");
        // No last DM sender set

        ChatEventHandler.onChat(player, "d:Hello?");

        // Should show warning message about no recent DMs
        assertTrue(gameContext.getSentMessages().stream()
            .anyMatch(msg -> msg.message().toString().contains("No recent DMs")));
    }

    // === Error Handling ===

    @Test
    void chatBeforeInitializationShowsWarning() {
        ChatChannelManager.reset(); // Uninitialize
        MockGamePlayer player = createPlayer("Player");

        ChatEventHandler.onChat(player, "Hello!");

        // Should show initialization warning
        assertTrue(gameContext.getSentMessages().stream()
            .anyMatch(msg -> msg.message().toString().contains("initializing")));
    }

    // === DM Message Sending ===

    @Test
    void dmMessageSentToBothParties() {
        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");

        // Set up DM focus
        ChatChannelManager.focusDm(sender, recipient.getUUID());
        gameContext.clearMessages();

        ChatEventHandler.onChat(sender, "Hello via DM!");

        // Both sender and recipient should receive messages
        // (sender sees "You -> Recipient", recipient sees "Sender -> You")
        var messages = gameContext.getSentMessages();
        assertTrue(messages.size() >= 2);
    }

    @Test
    void dmToOfflinePlayerShowsError() {
        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");

        // Set up DM focus then remove recipient
        ChatChannelManager.focusDm(sender, recipient.getUUID());
        gameContext.removePlayer(recipient);
        gameContext.clearMessages();

        ChatEventHandler.onChat(sender, "Hello?");

        // Should show offline error
        assertTrue(gameContext.getSentMessages().stream()
            .anyMatch(msg -> msg.message().toString().contains("not online")));
    }

    // === Permission Checking ===

    @Test
    void messageToRestrictedChannelWithoutPermissionFails() {
        ChatChannelManager.ChannelConfig staffChannel = new ChatChannelManager.ChannelConfig(
            "staff", "&c[Staff]", "s",
            "verbatim.channel.staff",  // permission required
            -1, "&f", ":", "&f", "&f",
            false, false, null, null
        );
        ChatChannelManager.reset();
        ChatChannelManager.addChannelConfig(staffChannel);

        MockGamePlayer player = createPlayer("Player");
        // Grant permission to join initially
        permissionService.grantPermission(player, "verbatim.channel.staff");
        ChatChannelManager.joinChannel(player, "staff");
        ChatChannelManager.focusChannel(player, "staff");

        // Now revoke permission
        permissionService.revokePermission(player, "verbatim.channel.staff");
        gameContext.clearMessages();

        ChatEventHandler.onChat(player, "Secret message!");

        // Should show permission error
        assertTrue(gameContext.getSentMessages().stream()
            .anyMatch(msg -> msg.message().toString().contains("permission")));
    }

    // === Edge Cases ===

    @Test
    void shortcutToRestrictedChannelWithoutPermissionShowsError() {
        // Create a restricted channel
        ChatChannelManager.ChannelConfig staffChannel = new ChatChannelManager.ChannelConfig(
            "staff", "&c[Staff]", "s",
            "verbatim.channel.staff",  // permission required
            -1, "&f", ":", "&f", "&f",
            false, false, null, null
        );
        ChatChannelManager.addChannelConfig(staffChannel);

        MockGamePlayer player = createPlayer("Player");
        ChatChannelManager.focusChannel(player, "global");
        gameContext.clearMessages();

        // Try to use s: shortcut without permission
        ChatEventHandler.onChat(player, "s:Hello staff!");

        // Should show permission error and NOT change focus
        assertTrue(gameContext.getSentMessages().stream()
            .anyMatch(msg -> msg.message().toString().contains("permission")));

        // Focus should remain on global, not staff
        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("global", focus.get().name);
    }

    @Test
    void messageDeliveredOnlyToChannelMembers() {
        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer memberRecipient = createPlayer("Member");
        MockGamePlayer nonMemberRecipient = createPlayer("NonMember");

        // Sender and one recipient join channel
        ChatChannelManager.joinChannel(sender, "local");
        ChatChannelManager.joinChannel(memberRecipient, "local");
        // nonMemberRecipient does NOT join

        ChatChannelManager.focusChannel(sender, "local");
        gameContext.clearMessages();

        ChatEventHandler.onChat(sender, "Hello local members!");

        // Member should receive the message
        var memberMessages = gameContext.getMessagesTo(memberRecipient);
        assertTrue(memberMessages.size() >= 1, "Channel member should receive message");

        // Non-member should NOT receive the message
        var nonMemberMessages = gameContext.getMessagesTo(nonMemberRecipient);
        assertEquals(0, nonMemberMessages.size(), "Non-member should not receive channel message");
    }

    @Test
    void messageWithColonInContentNotTreatedAsShortcut() {
        MockGamePlayer player = createPlayer("Player");
        ChatChannelManager.joinChannel(player, "global");
        ChatChannelManager.focusChannel(player, "global");

        // Message starting with "http:" should not be treated as a channel shortcut
        ChatEventHandler.onChat(player, "Check out http://example.com");

        // Should stay focused on global (http is not a valid shortcut)
        var focus = ChatChannelManager.getFocusedChannelConfig(player);
        assertTrue(focus.isPresent());
        assertEquals("global", focus.get().name);
    }

    @Test
    void recipientWithoutPermissionAutoKickedFromChannel() {
        // Create a restricted channel
        ChatChannelManager.ChannelConfig staffChannel = new ChatChannelManager.ChannelConfig(
            "staff", "&c[Staff]", "s",
            "verbatim.channel.staff",  // permission required
            -1, "&f", ":", "&f", "&f",
            false, false, null, null
        );
        ChatChannelManager.addChannelConfig(staffChannel);

        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");

        // Both have permission and join
        permissionService.grantPermission(sender, "verbatim.channel.staff");
        permissionService.grantPermission(recipient, "verbatim.channel.staff");
        ChatChannelManager.joinChannel(sender, "staff");
        ChatChannelManager.joinChannel(recipient, "staff");
        ChatChannelManager.focusChannel(sender, "staff");

        // Revoke recipient's permission
        permissionService.revokePermission(recipient, "verbatim.channel.staff");

        assertTrue(ChatChannelManager.isJoined(recipient, "staff"),
            "Recipient should still be in channel before message is sent");

        ChatEventHandler.onChat(sender, "Staff only message!");

        // Recipient should have been auto-kicked
        assertFalse(ChatChannelManager.isJoined(recipient, "staff"),
            "Recipient without permission should be auto-kicked when message is broadcast");
    }

    @Test
    void dmDoesNotBroadcastToOthers() {
        MockGamePlayer sender = createPlayer("Sender");
        MockGamePlayer recipient = createPlayer("Recipient");
        MockGamePlayer bystander = createPlayer("Bystander");

        ChatChannelManager.focusDm(sender, recipient.getUUID());
        gameContext.clearMessages();

        ChatEventHandler.onChat(sender, "Private message!");

        // Bystander should NOT receive any DM messages
        var bystanderMessages = gameContext.getMessagesTo(bystander);
        assertEquals(0, bystanderMessages.size(), "Bystander should not see DM");
    }
}
