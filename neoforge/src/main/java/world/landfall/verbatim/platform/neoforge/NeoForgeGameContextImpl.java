package world.landfall.verbatim.platform.neoforge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GameContext;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NeoForge implementation of GameContext.
 */
public class NeoForgeGameContextImpl implements GameContext {

    private MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * Unwraps a GamePlayer to a ServerPlayer. For use in platform layer only.
     */
    public static ServerPlayer unwrap(GamePlayer player) {
        if (player instanceof NeoForgeGamePlayer nfp) {
            return nfp.getHandle();
        }
        throw new IllegalArgumentException("GamePlayer is not a NeoForgeGamePlayer: " + player);
    }

    // === Server Operations ===

    @Override
    public boolean isServerAvailable() {
        return getServer() != null;
    }

    @Override
    public List<GamePlayer> getAllOnlinePlayers() {
        MinecraftServer server = getServer();
        if (server == null) {
            return Collections.emptyList();
        }
        return server.getPlayerList().getPlayers().stream()
            .map(NeoForgeGamePlayer::new)
            .collect(Collectors.toList());
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
    public GamePlayer getPlayerByUUID(UUID uuid) {
        MinecraftServer server = getServer();
        if (server == null || uuid == null) {
            return null;
        }
        ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
        return sp != null ? new NeoForgeGamePlayer(sp) : null;
    }

    @Override
    public GamePlayer getPlayerByName(String name) {
        MinecraftServer server = getServer();
        if (server == null || name == null) {
            return null;
        }
        ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
        return sp != null ? new NeoForgeGamePlayer(sp) : null;
    }

    // === Distance and Position ===

    @Override
    public double getDistanceSquared(GamePlayer player1, GamePlayer player2) {
        if (player1 == null || player2 == null) {
            return Double.MAX_VALUE;
        }
        ServerPlayer sp1 = unwrap(player1);
        ServerPlayer sp2 = unwrap(player2);
        return sp1.distanceToSqr(sp2);
    }

    // === Messaging ===

    @Override
    public void sendMessage(GamePlayer player, GameComponent message) {
        if (player == null || message == null) {
            return;
        }
        ServerPlayer sp = unwrap(player);
        sp.sendSystemMessage(((NeoForgeGameComponentImpl) message).toMinecraft());
    }

    @Override
    public void broadcastMessage(GameComponent message, boolean bypassHiddenPlayers) {
        MinecraftServer server = getServer();
        if (server == null || message == null) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(((NeoForgeGameComponentImpl) message).toMinecraft(), bypassHiddenPlayers);
    }

    // === Persistent Data ===

    @Override
    public boolean hasPlayerData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return false;
        }
        return unwrap(player).getPersistentData().contains(key);
    }

    @Override
    public String getPlayerStringData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return "";
        }
        return unwrap(player).getPersistentData().getString(key);
    }

    @Override
    public void setPlayerStringData(GamePlayer player, String key, String value) {
        if (player == null || key == null || value == null) {
            return;
        }
        unwrap(player).getPersistentData().putString(key, value);
    }

    @Override
    public void removePlayerData(GamePlayer player, String key) {
        if (player == null || key == null) {
            return;
        }
        unwrap(player).getPersistentData().remove(key);
    }

    // === Permissions ===

    @Override
    public boolean hasPermissionLevel(GamePlayer player, int level) {
        if (player == null) {
            return false;
        }
        return unwrap(player).hasPermissions(level);
    }

    // === Component Creation ===

    @Override
    public GameComponent createText(String text) {
        if (text == null) {
            return NeoForgeGameComponentImpl.empty();
        }
        return NeoForgeGameComponentImpl.literal(text);
    }

    @Override
    public GameComponent createEmpty() {
        return NeoForgeGameComponentImpl.empty();
    }

    @Override
    public GameComponent createInfoPrefix() {
        // Speech balloon emoji for Minecraft
        return NeoForgeGameComponentImpl.literal("\uD83D\uDDE8 ");
    }

    @Override
    public GameComponent createWarningPrefix() {
        // Warning emoji for Minecraft
        return NeoForgeGameComponentImpl.literal("\u26A0 ");
    }

    // === Command Response ===

    @Override
    public void sendCommandSuccess(GameCommandSource source, GameComponent message, boolean broadcast) {
        if (source == null || message == null) {
            return;
        }
        CommandSourceStack css = ((NeoForgeGameCommandSource) source).getHandle();
        css.sendSuccess(() -> ((NeoForgeGameComponentImpl) message).toMinecraft(), broadcast);
    }

    @Override
    public void sendCommandFailure(GameCommandSource source, GameComponent message) {
        if (source == null || message == null) {
            return;
        }
        CommandSourceStack css = ((NeoForgeGameCommandSource) source).getHandle();
        css.sendFailure(((NeoForgeGameComponentImpl) message).toMinecraft());
    }

    // === Discord Integration ===

    @Override
    public String getPlayerAvatarUrl(GamePlayer player) {
        if (player == null) {
            return "";
        }
        // Minecraft uses minotar.net with username for avatars
        return "https://minotar.net/helm/" + player.getUsername() + "/100.png";
    }
}
