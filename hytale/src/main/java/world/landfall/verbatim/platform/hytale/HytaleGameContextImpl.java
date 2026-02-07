package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GameContext;
import world.landfall.verbatim.context.GamePlayer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hytale implementation of GameContext.
 * Uses Hytale's Universe API for player access and messaging.
 */
public class HytaleGameContextImpl implements GameContext {

    /**
     * In-memory persistent data store keyed by player UUID + data key.
     * Hytale doesn't have Minecraft's NBT PersistentData out of the box,
     * so we use a simple in-memory map (backed by per-player file persistence).
     */
    private final ConcurrentHashMap<String, String> persistentData = new ConcurrentHashMap<>();

    /** Per-player file store for crash-resilient persistence. */
    private PlayerFileStore fileStore;

    /** Data directory for Verbatim plugin files. */
    private Path dataDirectory;

    /** Tracks online player UUIDs to their last known usernames (for file saves). */
    private final ConcurrentHashMap<UUID, String> onlinePlayerUsernames = new ConcurrentHashMap<>();

    /**
     * Unwraps a GamePlayer to a PlayerRef. For use in platform layer only.
     */
    public static PlayerRef unwrap(GamePlayer player) {
        if (player instanceof HytaleGamePlayer hp) {
            return hp.getHandle();
        }
        throw new IllegalArgumentException("GamePlayer is not a HytaleGamePlayer: " + player);
    }

    /**
     * Sets the file store used for per-player persistence.
     */
    public void setFileStore(PlayerFileStore fileStore) {
        this.fileStore = fileStore;
    }

    /**
     * Sets the data directory for this context.
     */
    public void setDataDirectory(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    // === Server Operations ===

    @Override
    public boolean isServerAvailable() {
        try {
            return Universe.get() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return Collections.emptyList();
            }
            return universe.getPlayers().stream()
                .map(HytaleGamePlayer::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public int getOnlinePlayerCount() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return 0;
            }
            return universe.getPlayerCount();
        } catch (Exception e) {
            return 0;
        }
    }

    // === Player Retrieval ===

    @Override
    public GamePlayer getPlayerByUUID(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            PlayerRef playerRef = universe.getPlayer(uuid);
            return playerRef != null ? new HytaleGamePlayer(playerRef) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public GamePlayer getPlayerByName(String name) {
        if (name == null) {
            return null;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            PlayerRef playerRef = universe.getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
            return playerRef != null ? new HytaleGamePlayer(playerRef) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // === Distance and Position ===

    @Override
    public double getDistanceSquared(GamePlayer player1, GamePlayer player2) {
        if (player1 == null || player2 == null) {
            return Double.MAX_VALUE;
        }
        PlayerRef ref1 = unwrap(player1);
        PlayerRef ref2 = unwrap(player2);

        // PlayerRef.getTransform() returns a Transform directly with x, y, z position.
        try {
            var transform1 = ref1.getTransform();
            var transform2 = ref2.getTransform();

            if (transform1 == null || transform2 == null) {
                return Double.MAX_VALUE;
            }

            var pos1 = transform1.getPosition();
            var pos2 = transform2.getPosition();
            double dx = pos1.getX() - pos2.getX();
            double dy = pos1.getY() - pos2.getY();
            double dz = pos1.getZ() - pos2.getZ();
            return dx * dx + dy * dy + dz * dz;
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    // === Messaging ===

    @Override
    public void sendMessage(GamePlayer player, GameComponent message) {
        if (player == null || message == null) {
            return;
        }
        PlayerRef ref = unwrap(player);
        ref.sendMessage(((HytaleGameComponentImpl) message).toHytale());
    }

    @Override
    public void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers) {
        if (message == null) {
            return;
        }
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return;
            }
            Message hytaleMessage = ((HytaleGameComponentImpl) message).toHytale();
            for (PlayerRef playerRef : universe.getPlayers()) {
                playerRef.sendMessage(hytaleMessage);
            }
        } catch (Exception e) {
            // Silently fail if universe is not available
        }
    }

    // === Persistent Data ===

    private String dataKey(GamePlayer player, String key) {
        return player.getUUID().toString() + ":" + key;
    }

    @Override
    public boolean hasPlayerData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return false;
        }
        String fullKey = dataKey(player, key);
        boolean has = persistentData.containsKey(fullKey);
        Verbatim.LOGGER.debug("[Verbatim] hasPlayerData: {} = {}", fullKey, has);
        return has;
    }

    @Override
    public String getPlayerStringData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return "";
        }
        String fullKey = dataKey(player, key);
        String value = persistentData.getOrDefault(fullKey, "");
        Verbatim.LOGGER.debug("[Verbatim] getPlayerStringData: {} = {}", fullKey, value);
        return value;
    }

    @Override
    public void setPlayerStringData(GamePlayer player, String key, String value) {
        if (player == null || key == null || value == null) {
            return;
        }
        String fullKey = dataKey(player, key);
        persistentData.put(fullKey, value);
        Verbatim.LOGGER.debug("[Verbatim] setPlayerStringData: {} = {}", fullKey, value);
        schedulePlayerSave(player);
    }

    @Override
    public void removePlayerData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return;
        }
        persistentData.remove(dataKey(player, key));
        schedulePlayerSave(player);
    }

    // === Per-Player Persistence ===

    /**
     * Saves a single player's data to disk immediately.
     * Extracts all entries belonging to this player from the in-memory map.
     */
    private void schedulePlayerSave(GamePlayer player) {
        if (fileStore == null) {
            return;
        }
        UUID uuid = player.getUUID();
        String prefix = uuid.toString() + ":";
        Map<String, String> playerData = new HashMap<>();
        for (Map.Entry<String, String> entry : persistentData.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                // Store with the key minus the UUID prefix
                playerData.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        String username = onlinePlayerUsernames.getOrDefault(uuid, player.getUsername());
        fileStore.savePlayer(uuid, username, playerData);
    }

    /**
     * Loads a player's data from disk into the in-memory map.
     * Called when a player joins the server.
     */
    public void loadPlayerFromDisk(UUID uuid) {
        if (fileStore == null) {
            return;
        }
        Map<String, String> data = fileStore.loadPlayer(uuid);
        String prefix = uuid.toString() + ":";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            persistentData.put(prefix + entry.getKey(), entry.getValue());
        }
        if (!data.isEmpty()) {
            Verbatim.LOGGER.info("[Verbatim] Loaded {} data entries for player {}", data.size(), uuid);
        }
    }

    /**
     * Saves all players' data to disk. Used by the periodic auto-save and shutdown flush.
     * Groups all in-memory entries by UUID and writes each player file, then updates the index.
     */
    public void saveAllPlayersToDisk() {
        if (fileStore == null) {
            return;
        }

        // Group all persistent data entries by player UUID
        Map<UUID, Map<String, String>> grouped = new HashMap<>();
        for (Map.Entry<String, String> entry : persistentData.entrySet()) {
            int colonIdx = entry.getKey().indexOf(':');
            if (colonIdx < 0) {
                continue;
            }
            String uuidStr = entry.getKey().substring(0, colonIdx);
            String dataKey = entry.getKey().substring(colonIdx + 1);
            try {
                UUID uuid = UUID.fromString(uuidStr);
                grouped.computeIfAbsent(uuid, k -> new HashMap<>()).put(dataKey, entry.getValue());
            } catch (IllegalArgumentException e) {
                Verbatim.LOGGER.warn("[Verbatim] Skipping entry with invalid UUID key: {}", entry.getKey());
            }
        }

        // Save each player and build the index
        Map<String, PlayerFileStore.IndexEntry> index = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, Map<String, String>> playerEntry : grouped.entrySet()) {
            UUID uuid = playerEntry.getKey();
            String username = onlinePlayerUsernames.getOrDefault(uuid, uuid.toString());
            fileStore.savePlayer(uuid, username, playerEntry.getValue());
            index.put(uuid.toString(), new PlayerFileStore.IndexEntry(username, System.currentTimeMillis()));
        }

        fileStore.saveIndex(index);
        Verbatim.LOGGER.info("[Verbatim] Saved all player data ({} players)", grouped.size());
    }

    /**
     * Tracks a player as online with their username (for save operations).
     */
    public void trackPlayerOnline(UUID uuid, String username) {
        onlinePlayerUsernames.put(uuid, username);
    }

    /**
     * Removes a player from online tracking.
     */
    public void trackPlayerOffline(UUID uuid) {
        onlinePlayerUsernames.remove(uuid);
    }

    // === Permissions ===

    @Override
    public boolean hasPermissionLevel(GamePlayer player, int level) {
        if (player == null) {
            return false;
        }
        if (level <= 0) {
            return true;
        }
        // Hytale uses string-based permissions. For OP-level compatibility,
        // we check a generic admin permission node via PermissionsModule.
        PlayerRef ref = unwrap(player);
        try {
            com.hypixel.hytale.server.core.permissions.PermissionsModule permissions =
                com.hypixel.hytale.server.core.permissions.PermissionsModule.get();
            if (permissions != null) {
                return permissions.hasPermission(ref.getUuid(), "verbatim.admin");
            }
        } catch (Exception e) {
            // PermissionsModule not available
        }
        return false;
    }

    // === Component Creation ===

    @Override
    public GameComponent createText(String text) {
        if (text == null) {
            return HytaleGameComponentImpl.empty();
        }
        return HytaleGameComponentImpl.literal(text);
    }

    @Override
    public GameComponent createEmpty() {
        return HytaleGameComponentImpl.empty();
    }

    @Override
    public GameComponent createInfoPrefix() {
        return HytaleGameComponentImpl.literal("[!] ")
            .withColor(world.landfall.verbatim.context.GameColor.LIGHT_PURPLE);
    }

    @Override
    public GameComponent createWarningPrefix() {
        return HytaleGameComponentImpl.literal("[!] ")
            .withColor(world.landfall.verbatim.context.GameColor.GOLD);
    }

    // === Command Response ===

    @Override
    public void sendCommandSuccess(GameCommandSource source, GameComponent message, boolean broadcast) {
        if (source == null || message == null) {
            return;
        }
        HytaleGameCommandSource hSource = (HytaleGameCommandSource) source;
        hSource.getHandle().sendMessage(((HytaleGameComponentImpl) message).toHytale());
    }

    @Override
    public void sendCommandFailure(GameCommandSource source, GameComponent message) {
        if (source == null || message == null) {
            return;
        }
        HytaleGameCommandSource hSource = (HytaleGameCommandSource) source;
        Message errorMsg = ((HytaleGameComponentImpl) message).toHytale()
            .color(java.awt.Color.RED);
        hSource.getHandle().sendMessage(errorMsg);
    }

    // === Discord Integration ===

    @Override
    public String getPlayerAvatarUrl(GamePlayer player) {
        if (player == null) {
            return "";
        }
        // Hytale uses crafthead.net with UUID for avatars
        return "https://crafthead.net/hytale/avatar/" + player.getUUID().toString();
    }
}
