package world.landfall.verbatim.context;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Interface defining game context operations.
 *
 * This abstraction layer provides a single point of change for Minecraft API interactions.
 * Benefits:
 * - Single point of change when Minecraft APIs change between versions
 * - Testability - can create mock implementations for testing
 * - Documentation - clearly shows all game interactions in one place
 * - Swappable - could have different implementations for different Minecraft versions
 */
public interface GameContext {

    // === Server Operations ===

    /**
     * Gets the current Minecraft server instance.
     * @return the server, or null if not available
     */
    MinecraftServer getServer();

    /**
     * Gets all online players.
     * @return list of all online players
     */
    List<ServerPlayer> getAllOnlinePlayers();

    /**
     * Gets the count of online players.
     * @return number of online players
     */
    int getOnlinePlayerCount();

    // === Player Retrieval ===

    /**
     * Gets a player by their UUID.
     * @param uuid the player's UUID
     * @return the player, or null if not online
     */
    ServerPlayer getPlayerByUUID(UUID uuid);

    /**
     * Gets a player by their name.
     * @param name the player's name
     * @return the player, or null if not online
     */
    ServerPlayer getPlayerByName(String name);

    // === Player Information ===

    /**
     * Gets a player's username (account name).
     * @param player the player
     * @return the username string
     */
    String getPlayerUsername(ServerPlayer player);

    /**
     * Gets a player's display name (may include formatting).
     * @param player the player
     * @return the display name string
     */
    String getPlayerDisplayName(ServerPlayer player);

    /**
     * Gets a player's UUID.
     * @param player the player
     * @return the player's UUID
     */
    UUID getPlayerUUID(ServerPlayer player);

    /**
     * Gets a player's display name as a Component.
     * @param player the player
     * @return the display name component
     */
    Component getPlayerDisplayNameComponent(ServerPlayer player);

    /**
     * Gets a player's name as a Component.
     * @param player the player
     * @return the name component
     */
    Component getPlayerNameComponent(ServerPlayer player);

    // === Distance and Position ===

    /**
     * Gets the squared distance between two players.
     * @param player1 first player
     * @param player2 second player
     * @return squared distance
     */
    double getDistanceSquared(ServerPlayer player1, ServerPlayer player2);

    // === Messaging ===

    /**
     * Sends a system message to a specific player.
     * @param player the player to send to
     * @param message the message component
     */
    void sendSystemMessage(ServerPlayer player, net.minecraft.network.chat.Component message);

    /**
     * Sends a system message to a specific player using GameComponent.
     * @param player the player to send to
     * @param message the game component message
     */
    void sendMessage(ServerPlayer player, GameComponent message);

    /**
     * Broadcasts a system message to all online players.
     * @param message the message component
     * @param bypassHiddenPlayers whether to bypass hidden players filter
     */
    void broadcastSystemMessage(net.minecraft.network.chat.Component message, boolean bypassHiddenPlayers);

    /**
     * Broadcasts a system message to all online players using GameComponent.
     * @param message the game component message
     * @param bypassHiddenPlayers whether to bypass hidden players filter
     */
    void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers);

    // === Persistent Data ===

    /**
     * Checks if a player has persistent data with the given key.
     * @param player the player
     * @param key the data key
     * @return true if the data exists
     */
    boolean hasPlayerData(ServerPlayer player, String key);

    /**
     * Gets a string value from player's persistent data.
     * @param player the player
     * @param key the data key
     * @return the string value, or empty string if not found
     */
    String getPlayerStringData(ServerPlayer player, String key);

    /**
     * Sets a string value in player's persistent data.
     * @param player the player
     * @param key the data key
     * @param value the value to set
     */
    void setPlayerStringData(ServerPlayer player, String key, String value);

    /**
     * Removes a key from player's persistent data.
     * @param player the player
     * @param key the data key to remove
     */
    void removePlayerData(ServerPlayer player, String key);

    // === Permissions ===

    /**
     * Checks if a player has a specific permission level (vanilla OP level).
     * @param player the player
     * @param level the permission level (0-4)
     * @return true if the player has the permission level
     */
    boolean hasPermissionLevel(ServerPlayer player, int level);

    // === Component Creation ===

    /**
     * Creates a literal text component.
     * @param text the text content
     * @return the game component
     */
    GameComponent createText(String text);

    /**
     * Creates an empty component.
     * @return an empty game component
     */
    GameComponent createEmpty();

    /**
     * Wraps a Minecraft Component into a GameComponent.
     * @param component the Minecraft component
     * @return the wrapped game component
     */
    GameComponent wrapComponent(net.minecraft.network.chat.Component component);

    // === Entity Type Checking ===

    /**
     * Checks if an entity is a ServerPlayer.
     * @param entity the entity to check
     * @return true if the entity is a ServerPlayer
     */
    boolean isServerPlayer(Object entity);

    /**
     * Casts an entity to ServerPlayer if possible.
     * @param entity the entity to cast
     * @return Optional containing the player, or empty if not a ServerPlayer
     */
    Optional<ServerPlayer> asServerPlayer(Object entity);

    // === Command Response ===

    /**
     * Sends a success message in response to a command.
     * @param source the command source
     * @param message the message to send
     * @param broadcast whether to broadcast to other operators
     */
    void sendCommandSuccess(CommandSourceStack source, GameComponent message, boolean broadcast);

    /**
     * Sends a failure message in response to a command.
     * @param source the command source
     * @param message the message to send
     */
    void sendCommandFailure(CommandSourceStack source, GameComponent message);
}
