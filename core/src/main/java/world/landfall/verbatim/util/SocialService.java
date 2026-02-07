package world.landfall.verbatim.util;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SocialService {

    private static final String DATA_IGNORED_PLAYERS = "verbatim:ignored_players";
    private static final String DATA_FAVORITE_PLAYERS = "verbatim:favorite_players";
    private static final String DATA_FAV_META = "verbatim:fav_meta";

    private static final long BLOCK_NOTIFY_COOLDOWN_MS = 5 * 60 * 1000L; // 5 minutes

    private static final Map<UUID, Set<UUID>> ignoreCache = new HashMap<>();
    private static final Map<UUID, Set<UUID>> favoriteCache = new HashMap<>();
    private static final Map<UUID, Map<UUID, FavoriteMeta>> favoriteMetaCache = new HashMap<>();
    // Tracks last time a blocker was notified about a specific sender: blocker -> (sender -> timestamp)
    private static final Map<UUID, Map<UUID, Long>> blockNotifyCooldowns = new HashMap<>();

    public static class FavoriteMeta {
        public final String name;
        public final long lastSeen;

        public FavoriteMeta(String name, long lastSeen) {
            this.name = name;
            this.lastSeen = lastSeen;
        }
    }

    // === Ignore Methods ===

    public static void addIgnore(GamePlayer player, UUID targetUUID) {
        Set<UUID> ignored = loadIgnoreList(player);
        ignored.add(targetUUID);
        ignoreCache.put(player.getUUID(), ignored);
        saveIgnoreList(player);
    }

    public static void removeIgnore(GamePlayer player, UUID targetUUID) {
        Set<UUID> ignored = loadIgnoreList(player);
        ignored.remove(targetUUID);
        ignoreCache.put(player.getUUID(), ignored);
        saveIgnoreList(player);
    }

    public static Set<UUID> getIgnoredUUIDs(GamePlayer player) {
        return loadIgnoreList(player);
    }

    public static boolean isIgnoring(GamePlayer recipient, UUID senderUUID) {
        return loadIgnoreList(recipient).contains(senderUUID);
    }

    // === Favorite Methods ===

    public static void addFavorite(GamePlayer player, UUID targetUUID, String targetName) {
        Set<UUID> favorites = loadFavoriteList(player);
        favorites.add(targetUUID);
        favoriteCache.put(player.getUUID(), favorites);

        Map<UUID, FavoriteMeta> meta = loadFavoriteMeta(player);
        meta.put(targetUUID, new FavoriteMeta(targetName, System.currentTimeMillis()));
        favoriteMetaCache.put(player.getUUID(), meta);

        saveFavoriteList(player);
    }

    public static void removeFavorite(GamePlayer player, UUID targetUUID) {
        Set<UUID> favorites = loadFavoriteList(player);
        favorites.remove(targetUUID);
        favoriteCache.put(player.getUUID(), favorites);

        Map<UUID, FavoriteMeta> meta = loadFavoriteMeta(player);
        meta.remove(targetUUID);
        favoriteMetaCache.put(player.getUUID(), meta);

        saveFavoriteList(player);
    }

    public static Set<UUID> getFavoriteUUIDs(GamePlayer player) {
        return loadFavoriteList(player);
    }

    public static boolean isFavorited(GamePlayer player, UUID senderUUID) {
        return loadFavoriteList(player).contains(senderUUID);
    }

    public static Map<UUID, FavoriteMeta> getFavoriteMeta(GamePlayer player) {
        return loadFavoriteMeta(player);
    }

    public static void updateFavoriteMetaForPlayer(UUID targetUUID, String name, long lastSeen) {
        if (!Verbatim.gameContext.isServerAvailable()) return;
        for (GamePlayer onlinePlayer : Verbatim.gameContext.getAllOnlinePlayers()) {
            Set<UUID> favorites = favoriteCache.get(onlinePlayer.getUUID());
            if (favorites != null && favorites.contains(targetUUID)) {
                Map<UUID, FavoriteMeta> meta = favoriteMetaCache.computeIfAbsent(
                    onlinePlayer.getUUID(), k -> new HashMap<>());
                meta.put(targetUUID, new FavoriteMeta(name, lastSeen));
                saveFavoriteList(onlinePlayer);
            }
        }
    }

    // === Block Notification Cooldown ===

    /**
     * Returns true if the blocker should be notified about a blocked DM from this sender.
     * Rate-limited to once per 5 minutes per sender.
     */
    public static boolean shouldNotifyBlock(UUID blockerUUID, UUID senderUUID) {
        Map<UUID, Long> cooldowns = blockNotifyCooldowns.computeIfAbsent(blockerUUID, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        Long lastNotified = cooldowns.get(senderUUID);
        if (lastNotified != null && now - lastNotified < BLOCK_NOTIFY_COOLDOWN_MS) {
            return false;
        }
        cooldowns.put(senderUUID, now);
        return true;
    }

    // === Lifecycle ===

    public static void onPlayerLogout(UUID playerId) {
        ignoreCache.remove(playerId);
        favoriteCache.remove(playerId);
        favoriteMetaCache.remove(playerId);
        blockNotifyCooldowns.remove(playerId);
    }

    public static void reset() {
        ignoreCache.clear();
        favoriteCache.clear();
        favoriteMetaCache.clear();
        blockNotifyCooldowns.clear();
    }

    // === Private Helpers ===

    private static Set<UUID> loadIgnoreList(GamePlayer player) {
        UUID playerId = player.getUUID();
        if (ignoreCache.containsKey(playerId)) {
            return ignoreCache.get(playerId);
        }

        Set<UUID> ignored = new HashSet<>();
        if (Verbatim.gameContext.hasPlayerData(player, DATA_IGNORED_PLAYERS)) {
            String raw = Verbatim.gameContext.getPlayerStringData(player, DATA_IGNORED_PLAYERS);
            if (raw != null && !raw.isEmpty()) {
                for (String part : raw.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            ignored.add(UUID.fromString(trimmed));
                        } catch (IllegalArgumentException e) {
                            Verbatim.LOGGER.warn("[SocialService] Invalid UUID in ignore list for {}: {}", player.getUsername(), trimmed);
                        }
                    }
                }
            }
        }
        ignoreCache.put(playerId, ignored);
        return ignored;
    }

    private static void saveIgnoreList(GamePlayer player) {
        Set<UUID> ignored = ignoreCache.getOrDefault(player.getUUID(), new HashSet<>());
        if (ignored.isEmpty()) {
            Verbatim.gameContext.removePlayerData(player, DATA_IGNORED_PLAYERS);
        } else {
            StringBuilder sb = new StringBuilder();
            for (UUID uuid : ignored) {
                if (sb.length() > 0) sb.append(",");
                sb.append(uuid.toString());
            }
            Verbatim.gameContext.setPlayerStringData(player, DATA_IGNORED_PLAYERS, sb.toString());
        }
    }

    private static Set<UUID> loadFavoriteList(GamePlayer player) {
        UUID playerId = player.getUUID();
        if (favoriteCache.containsKey(playerId)) {
            return favoriteCache.get(playerId);
        }

        Set<UUID> favorites = new HashSet<>();
        if (Verbatim.gameContext.hasPlayerData(player, DATA_FAVORITE_PLAYERS)) {
            String raw = Verbatim.gameContext.getPlayerStringData(player, DATA_FAVORITE_PLAYERS);
            if (raw != null && !raw.isEmpty()) {
                for (String part : raw.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            favorites.add(UUID.fromString(trimmed));
                        } catch (IllegalArgumentException e) {
                            Verbatim.LOGGER.warn("[SocialService] Invalid UUID in favorite list for {}: {}", player.getUsername(), trimmed);
                        }
                    }
                }
            }
        }
        favoriteCache.put(playerId, favorites);

        // Also load meta
        loadFavoriteMeta(player);

        return favorites;
    }

    private static Map<UUID, FavoriteMeta> loadFavoriteMeta(GamePlayer player) {
        UUID playerId = player.getUUID();
        if (favoriteMetaCache.containsKey(playerId)) {
            return favoriteMetaCache.get(playerId);
        }

        Map<UUID, FavoriteMeta> meta = new HashMap<>();
        if (Verbatim.gameContext.hasPlayerData(player, DATA_FAV_META)) {
            String raw = Verbatim.gameContext.getPlayerStringData(player, DATA_FAV_META);
            if (raw != null && !raw.isEmpty()) {
                for (String entry : raw.split(",")) {
                    String[] parts = entry.split("\\|");
                    if (parts.length == 3) {
                        try {
                            UUID uuid = UUID.fromString(parts[0].trim());
                            String name = parts[1].trim();
                            long lastSeen = Long.parseLong(parts[2].trim());
                            meta.put(uuid, new FavoriteMeta(name, lastSeen));
                        } catch (Exception e) {
                            Verbatim.LOGGER.warn("[SocialService] Invalid fav_meta entry for {}: {}", player.getUsername(), entry);
                        }
                    }
                }
            }
        }
        favoriteMetaCache.put(playerId, meta);
        return meta;
    }

    private static void saveFavoriteList(GamePlayer player) {
        Set<UUID> favorites = favoriteCache.getOrDefault(player.getUUID(), new HashSet<>());
        if (favorites.isEmpty()) {
            Verbatim.gameContext.removePlayerData(player, DATA_FAVORITE_PLAYERS);
            Verbatim.gameContext.removePlayerData(player, DATA_FAV_META);
        } else {
            StringBuilder favSb = new StringBuilder();
            for (UUID uuid : favorites) {
                if (favSb.length() > 0) favSb.append(",");
                favSb.append(uuid.toString());
            }
            Verbatim.gameContext.setPlayerStringData(player, DATA_FAVORITE_PLAYERS, favSb.toString());

            Map<UUID, FavoriteMeta> meta = favoriteMetaCache.getOrDefault(player.getUUID(), new HashMap<>());
            StringBuilder metaSb = new StringBuilder();
            for (Map.Entry<UUID, FavoriteMeta> entry : meta.entrySet()) {
                if (metaSb.length() > 0) metaSb.append(",");
                metaSb.append(entry.getKey().toString())
                    .append("|").append(entry.getValue().name)
                    .append("|").append(entry.getValue().lastSeen);
            }
            if (metaSb.length() > 0) {
                Verbatim.gameContext.setPlayerStringData(player, DATA_FAV_META, metaSb.toString());
            } else {
                Verbatim.gameContext.removePlayerData(player, DATA_FAV_META);
            }
        }
    }
}
