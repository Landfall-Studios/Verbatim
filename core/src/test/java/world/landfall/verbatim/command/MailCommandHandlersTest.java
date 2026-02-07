package world.landfall.verbatim.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import world.landfall.verbatim.test.MockGameCommandSource;
import world.landfall.verbatim.test.MockGameContext;
import world.landfall.verbatim.test.MockGamePlayer;
import world.landfall.verbatim.test.VerbatimTestBase;
import world.landfall.verbatim.util.MailService;
import world.landfall.verbatim.util.SocialService;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MailCommandHandlersTest extends VerbatimTestBase {

    @TempDir
    Path tempDir;

    @BeforeEach
    public void initMail() {
        MailService.init(tempDir);
    }

    @AfterEach
    void shutdownMail() {
        MailService.shutdown();
    }

    @Test
    void sendMailToOnlinePlayer() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        int result = VerbatimCommandHandlers.executeMailSend(sender, "Bob", "Hello Bob!");
        assertEquals(1, result);

        // Verify success message to sender
        List<MockGameContext.SentMessage> senderMessages = gameContext.getMessagesTo(sender);
        assertTrue(senderMessages.stream().anyMatch(m -> m.message().getString().contains("Mail sent to")));

        // Verify notification to online target
        List<MockGameContext.SentMessage> targetMessages = gameContext.getMessagesTo(target);
        assertTrue(targetMessages.stream().anyMatch(m -> m.message().getString().contains("new mail from")));

        // Verify mail actually stored
        assertEquals(1, MailService.getMail(target.getUUID()).size());
    }

    @Test
    void sendMailToOfflinePlayerViaNameCache() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        // Register name in cache, then remove player (simulate offline)
        MailService.registerPlayerName(target.getUUID(), "Bob");
        gameContext.removePlayer(target);

        int result = VerbatimCommandHandlers.executeMailSend(sender, "Bob", "Hey offline Bob!");
        assertEquals(1, result);

        assertEquals(1, MailService.getMail(target.getUUID()).size());
    }

    @Test
    void sendMailToUnknownPlayerFails() {
        MockGamePlayer sender = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeMailSend(sender, "UnknownPlayer", "Hello?");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(sender);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("not found")));
    }

    @Test
    void sendMailToSelfFails() {
        MockGamePlayer sender = createPlayer("Alice");

        int result = VerbatimCommandHandlers.executeMailSend(sender, "Alice", "Note to self");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(sender);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("cannot send mail to yourself")));
    }

    @Test
    void sendMailEmptyMessageFails() {
        MockGamePlayer sender = createPlayer("Alice");
        createPlayer("Bob");

        assertEquals(0, VerbatimCommandHandlers.executeMailSend(sender, "Bob", null));
        assertEquals(0, VerbatimCommandHandlers.executeMailSend(sender, "Bob", ""));
        assertEquals(0, VerbatimCommandHandlers.executeMailSend(sender, "Bob", "   "));
    }

    @Test
    void sendMailFullMailboxFails() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        for (int i = 0; i < MailService.MAX_MAILBOX_SIZE; i++) {
            MailService.sendMail(sender.getUUID(), "Alice", target.getUUID(), "msg " + i);
        }

        gameContext.clearMessages();
        int result = VerbatimCommandHandlers.executeMailSend(sender, "Bob", "One too many");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(sender);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("full")));
    }

    @Test
    void readMailShowsMessages() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer reader = createPlayer("Bob");

        MailService.sendMail(sender.getUUID(), "Alice", reader.getUUID(), "Test message");

        gameContext.clearMessages();
        int result = VerbatimCommandHandlers.executeMailRead(reader);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(reader);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Test message")));
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("Alice")));

        // Verify marked as read
        assertEquals(0, MailService.getUnreadCount(reader.getUUID()));
    }

    @Test
    void readEmptyMailboxShowsEmpty() {
        MockGamePlayer reader = createPlayer("Bob");

        int result = VerbatimCommandHandlers.executeMailRead(reader);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(reader);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("empty")));
    }

    @Test
    void clearMailSuccess() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer player = createPlayer("Bob");

        MailService.sendMail(sender.getUUID(), "Alice", player.getUUID(), "msg");

        gameContext.clearMessages();
        int result = VerbatimCommandHandlers.executeMailClear(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("cleared")));
        assertTrue(MailService.getMail(player.getUUID()).isEmpty());
    }

    @Test
    void clearEmptyMailboxShowsAlready() {
        MockGamePlayer player = createPlayer("Bob");

        int result = VerbatimCommandHandlers.executeMailClear(player);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("already empty")));
    }

    @Test
    void mailHelpShowsUnreadCount() {
        MockGamePlayer player = createPlayer("Bob");
        MockGameCommandSource source = new MockGameCommandSource(player);

        MailService.sendMail(UUID.randomUUID(), "Alice", player.getUUID(), "msg1");
        MailService.sendMail(UUID.randomUUID(), "Carol", player.getUUID(), "msg2");

        gameContext.clearMessages();
        int result = VerbatimCommandHandlers.executeMailHelp(source);
        assertEquals(1, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(player);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("2")));
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("unread")));
    }

    @Test
    void ignoredPlayerCannotSendMail() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        // Sender ignores target
        SocialService.addIgnore(sender, target.getUUID());

        gameContext.clearMessages();
        int result = VerbatimCommandHandlers.executeMailSend(sender, "Bob", "Hello");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> messages = gameContext.getMessagesTo(sender);
        assertTrue(messages.stream().anyMatch(m -> m.message().getString().contains("ignored")));
    }

    @Test
    void ignoredBySenderCannotReceiveMail() {
        MockGamePlayer sender = createPlayer("Alice");
        MockGamePlayer target = createPlayer("Bob");

        // Target ignores sender
        SocialService.addIgnore(target, sender.getUUID());

        gameContext.clearMessages();
        int result = VerbatimCommandHandlers.executeMailSend(sender, "Bob", "Hello");
        assertEquals(0, result);

        List<MockGameContext.SentMessage> senderMessages = gameContext.getMessagesTo(sender);
        assertTrue(senderMessages.stream().anyMatch(m -> m.message().getString().contains("Cannot send mail")));
    }
}
