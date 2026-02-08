package world.landfall.verbatim.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
 * Paper implementation of GameContext.
 * Uses Bukkit API for player access, messaging, and persistence.
 * Uses in-memory ConcurrentHashMap + file store for player data
 * (Bukkit's PersistentDataContainer only stores primitives for online players).
 */
public class PaperGameContextImpl implements GameContext {

    private final ConcurrentHashMap<String, String> persistentData = new ConcurrentHashMap<>();
    private PlayerFileStore fileStore;
    private Path dataDirectory;
    private final ConcurrentHashMap<UUID, String> onlinePlayerUsernames = new ConcurrentHashMap<>();

    public static Player unwrap(GamePlayer player) {
        if (player instanceof PaperGamePlayer pp) {
            return pp.getHandle();
        }
        throw new IllegalArgumentException("GamePlayer is not a PaperGamePlayer: " + player);
    }

    public void setFileStore(PlayerFileStore fileStore) {
        this.fileStore = fileStore;
    }

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
            return Bukkit.getServer() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        try {
            return Bukkit.getOnlinePlayers().stream()
                .map(PaperGamePlayer::new)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public int getOnlinePlayerCount() {
        try {
            return Bukkit.getOnlinePlayers().size();
        } catch (Exception e) {
            return 0;
        }
    }

    // === Player Retrieval ===

    @Override
    public GamePlayer getPlayerByUUID(UUID uuid) {
        if (uuid == null) return null;
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? new PaperGamePlayer(player) : null;
    }

    @Override
    public GamePlayer getPlayerByName(String name) {
        if (name == null) return null;
        Player player = Bukkit.getPlayerExact(name);
        return player != null ? new PaperGamePlayer(player) : null;
    }

    // === Distance and Position ===

    @Override
    public double getDistanceSquared(GamePlayer player1, GamePlayer player2) {
        if (player1 == null || player2 == null) {
            return Double.MAX_VALUE;
        }
        Player bp1 = unwrap(player1);
        Player bp2 = unwrap(player2);

        // Same-world guard: cross-world distance is infinite
        if (!bp1.getWorld().equals(bp2.getWorld())) {
            return Double.MAX_VALUE;
        }

        Location loc1 = bp1.getLocation();
        Location loc2 = bp2.getLocation();
        return loc1.distanceSquared(loc2);
    }

    // === Messaging ===

    @Override
    public void sendMessage(GamePlayer player, GameComponent message) {
        if (player == null || message == null) return;
        Player bp = unwrap(player);
        bp.sendMessage(((PaperGameComponentImpl) message).toAdventure());
    }

    @Override
    public void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers) {
        if (message == null) return;
        Component adventureMsg = ((PaperGameComponentImpl) message).toAdventure();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(adventureMsg);
        }
    }

    // === Persistent Data ===

    private String dataKey(GamePlayer player, String key) {
        return player.getUUID().toString() + ":" + key;
    }

    @Override
    public boolean hasPlayerData(GamePlayer player, String key) {
        if (player == null || key == null) return false;
        return persistentData.containsKey(dataKey(player, key));
    }

    @Override
    public String getPlayerStringData(GamePlayer player, String key) {
        if (player == null || key == null) return "";
        return persistentData.getOrDefault(dataKey(player, key), "");
    }

    @Override
    public void setPlayerStringData(GamePlayer player, String key, String value) {
        if (player == null || key == null || value == null) return;
        persistentData.put(dataKey(player, key), value);
        schedulePlayerSave(player);
    }

    @Override
    public void removePlayerData(GamePlayer player, String key) {
        if (player == null || key == null) return;
        persistentData.remove(dataKey(player, key));
        schedulePlayerSave(player);
    }

    // === Per-Player Persistence ===

    private void schedulePlayerSave(GamePlayer player) {
        if (fileStore == null) return;
        UUID uuid = player.getUUID();
        String prefix = uuid.toString() + ":";
        Map<String, String> playerData = new HashMap<>();
        for (Map.Entry<String, String> entry : persistentData.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                playerData.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        String username = onlinePlayerUsernames.getOrDefault(uuid, player.getUsername());
        fileStore.savePlayer(uuid, username, playerData);
    }

    public void loadPlayerFromDisk(UUID uuid) {
        if (fileStore == null) return;
        Map<String, String> data = fileStore.loadPlayer(uuid);
        String prefix = uuid.toString() + ":";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            persistentData.put(prefix + entry.getKey(), entry.getValue());
        }
        if (!data.isEmpty()) {
            Verbatim.LOGGER.info("[Verbatim] Loaded {} data entries for player {}", data.size(), uuid);
        }
    }

    public void saveAllPlayersToDisk() {
        if (fileStore == null) return;

        Map<UUID, Map<String, String>> grouped = new HashMap<>();
        for (Map.Entry<String, String> entry : persistentData.entrySet()) {
            int colonIdx = entry.getKey().indexOf(':');
            if (colonIdx < 0) continue;
            String uuidStr = entry.getKey().substring(0, colonIdx);
            String dataKey = entry.getKey().substring(colonIdx + 1);
            try {
                UUID uuid = UUID.fromString(uuidStr);
                grouped.computeIfAbsent(uuid, k -> new HashMap<>()).put(dataKey, entry.getValue());
            } catch (IllegalArgumentException e) {
                Verbatim.LOGGER.warn("[Verbatim] Skipping entry with invalid UUID key: {}", entry.getKey());
            }
        }

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

    public void trackPlayerOnline(UUID uuid, String username) {
        onlinePlayerUsernames.put(uuid, username);
    }

    public void trackPlayerOffline(UUID uuid) {
        onlinePlayerUsernames.remove(uuid);
    }

    // === Permissions ===

    @Override
    public boolean hasPermissionLevel(GamePlayer player, int level) {
        if (player == null) return false;
        if (level <= 0) return true;
        // Map permission levels to isOp() for level 2+ (same as NeoForge fallback)
        Player bp = unwrap(player);
        return bp.isOp();
    }

    // === Component Creation ===

    @Override
    public GameComponent createText(String text) {
        if (text == null) return PaperGameComponentImpl.empty();
        return PaperGameComponentImpl.literal(text);
    }

    @Override
    public GameComponent createEmpty() {
        return PaperGameComponentImpl.empty();
    }

    @Override
    public GameComponent createInfoPrefix() {
        // Speech balloon emoji for Minecraft clients
        return PaperGameComponentImpl.literal("\uD83D\uDDE8 ");
    }

    @Override
    public GameComponent createWarningPrefix() {
        // Warning emoji for Minecraft clients
        return PaperGameComponentImpl.literal("\u26A0 ");
    }

    // === Command Response ===

    @Override
    public void sendCommandSuccess(GameCommandSource source, GameComponent message, boolean broadcast) {
        if (source == null || message == null) return;
        @SuppressWarnings("UnstableApiUsage")
        io.papermc.paper.command.brigadier.CommandSourceStack css =
            ((PaperGameCommandSource) source).getHandle();
        css.getSender().sendMessage(((PaperGameComponentImpl) message).toAdventure());
    }

    @Override
    public void sendCommandFailure(GameCommandSource source, GameComponent message) {
        if (source == null || message == null) return;
        @SuppressWarnings("UnstableApiUsage")
        io.papermc.paper.command.brigadier.CommandSourceStack css =
            ((PaperGameCommandSource) source).getHandle();
        // Prefix with red color for error messages
        Component errorMsg = ((PaperGameComponentImpl) message).toAdventure()
            .color(TextColor.color(0xFF5555));
        css.getSender().sendMessage(errorMsg);
    }

    // === Discord Integration ===

    @Override
    public String getPlayerAvatarUrl(GamePlayer player) {
        if (player == null) return "";
        return "https://minotar.net/helm/" + player.getUsername() + "/100.png";
    }
}
