package world.landfall.verbatim.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import world.landfall.verbatim.test.VerbatimTestBase;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MailServiceTest extends VerbatimTestBase {

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
    void sendMailAndRetrieve() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        boolean sent = MailService.sendMail(sender, "Alice", recipient, "Hello!");
        assertTrue(sent);

        List<MailService.MailMessage> mail = MailService.getMail(recipient);
        assertEquals(1, mail.size());

        MailService.MailMessage msg = mail.get(0);
        assertEquals(sender, msg.senderUUID);
        assertEquals("Alice", msg.senderName);
        assertEquals("Hello!", msg.message);
        assertFalse(msg.read);
        assertTrue(msg.timestamp > 0);
    }

    @Test
    void unreadCountTracksCorrectly() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        MailService.sendMail(sender, "Bob", recipient, "msg1");
        MailService.sendMail(sender, "Bob", recipient, "msg2");
        MailService.sendMail(sender, "Bob", recipient, "msg3");

        assertEquals(3, MailService.getUnreadCount(recipient));
    }

    @Test
    void markAllReadSetsReadFlag() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        MailService.sendMail(sender, "Carol", recipient, "message");
        assertEquals(1, MailService.getUnreadCount(recipient));

        MailService.markAllRead(recipient);
        assertEquals(0, MailService.getUnreadCount(recipient));

        List<MailService.MailMessage> mail = MailService.getMail(recipient);
        assertEquals(1, mail.size());
        assertTrue(mail.get(0).read);
    }

    @Test
    void clearMailRemovesAll() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        MailService.sendMail(sender, "Dave", recipient, "message");
        assertFalse(MailService.getMail(recipient).isEmpty());

        MailService.clearMail(recipient);
        assertTrue(MailService.getMail(recipient).isEmpty());
        assertEquals(0, MailService.getUnreadCount(recipient));
    }

    @Test
    void mailboxLimitRejectsSendsOver50() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        for (int i = 0; i < MailService.MAX_MAILBOX_SIZE; i++) {
            assertTrue(MailService.sendMail(sender, "Spammer", recipient, "msg " + i));
        }

        assertFalse(MailService.sendMail(sender, "Spammer", recipient, "msg 51"));
        assertEquals(MailService.MAX_MAILBOX_SIZE, MailService.getMail(recipient).size());
    }

    @Test
    void registerAndResolvePlayerName() {
        UUID uuid = UUID.randomUUID();
        MailService.registerPlayerName(uuid, "Steve");

        assertEquals(uuid, MailService.resolvePlayerUUID("Steve"));
    }

    @Test
    void resolvePlayerNameCaseInsensitive() {
        UUID uuid = UUID.randomUUID();
        MailService.registerPlayerName(uuid, "Steve");

        assertEquals(uuid, MailService.resolvePlayerUUID("steve"));
        assertEquals(uuid, MailService.resolvePlayerUUID("STEVE"));
    }

    @Test
    void resolveUnknownNameReturnsNull() {
        assertNull(MailService.resolvePlayerUUID("NonExistent"));
    }

    @Test
    void persistenceRoundTrip() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        MailService.sendMail(sender, "Persister", recipient, "persistent message");
        MailService.registerPlayerName(sender, "Persister");
        MailService.shutdown();

        // Re-init from same directory — should reload data
        MailService.init(tempDir);

        List<MailService.MailMessage> mail = MailService.getMail(recipient);
        assertEquals(1, mail.size());
        assertEquals("persistent message", mail.get(0).message);
        assertEquals("Persister", mail.get(0).senderName);
        assertEquals(sender, mail.get(0).senderUUID);

        assertEquals(sender, MailService.resolvePlayerUUID("Persister"));
    }

    @Test
    void sendMailToSelfAllowed() {
        UUID player = UUID.randomUUID();

        // MailService itself doesn't block self-sends; the command handler does
        assertTrue(MailService.sendMail(player, "Me", player, "note to self"));
        assertEquals(1, MailService.getMail(player).size());
    }

    @Test
    void getMailReturnsEmptyForUnknownPlayer() {
        List<MailService.MailMessage> mail = MailService.getMail(UUID.randomUUID());
        assertNotNull(mail);
        assertTrue(mail.isEmpty());
    }

    @Test
    void getUnreadCountReturnsZeroForUnknownPlayer() {
        assertEquals(0, MailService.getUnreadCount(UUID.randomUUID()));
    }
}
