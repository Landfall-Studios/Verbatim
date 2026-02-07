package world.landfall.verbatim.context;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Interface defining game context operations.
 * Platform-independent - no Minecraft types in signatures.
 */
public interface GameContext {

    // === Server Operations ===

    /**
     * Checks whether the server is currently available.
     */
    boolean isServerAvailable();

    /**
     * Gets all online players.
     */
    List<GamePlayer> getAllOnlinePlayers();

    /**
     * Gets the count of online players.
     */
    int getOnlinePlayerCount();

    // === Player Retrieval ===

    /**
     * Gets a player by their UUID.
     * @return the player, or null if not online
     */
    GamePlayer getPlayerByUUID(UUID uuid);

    /**
     * Gets a player by their name.
     * @return the player, or null if not online
     */
    GamePlayer getPlayerByName(String name);

    // === Distance and Position ===

    /**
     * Gets the squared distance between two players.
     */
    double getDistanceSquared(GamePlayer player1, GamePlayer player2);

    // === Messaging ===

    /**
     * Sends a system message to a specific player using GameComponent.
     */
    void sendMessage(GamePlayer player, GameComponent message);

    /**
     * Broadcasts a system message to all online players using GameComponent.
     */
    void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers);

    // === Persistent Data ===

    /**
     * Checks if a player has persistent data with the given key.
     */
    boolean hasPlayerData(GamePlayer player, String key);

    /**
     * Gets a string value from player's persistent data.
     */
    String getPlayerStringData(GamePlayer player, String key);

    /**
     * Sets a string value in player's persistent data.
     */
    void setPlayerStringData(GamePlayer player, String key, String value);

    /**
     * Removes a key from player's persistent data.
     */
    void removePlayerData(GamePlayer player, String key);

    // === Permissions ===

    /**
     * Checks if a player has a specific permission level (vanilla OP level).
     */
    boolean hasPermissionLevel(GamePlayer player, int level);

    // === Component Creation ===

    /**
     * Creates a literal text component.
     */
    GameComponent createText(String text);

    /**
     * Creates an empty component.
     */
    GameComponent createEmpty();

    // === Platform-Specific Symbols ===

    /**
     * Creates an info/notification prefix component.
     * Minecraft: 🗨️ (speech balloon emoji)
     * Hytale: [!] in light purple
     */
    GameComponent createInfoPrefix();

    /**
     * Creates a warning prefix component.
     * Minecraft: ⚠ (warning emoji)
     * Hytale: [!] in gold
     */
    GameComponent createWarningPrefix();

    // === Command Response ===

    /**
     * Sends a success message in response to a command.
     */
    void sendCommandSuccess(GameCommandSource source, GameComponent message, boolean broadcast);

    /**
     * Sends a failure message in response to a command.
     */
    void sendCommandFailure(GameCommandSource source, GameComponent message);

    // === Data Directory ===

    /**
     * Gets the platform-specific data directory for Verbatim.
     * Used by services like MailService to store persistent files.
     */
    Path getDataDirectory();

    // === Discord Integration ===

    /**
     * Gets the avatar URL for a player for use in Discord embeds.
     * Minecraft: Uses minotar.net with username
     * Hytale: Uses crafthead.net/hytale with UUID
     */
    String getPlayerAvatarUrl(GamePlayer player);
}
