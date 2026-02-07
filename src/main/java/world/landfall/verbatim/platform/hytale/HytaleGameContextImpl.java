package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GameContext;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Collections;
import java.util.List;
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
     * so we use a simple in-memory map (backed by file persistence in HytaleEntryPoint).
     */
    private final ConcurrentHashMap<String, String> persistentData = new ConcurrentHashMap<>();

    /**
     * Unwraps a GamePlayer to a PlayerRef. For use in platform layer only.
     */
    public static PlayerRef unwrap(GamePlayer player) {
        if (player instanceof HytaleGamePlayer hp) {
            return hp.getHandle();
        }
        throw new IllegalArgumentException("GamePlayer is not a HytaleGamePlayer: " + player);
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
        return persistentData.containsKey(dataKey(player, key));
    }

    @Override
    public String getPlayerStringData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return "";
        }
        return persistentData.getOrDefault(dataKey(player, key), "");
    }

    @Override
    public void setPlayerStringData(GamePlayer player, String key, String value) {
        if (player == null || key == null || value == null) {
            return;
        }
        persistentData.put(dataKey(player, key), value);
    }

    @Override
    public void removePlayerData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return;
        }
        persistentData.remove(dataKey(player, key));
    }

    /**
     * Gets the persistent data map for serialization by the entry point.
     */
    public ConcurrentHashMap<String, String> getPersistentDataMap() {
        return persistentData;
    }

    /**
     * Loads persistent data from an external source (called on startup).
     */
    public void loadPersistentData(ConcurrentHashMap<String, String> data) {
        persistentData.putAll(data);
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
        // we check a generic admin permission node.
        // The PermissionService with LuckPerms will handle this in most cases.
        PlayerRef ref = unwrap(player);
        try {
            // Check if the player has the admin permission through the context system
            return false; // Default to false; LuckPerms fallback handles this
        } catch (Exception e) {
            return false;
        }
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
}
