package world.landfall.verbatim.test;

import world.landfall.verbatim.context.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock GameContext for unit testing.
 * Tracks sent messages and simulates player management.
 */
public class MockGameContext implements GameContext {
    private final Map<UUID, MockGamePlayer> players = new ConcurrentHashMap<>();
    private final Map<String, String> persistentData = new ConcurrentHashMap<>();
    private final List<SentMessage> sentMessages = new ArrayList<>();
    private final List<GameComponent> broadcasts = new ArrayList<>();
    private final Set<UUID> ops = new HashSet<>();

    public record SentMessage(GamePlayer player, GameComponent message) {}

    // Test setup methods
    public void addPlayer(MockGamePlayer player) {
        players.put(player.getUUID(), player);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public void removePlayer(GamePlayer player) {
        players.remove(player.getUUID());
    }

    public void setOp(UUID uuid, boolean op) {
        if (op) {
            ops.add(uuid);
        } else {
            ops.remove(uuid);
        }
    }

    public void clear() {
        players.clear();
        persistentData.clear();
        sentMessages.clear();
        broadcasts.clear();
        ops.clear();
    }

    // Test assertion helpers
    public List<SentMessage> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public List<SentMessage> getMessagesTo(GamePlayer player) {
        return sentMessages.stream()
            .filter(m -> m.player().getUUID().equals(player.getUUID()))
            .toList();
    }

    public List<GameComponent> getBroadcasts() {
        return Collections.unmodifiableList(broadcasts);
    }

    public void clearMessages() {
        sentMessages.clear();
        broadcasts.clear();
    }

    // GameContext implementation
    @Override
    public boolean isServerAvailable() {
        return true;
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        return new ArrayList<>(players.values());
    }

    @Override
    public int getOnlinePlayerCount() {
        return players.size();
    }

    @Override
    public GamePlayer getPlayerByUUID(UUID uuid) {
        return players.get(uuid);
    }

    @Override
    public GamePlayer getPlayerByName(String name) {
        return players.values().stream()
            .filter(p -> p.getUsername().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    @Override
    public double getDistanceSquared(GamePlayer player1, GamePlayer player2) {
        if (!(player1 instanceof MockGamePlayer p1) || !(player2 instanceof MockGamePlayer p2)) {
            return Double.MAX_VALUE;
        }
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        double dz = p1.getZ() - p2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void sendMessage(GamePlayer player, GameComponent message) {
        sentMessages.add(new SentMessage(player, message));
    }

    @Override
    public void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers) {
        broadcasts.add(message);
        for (GamePlayer player : players.values()) {
            sentMessages.add(new SentMessage(player, message));
        }
    }

    // Persistent data
    private String dataKey(GamePlayer player, String key) {
        return player.getUUID().toString() + ":" + key;
    }

    @Override
    public boolean hasPlayerData(GamePlayer player, String key) {
        return persistentData.containsKey(dataKey(player, key));
    }

    @Override
    public String getPlayerStringData(GamePlayer player, String key) {
        return persistentData.getOrDefault(dataKey(player, key), "");
    }

    @Override
    public void setPlayerStringData(GamePlayer player, String key, String value) {
        persistentData.put(dataKey(player, key), value);
    }

    @Override
    public void removePlayerData(GamePlayer player, String key) {
        persistentData.remove(dataKey(player, key));
    }

    @Override
    public boolean hasPermissionLevel(GamePlayer player, int level) {
        if (level <= 0) return true;
        return ops.contains(player.getUUID());
    }

    @Override
    public GameComponent createText(String text) {
        return new MockGameComponent(text);
    }

    @Override
    public GameComponent createEmpty() {
        return new MockGameComponent();
    }

    @Override
    public GameComponent createInfoPrefix() {
        return new MockGameComponent("[!] ").withColor(GameColor.LIGHT_PURPLE);
    }

    @Override
    public GameComponent createWarningPrefix() {
        return new MockGameComponent("[!] ").withColor(GameColor.GOLD);
    }

    @Override
    public void sendCommandSuccess(GameCommandSource source, GameComponent message, boolean broadcast) {
        // For testing, we just track it as a sent message
        source.asPlayer().ifPresent(player ->
            sentMessages.add(new SentMessage(player, message))
        );
    }

    @Override
    public void sendCommandFailure(GameCommandSource source, GameComponent message) {
        source.asPlayer().ifPresent(player ->
            sentMessages.add(new SentMessage(player, message))
        );
    }
}
