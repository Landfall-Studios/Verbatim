package world.landfall.verbatim.context;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of GameContext for NeoForge Minecraft.
 *
 * Contains all the Minecraft-specific logic. If Minecraft APIs change in a future version,
 * only this class needs to be modified, not every class that uses these operations.
 */
public class GameContextImpl implements GameContext {

    // === Server Operations ===

    @Override
    public MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public List<ServerPlayer> getAllOnlinePlayers() {
        MinecraftServer server = getServer();
        if (server == null) {
            return Collections.emptyList();
        }
        return server.getPlayerList().getPlayers();
    }

    @Override
    public int getOnlinePlayerCount() {
        MinecraftServer server = getServer();
        if (server == null) {
            return 0;
        }
        return server.getPlayerList().getPlayerCount();
    }

    // === Player Retrieval ===

    @Override
    public ServerPlayer getPlayerByUUID(UUID uuid) {
        MinecraftServer server = getServer();
        if (server == null || uuid == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(uuid);
    }

    @Override
    public ServerPlayer getPlayerByName(String name) {
        MinecraftServer server = getServer();
        if (server == null || name == null) {
            return null;
        }
        return server.getPlayerList().getPlayerByName(name);
    }

    // === Player Information ===

    @Override
    public String getPlayerUsername(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return player.getName().getString();
    }

    @Override
    public String getPlayerDisplayName(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return player.getDisplayName().getString();
    }

    @Override
    public UUID getPlayerUUID(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return player.getUUID();
    }

    @Override
    public Component getPlayerDisplayNameComponent(ServerPlayer player) {
        if (player == null) {
            return Component.empty();
        }
        return player.getDisplayName();
    }

    @Override
    public Component getPlayerNameComponent(ServerPlayer player) {
        if (player == null) {
            return Component.empty();
        }
        return player.getName();
    }

    // === Distance and Position ===

    @Override
    public double getDistanceSquared(ServerPlayer player1, ServerPlayer player2) {
        if (player1 == null || player2 == null) {
            return Double.MAX_VALUE;
        }
        return player1.distanceToSqr(player2);
    }

    // === Messaging ===

    @Override
    public void sendSystemMessage(ServerPlayer player, Component message) {
        if (player == null || message == null) {
            return;
        }
        player.sendSystemMessage(message);
    }

    @Override
    public void sendMessage(ServerPlayer player, GameComponent message) {
        if (player == null || message == null) {
            return;
        }
        player.sendSystemMessage(message.toMinecraft());
    }

    @Override
    public void broadcastSystemMessage(Component message, boolean bypassHiddenPlayers) {
        MinecraftServer server = getServer();
        if (server == null || message == null) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(message, bypassHiddenPlayers);
    }

    @Override
    public void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers) {
        MinecraftServer server = getServer();
        if (server == null || message == null) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(message.toMinecraft(), bypassHiddenPlayers);
    }

    // === Persistent Data ===

    @Override
    public boolean hasPlayerData(ServerPlayer player, String key) {
        if (player == null || key == null) {
            return false;
        }
        return player.getPersistentData().contains(key);
    }

    @Override
    public String getPlayerStringData(ServerPlayer player, String key) {
        if (player == null || key == null) {
            return "";
        }
        return player.getPersistentData().getString(key);
    }

    @Override
    public void setPlayerStringData(ServerPlayer player, String key, String value) {
        if (player == null || key == null || value == null) {
            return;
        }
        player.getPersistentData().putString(key, value);
    }

    @Override
    public void removePlayerData(ServerPlayer player, String key) {
        if (player == null || key == null) {
            return;
        }
        player.getPersistentData().remove(key);
    }

    // === Permissions ===

    @Override
    public boolean hasPermissionLevel(ServerPlayer player, int level) {
        if (player == null) {
            return false;
        }
        return player.hasPermissions(level);
    }

    // === Component Creation ===

    @Override
    public GameComponent createText(String text) {
        if (text == null) {
            return GameComponentImpl.empty();
        }
        return GameComponentImpl.literal(text);
    }

    @Override
    public GameComponent createEmpty() {
        return GameComponentImpl.empty();
    }

    @Override
    public GameComponent wrapComponent(Component component) {
        if (component == null) {
            return GameComponentImpl.empty();
        }
        return GameComponentImpl.wrap(component);
    }

    // === Entity Type Checking ===

    @Override
    public boolean isServerPlayer(Object entity) {
        return entity instanceof ServerPlayer;
    }

    @Override
    public Optional<ServerPlayer> asServerPlayer(Object entity) {
        if (entity instanceof ServerPlayer player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    // === Command Response ===

    @Override
    public void sendCommandSuccess(CommandSourceStack source, GameComponent message, boolean broadcast) {
        if (source == null || message == null) {
            return;
        }
        source.sendSuccess(() -> message.toMinecraft(), broadcast);
    }

    @Override
    public void sendCommandFailure(CommandSourceStack source, GameComponent message) {
        if (source == null || message == null) {
            return;
        }
        source.sendFailure(message.toMinecraft());
    }
}
