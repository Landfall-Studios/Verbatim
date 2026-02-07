package world.landfall.verbatim.util;

import world.landfall.verbatim.Verbatim;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized mail service for offline-capable player mail.
 * Mail is stored in a JSON file on disk, keyed by recipient UUID.
 * Thread-safe for concurrent sends to the same recipient.
 */
public class MailService {

    public static final int MAX_MAILBOX_SIZE = 50;

    private static final ConcurrentHashMap<UUID, List<MailMessage>> mailboxes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UUID> nameCache = new ConcurrentHashMap<>();
    private static Path dataDir;
    private static boolean initialized = false;

    public static class MailMessage {
        public final UUID senderUUID;
        public final String senderName;
        public final String message;
        public final long timestamp;
        public boolean read;

        public MailMessage(UUID senderUUID, String senderName, String message, long timestamp, boolean read) {
            this.senderUUID = senderUUID;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
            this.read = read;
        }
    }

    public static void init(Path dir) {
        dataDir = dir;
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            Verbatim.LOGGER.error("[MailService] Failed to create data directory: {}", dataDir, e);
        }
        loadFromDisk();
        initialized = true;
        Verbatim.LOGGER.info("[MailService] Initialized with {} mailboxes", mailboxes.size());
    }

    public static void shutdown() {
        if (!initialized) return;
        saveToDisk();
        initialized = false;
        Verbatim.LOGGER.info("[MailService] Shut down.");
    }

    public static boolean sendMail(UUID senderUUID, String senderName, UUID recipientUUID, String message) {
        if (!initialized) return false;

        List<MailMessage> mailbox = mailboxes.computeIfAbsent(recipientUUID, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (mailbox) {
            if (mailbox.size() >= MAX_MAILBOX_SIZE) {
                return false;
            }
            mailbox.add(new MailMessage(senderUUID, senderName, message, System.currentTimeMillis(), false));
        }
        saveToDisk();
        return true;
    }

    public static int getUnreadCount(UUID playerUUID) {
        List<MailMessage> mailbox = mailboxes.get(playerUUID);
        if (mailbox == null) return 0;
        synchronized (mailbox) {
            int count = 0;
            for (MailMessage msg : mailbox) {
                if (!msg.read) count++;
            }
            return count;
        }
    }

    public static List<MailMessage> getMail(UUID playerUUID) {
        List<MailMessage> mailbox = mailboxes.get(playerUUID);
        if (mailbox == null) return Collections.emptyList();
        synchronized (mailbox) {
            return new ArrayList<>(mailbox);
        }
    }

    public static void markAllRead(UUID playerUUID) {
        List<MailMessage> mailbox = mailboxes.get(playerUUID);
        if (mailbox == null) return;
        synchronized (mailbox) {
            for (MailMessage msg : mailbox) {
                msg.read = true;
            }
        }
        saveToDisk();
    }

    public static void clearMail(UUID playerUUID) {
        mailboxes.remove(playerUUID);
        saveToDisk();
    }

    public static void registerPlayerName(UUID uuid, String name) {
        nameCache.put(name.toLowerCase(), uuid);
    }

    public static UUID resolvePlayerUUID(String name) {
        return nameCache.get(name.toLowerCase());
    }

    // === JSON Persistence (manual, no Gson dependency in core) ===

    private static Path mailFile() {
        return dataDir.resolve("mail.json");
    }

    private static Path mailFileTmp() {
        return dataDir.resolve("mail.json.tmp");
    }

    private static synchronized void saveToDisk() {
        if (dataDir == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"mailboxes\": {");

            boolean firstMailbox = true;
            for (Map.Entry<UUID, List<MailMessage>> entry : mailboxes.entrySet()) {
                List<MailMessage> mailbox = entry.getValue();
                synchronized (mailbox) {
                    if (mailbox.isEmpty()) continue;
                    if (!firstMailbox) sb.append(",");
                    firstMailbox = false;
                    sb.append("\n    \"").append(entry.getKey()).append("\": [");
                    boolean firstMsg = true;
                    for (MailMessage msg : mailbox) {
                        if (!firstMsg) sb.append(",");
                        firstMsg = false;
                        sb.append("\n      {\"from\": \"").append(msg.senderUUID)
                          .append("\", \"fromName\": \"").append(escapeJson(msg.senderName))
                          .append("\", \"message\": \"").append(escapeJson(msg.message))
                          .append("\", \"timestamp\": ").append(msg.timestamp)
                          .append(", \"read\": ").append(msg.read)
                          .append("}");
                    }
                    sb.append("\n    ]");
                }
            }

            sb.append("\n  },\n  \"nameCache\": {");
            boolean firstName = true;
            for (Map.Entry<String, UUID> entry : nameCache.entrySet()) {
                if (!firstName) sb.append(",");
                firstName = false;
                sb.append("\n    \"").append(escapeJson(entry.getKey()))
                  .append("\": \"").append(entry.getValue()).append("\"");
            }
            sb.append("\n  }\n}\n");

            Path tmp = mailFileTmp();
            Path target = mailFile();
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                writer.write(sb.toString());
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Verbatim.LOGGER.error("[MailService] Failed to save mail data", e);
        }
    }

    private static void loadFromDisk() {
        mailboxes.clear();
        nameCache.clear();
        Path file = mailFile();
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            parseJson(sb.toString());
        } catch (IOException e) {
            Verbatim.LOGGER.error("[MailService] Failed to load mail data", e);
        }
    }

    /**
     * Simple JSON parser for the mail.json format.
     * Only handles our known structure — not a general-purpose parser.
     */
    private static void parseJson(String json) {
        try {
            // Find "mailboxes" section
            int mailboxesIdx = json.indexOf("\"mailboxes\"");
            if (mailboxesIdx < 0) return;

            int mailboxesObjStart = json.indexOf('{', mailboxesIdx);
            int mailboxesObjEnd = findMatchingBrace(json, mailboxesObjStart);
            if (mailboxesObjEnd < 0) return;

            String mailboxesStr = json.substring(mailboxesObjStart + 1, mailboxesObjEnd);
            parseMailboxes(mailboxesStr);

            // Find "nameCache" section
            int nameCacheIdx = json.indexOf("\"nameCache\"");
            if (nameCacheIdx < 0) return;

            int nameCacheObjStart = json.indexOf('{', nameCacheIdx);
            int nameCacheObjEnd = findMatchingBrace(json, nameCacheObjStart);
            if (nameCacheObjEnd < 0) return;

            String nameCacheStr = json.substring(nameCacheObjStart + 1, nameCacheObjEnd);
            parseNameCache(nameCacheStr);
        } catch (Exception e) {
            Verbatim.LOGGER.error("[MailService] Failed to parse mail.json", e);
        }
    }

    private static void parseMailboxes(String str) {
        int pos = 0;
        while (pos < str.length()) {
            int keyStart = str.indexOf('"', pos);
            if (keyStart < 0) break;
            int keyEnd = str.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String uuidStr = str.substring(keyStart + 1, keyEnd);

            int arrayStart = str.indexOf('[', keyEnd);
            if (arrayStart < 0) break;
            int arrayEnd = findMatchingBracket(str, arrayStart);
            if (arrayEnd < 0) break;

            try {
                UUID recipientUUID = UUID.fromString(uuidStr);
                String arrayContent = str.substring(arrayStart + 1, arrayEnd);
                List<MailMessage> messages = parseMailArray(arrayContent);
                if (!messages.isEmpty()) {
                    mailboxes.put(recipientUUID, Collections.synchronizedList(messages));
                }
            } catch (IllegalArgumentException e) {
                Verbatim.LOGGER.warn("[MailService] Invalid UUID in mailboxes: {}", uuidStr);
            }

            pos = arrayEnd + 1;
        }
    }

    private static List<MailMessage> parseMailArray(String str) {
        List<MailMessage> messages = new ArrayList<>();
        int pos = 0;
        while (pos < str.length()) {
            int objStart = str.indexOf('{', pos);
            if (objStart < 0) break;
            int objEnd = findMatchingBrace(str, objStart);
            if (objEnd < 0) break;

            String objStr = str.substring(objStart + 1, objEnd);
            MailMessage msg = parseMailMessage(objStr);
            if (msg != null) {
                messages.add(msg);
            }
            pos = objEnd + 1;
        }
        return messages;
    }

    private static MailMessage parseMailMessage(String obj) {
        String from = extractJsonString(obj, "from");
        String fromName = extractJsonString(obj, "fromName");
        String message = extractJsonString(obj, "message");
        Long timestamp = extractJsonLong(obj, "timestamp");
        Boolean read = extractJsonBoolean(obj, "read");

        if (from == null || fromName == null || message == null || timestamp == null || read == null) {
            return null;
        }

        try {
            return new MailMessage(UUID.fromString(from), fromName, message, timestamp, read);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void parseNameCache(String str) {
        int pos = 0;
        while (pos < str.length()) {
            int keyStart = str.indexOf('"', pos);
            if (keyStart < 0) break;
            int keyEnd = str.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String name = str.substring(keyStart + 1, keyEnd);

            int valStart = str.indexOf('"', keyEnd + 1);
            if (valStart < 0) break;
            int valEnd = str.indexOf('"', valStart + 1);
            if (valEnd < 0) break;
            String uuidStr = str.substring(valStart + 1, valEnd);

            try {
                nameCache.put(name, UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                Verbatim.LOGGER.warn("[MailService] Invalid UUID in nameCache: {}", uuidStr);
            }

            pos = valEnd + 1;
        }
    }

    // === JSON utility methods ===

    private static String extractJsonString(String obj, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = obj.indexOf(searchKey);
        if (keyIdx < 0) return null;
        int colonIdx = obj.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;
        int valStart = obj.indexOf('"', colonIdx + 1);
        if (valStart < 0) return null;
        int valEnd = findUnescapedQuote(obj, valStart + 1);
        if (valEnd < 0) return null;
        return unescapeJson(obj.substring(valStart + 1, valEnd));
    }

    private static Long extractJsonLong(String obj, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = obj.indexOf(searchKey);
        if (keyIdx < 0) return null;
        int colonIdx = obj.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;
        int start = colonIdx + 1;
        while (start < obj.length() && obj.charAt(start) == ' ') start++;
        int end = start;
        while (end < obj.length() && (Character.isDigit(obj.charAt(end)) || obj.charAt(end) == '-')) end++;
        if (end == start) return null;
        try {
            return Long.parseLong(obj.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean extractJsonBoolean(String obj, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = obj.indexOf(searchKey);
        if (keyIdx < 0) return null;
        int colonIdx = obj.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;
        String rest = obj.substring(colonIdx + 1).trim();
        if (rest.startsWith("true")) return true;
        if (rest.startsWith("false")) return false;
        return null;
    }

    private static int findMatchingBrace(String str, int openPos) {
        int depth = 1;
        boolean inString = false;
        for (int i = openPos + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    private static int findMatchingBracket(String str, int openPos) {
        int depth = 1;
        boolean inString = false;
        for (int i = openPos + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '[') depth++;
                else if (c == ']') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }

    private static int findUnescapedQuote(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '\\') { i++; continue; }
            if (str.charAt(i) == '"') return i;
        }
        return -1;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private static String unescapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
