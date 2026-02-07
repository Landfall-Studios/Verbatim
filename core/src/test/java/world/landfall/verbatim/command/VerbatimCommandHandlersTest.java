package world.landfall.verbatim.command;

import org.junit.jupiter.api.Test;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.test.MockGameCommandSource;
import world.landfall.verbatim.test.MockGameContext;
import world.landfall.verbatim.test.MockGamePlayer;
import world.landfall.verbatim.test.VerbatimTestBase;
import world.landfall.verbatim.util.NicknameService;
import world.landfall.verbatim.util.SocialService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerbatimCommandHandlersTest extends VerbatimTestBase {

    // === Custom List Command ===

    @Test
    void customListShowsOnlinePlayers() {
        MockGamePlayer alice = createPlayer("Alice");
        MockGamePlayer bob = createPlayer("Bob");
        MockGameCommandSource source = new MockGameCommandSource(alice);

        int result = VerbatimCommandHandlers.executeCustomListCommand(source);
        assertEquals(2, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(alice);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Alice")));
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Bob")));
    }

    @Test
    void customListShowsEmptyMessage() {
        MockGamePlayer alice = createPlayer("Alice");
        MockGameCommandSource source = new MockGameCommandSource(alice);
        gameContext.removePlayer(alice);

        int result = VerbatimCommandHandlers.executeCustomListCommand(source);
        assertEquals(1, result);
    }

    @Test
    void customListShowsDisplayNameWithUsername() {
        MockGamePlayer alice = createPlayer("Alice", "Queen Alice");
        MockGameCommandSource source = new MockGameCommandSource(alice);

        int result = VerbatimCommandHandlers.executeCustomListCommand(source);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(alice);
        assertTrue(messages.stream().anyMatch(m -> {
            String text = m.message().getString();
            return text.contains("Queen Alice") && text.contains("Alice");
        }));
    }

    // === List Online Players (vlist) ===

    @Test
    void vlistShowsOnlinePlayers() {
        MockGamePlayer alice = createPlayer("Alice");
        createPlayer("Bob");
        MockGameCommandSource source = new MockGameCommandSource(alice);

        int result = VerbatimCommandHandlers.listOnlinePlayers(source);
        assertEquals(2, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(alice);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("vlist")));
    }

    @Test
    void vlistShowsEmptyWhenNoPlayers() {
        MockGamePlayer alice = createPlayer("Alice");
        MockGameCommandSource source = new MockGameCommandSource(alice);
        gameContext.removePlayer(alice);

        int result = VerbatimCommandHandlers.listOnlinePlayers(source);
        assertEquals(1, result);
    }

    // === Show Help ===

    @Test
    void showHelpDisplaysCommands() {
        MockGamePlayer player = createPlayer("Alice");
        MockGameCommandSource source = new MockGameCommandSource(player);

        int result = VerbatimCommandHandlers.showHelp(source);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> {
            String text = m.message().getString();
            return text.contains("/msg") && text.contains("/channels");
        }));
    }

    // === Direct Messages ===

    @Test
    void sendDirectMessageDelivers() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        int result = VerbatimCommandHandlers.sendDirectMessage(sender, target, "Hello!");
        assertEquals(1, result);

        List<MockGameContext.SentMessage> senderMsgs = gameContext.getMessagesTo(sender);
        assertTrue(senderMsgs.stream().anyMatch(m -> m.message().getString().contains("Hello!")));
        assertTrue(senderMsgs.stream().anyMatch(m -> m.message().getString().contains("Bob")));

        List<MockGameContext.SentMessage> targetMsgs = gameContext.getMessagesTo(target);
        assertTrue(targetMsgs.stream().anyMatch(m -> m.message().getString().contains("Hello!")));
        assertTrue(targetMsgs.stream().anyMatch(m -> m.message().getString().contains("Alice")));
    }

    @Test
    void sendDirectMessageBlockedBySenderIgnore() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addIgnore(sender, target.getUUID());

        int result = VerbatimCommandHandlers.sendDirectMessage(sender, target, "Hello!");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(sender);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("ignored")));
    }

    @Test
    void sendDirectMessageBlockedByTargetIgnore() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addIgnore(target, sender.getUUID());

        int result = VerbatimCommandHandlers.sendDirectMessage(sender, target, "Hello!");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> senderMsgs = gameContext.getMessagesTo(sender);
        assertTrue(senderMsgs.stream().anyMatch(m -> m.message().getString().contains("Cannot send message")));
    }

    // === Reply to DM ===

    @Test
    void replyToLastDmSendsMessage() {
        MockGamePlayer alice = createPlayer("Alice");
        MockGamePlayer bob = createPlayer("Bob");

        // Bob sends DM to Alice first (sets Alice's last incoming sender)
        VerbatimCommandHandlers.sendDirectMessage(bob, alice, "Hi Alice!");
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.replyToLastDm(alice, "Hi back!");
        assertEquals(1, result);

        List<MockGameContext.SentMessage> aliceMsgs = gameContext.getMessagesTo(alice);
        assertTrue(aliceMsgs.stream().anyMatch(m -> m.message().getString().contains("Hi back!")));
    }

    @Test
    void replyWithNoRecentDmFails() {
        MockGamePlayer alice = createPlayer("Alice");

        int result = VerbatimCommandHandlers.replyToLastDm(alice, "Hello?");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(alice);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("No recent DMs")));
    }

    @Test
    void replyWhenTargetOfflineFails() {
        MockGamePlayer alice = createPlayer("Alice");
        MockGamePlayer bob = createPlayer("Bob");

        VerbatimCommandHandlers.sendDirectMessage(bob, alice, "Hi!");
        gameContext.removePlayer(bob);
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.replyToLastDm(alice, "Reply!");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(alice);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("not online")));
    }

    // === Nick Commands ===

    @Test
    void nickShowDisplaysCurrentNickname() {
        MockGamePlayer player = createPlayer("Alice");
        NicknameService.setNickname(player, "Allie");

        int result = VerbatimCommandHandlers.executeNickShow(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Allie")));
    }

    @Test
    void nickShowNoNicknameSet() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeNickShow(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("don't have a nickname")));
    }

    @Test
    void nickSetSuccess() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeNickSet(player, "Allie");
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Nickname set")));
        assertEquals("Allie", NicknameService.getNickname(player));
    }

    @Test
    void nickSetTooLongFails() {
        MockGamePlayer player = createPlayer("Alice");
        String longNick = "A".repeat(65);

        int result = VerbatimCommandHandlers.executeNickSet(player, longNick);
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("too long")));
    }

    @Test
    void nickClearSuccess() {
        MockGamePlayer player = createPlayer("Alice");
        NicknameService.setNickname(player, "Allie");
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeNickClear(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Nickname cleared")));
        assertNull(NicknameService.getNickname(player));
    }

    @Test
    void nickClearWhenNoneSet() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeNickClear(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("don't have a nickname")));
    }

    // === Ignore Commands ===

    @Test
    void ignoreAddSuccess() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        int result = VerbatimCommandHandlers.executeIgnoreAdd(player, target);
        assertEquals(1, result);

        assertTrue(SocialService.isIgnoring(player, target.getUUID()));
        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Now ignoring")));
    }

    @Test
    void ignoreAddSelfFails() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeIgnoreAdd(player, player);
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("cannot ignore yourself")));
    }

    @Test
    void ignoreAddAlreadyIgnoredFails() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addIgnore(player, target.getUUID());
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeIgnoreAdd(player, target);
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("already ignoring")));
    }

    @Test
    void ignoreRemoveSuccess() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addIgnore(player, target.getUUID());
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeIgnoreRemove(player, "Bob");
        assertEquals(1, result);

        assertFalse(SocialService.isIgnoring(player, target.getUUID()));
        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("No longer ignoring")));
    }

    @Test
    void ignoreRemoveEmptyListFails() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeIgnoreRemove(player, "Bob");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("empty")));
    }

    @Test
    void ignoreRemovePlayerNotFoundFails() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addIgnore(player, target.getUUID());
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeIgnoreRemove(player, "Charlie");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("not found")));
    }

    @Test
    void ignoreListShowsIgnoredPlayers() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addIgnore(player, target.getUUID());
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeIgnoreList(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Bob")));
    }

    @Test
    void ignoreListEmptyShowsEmpty() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeIgnoreList(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("empty")));
    }

    // === Favorite Commands ===

    @Test
    void favAddSuccess() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        int result = VerbatimCommandHandlers.executeFavAdd(player, target);
        assertEquals(1, result);

        assertTrue(SocialService.isFavorited(player, target.getUUID()));
        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Added")));
    }

    @Test
    void favAddSelfFails() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeFavAdd(player, player);
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("cannot favorite yourself")));
    }

    @Test
    void favAddAlreadyFavoritedFails() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addFavorite(player, target.getUUID(), "Bob");
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeFavAdd(player, target);
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("already favorited")));
    }

    @Test
    void favRemoveSuccess() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addFavorite(player, target.getUUID(), "Bob");
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeFavRemove(player, "Bob");
        assertEquals(1, result);

        assertFalse(SocialService.isFavorited(player, target.getUUID()));
        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Removed")));
    }

    @Test
    void favRemoveEmptyListFails() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeFavRemove(player, "Bob");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("empty")));
    }

    @Test
    void favRemoveNotFoundFails() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addFavorite(player, target.getUUID(), "Bob");
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeFavRemove(player, "Charlie");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("not found")));
    }

    @Test
    void favListShowsFavorites() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addFavorite(player, target.getUUID(), "Bob");
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeFavList(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> {
            String text = m.message().getString();
            return text.contains("Bob") && text.contains("Online");
        }));
    }

    @Test
    void favListShowsOfflineFavorite() {
        MockGamePlayer player = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");
        SocialService.addFavorite(player, target.getUUID(), "Bob");
        gameContext.removePlayer(target);
        gameContext.clearMessages();

        int result = VerbatimCommandHandlers.executeFavList(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Bob")));
    }

    @Test
    void favListEmptyShowsEmpty() {
        MockGamePlayer player = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeFavList(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("empty")));
    }

    // === Channel Commands ===

    @Test
    void listChannelsWithNoChannelsConfigured() {
        MockGamePlayer player = createPlayer("Alice");
        MockGameCommandSource source = new MockGameCommandSource(player);

        int result = VerbatimCommandHandlers.listChannels(source);
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("No chat channels")));
    }

    @Test
    void chListPlayerNotFoundAndNoChannel() {
        MockGamePlayer admin = createPlayer("Admin");
        MockGameCommandSource source = new MockGameCommandSource(admin);

        int result = VerbatimCommandHandlers.executeChList(source, "NonExistent");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(admin);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("not a valid")));
    }

    @Test
    void chListOnlinePlayerWithNoChannels() {
        MockGamePlayer admin = createPlayer("Admin");
        MockGamePlayer target = createPlayer("Bob");
        MockGameCommandSource source = new MockGameCommandSource(admin);

        int result = VerbatimCommandHandlers.executeChList(source, "Bob");
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(admin);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("not in any channels")));
    }
}
