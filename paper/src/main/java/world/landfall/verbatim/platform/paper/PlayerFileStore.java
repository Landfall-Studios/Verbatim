package world.landfall.verbatim.platform.paper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import world.landfall.verbatim.Verbatim;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player JSON file I/O engine with atomic writes and crash resilience.
 *
 * <p>File layout under {@code playerstore/}:
 * <pre>
 *   index.json              - UUID to last-known username mapping
 *   players/
 *     &lt;uuid&gt;.json          - one file per player
 * </pre>
 *
 * <p>Writes use atomic rename ({@code .tmp} -> {@code .json}) so a crash mid-write
 * never corrupts the original file. Per-UUID synchronized locks prevent concurrent
 * writes to the same player file while allowing different players to save in parallel.
 */
public class PlayerFileStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA_VERSION = 1;

    private final File playersDir;
    private final File indexFile;
    private final ConcurrentHashMap<UUID, Object> writeLocks = new ConcurrentHashMap<>();

    public PlayerFileStore(File storeDir) {
        this.playersDir = new File(storeDir, "players");
        this.indexFile = new File(storeDir, "index.json");
        storeDir.mkdirs();
        playersDir.mkdirs();
        cleanStaleTmpFiles();
    }

    public Map<String, String> loadPlayer(UUID uuid) {
        File file = playerFile(uuid);
        if (!file.exists()) {
            return Collections.emptyMap();
        }
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<PlayerFileData>(){}.getType();
            PlayerFileData data = GSON.fromJson(reader, type);
            if (data != null && data.data != null) {
                Verbatim.LOGGER.debug("[Verbatim] Loaded player file for {}: {} entries", uuid, data.data.size());
                return data.data;
            }
            Verbatim.LOGGER.warn("[Verbatim] Player file for {} was empty or missing data section", uuid);
            return Collections.emptyMap();
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim] Corrupt player file for {}, renaming to .corrupt: {}", uuid, e.getMessage());
            renameCorrupt(file);
            return Collections.emptyMap();
        }
    }

    public void savePlayer(UUID uuid, String username, Map<String, String> data) {
        Object lock = writeLocks.computeIfAbsent(uuid, k -> new Object());
        synchronized (lock) {
            File file = playerFile(uuid);
            File tmpFile = new File(playersDir, uuid.toString() + ".tmp");

            PlayerFileData fileData = new PlayerFileData();
            fileData.schemaVersion = SCHEMA_VERSION;
            fileData.uuid = uuid.toString();
            fileData.lastUsername = username;
            fileData.lastSeen = System.currentTimeMillis();
            fileData.data = data;

            try (FileWriter writer = new FileWriter(tmpFile)) {
                GSON.toJson(fileData, writer);
                writer.flush();
            } catch (IOException e) {
                Verbatim.LOGGER.error("[Verbatim] Failed to write tmp file for {}: {}", uuid, e.getMessage());
                tmpFile.delete();
                return;
            }

            try {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e2) {
                    Verbatim.LOGGER.error("[Verbatim] Failed to rename tmp to player file for {}: {}", uuid, e2.getMessage());
                    tmpFile.delete();
                }
            }
        }
    }

    public Map<String, IndexEntry> loadIndex() {
        if (!indexFile.exists()) {
            return new ConcurrentHashMap<>();
        }
        try (FileReader reader = new FileReader(indexFile)) {
            IndexFileData data = GSON.fromJson(reader, IndexFileData.class);
            if (data != null && data.players != null) {
                Verbatim.LOGGER.info("[Verbatim] Loaded player index: {} entries", data.players.size());
                return new ConcurrentHashMap<>(data.players);
            }
            return new ConcurrentHashMap<>();
        } catch (Exception e) {
            Verbatim.LOGGER.warn("[Verbatim] Corrupt index file, starting fresh: {}", e.getMessage());
            renameCorrupt(indexFile);
            return new ConcurrentHashMap<>();
        }
    }

    public void saveIndex(Map<String, IndexEntry> index) {
        File tmpFile = new File(indexFile.getParentFile(), "index.tmp");

        IndexFileData fileData = new IndexFileData();
        fileData.schemaVersion = SCHEMA_VERSION;
        fileData.players = index;

        try (FileWriter writer = new FileWriter(tmpFile)) {
            GSON.toJson(fileData, writer);
            writer.flush();
        } catch (IOException e) {
            Verbatim.LOGGER.error("[Verbatim] Failed to write tmp index file: {}", e.getMessage());
            tmpFile.delete();
            return;
        }

        try {
            Files.move(tmpFile.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(tmpFile.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                Verbatim.LOGGER.error("[Verbatim] Failed to rename tmp to index file: {}", e2.getMessage());
                tmpFile.delete();
            }
        }
    }

    private File playerFile(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".json");
    }

    private void renameCorrupt(File file) {
        File corrupt = new File(file.getParentFile(), file.getName() + ".corrupt");
        if (corrupt.exists()) {
            corrupt.delete();
        }
        file.renameTo(corrupt);
    }

    private void cleanStaleTmpFiles() {
        File[] tmpFiles = playersDir.listFiles((dir, name) -> name.endsWith(".tmp"));
        if (tmpFiles != null) {
            for (File tmp : tmpFiles) {
                Verbatim.LOGGER.info("[Verbatim] Cleaning stale tmp file: {}", tmp.getName());
                tmp.delete();
            }
        }
        File indexTmp = new File(indexFile.getParentFile(), "index.tmp");
        if (indexTmp.exists()) {
            Verbatim.LOGGER.info("[Verbatim] Cleaning stale index tmp file");
            indexTmp.delete();
        }
    }

    // === Data classes for JSON serialization ===

    static class PlayerFileData {
        int schemaVersion;
        String uuid;
        String lastUsername;
        long lastSeen;
        Map<String, String> data;
    }

    static class IndexFileData {
        int schemaVersion;
        Map<String, IndexEntry> players;
    }

    static class IndexEntry {
        String lastUsername;
        long lastSeen;

        IndexEntry() {}

        IndexEntry(String lastUsername, long lastSeen) {
            this.lastUsername = lastUsername;
            this.lastSeen = lastSeen;
        }
    }
}
