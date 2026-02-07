package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Hytale implementation of GameCommandSource wrapping CommandContext.
 *
 * In Hytale's command system, commands that extend AbstractPlayerCommand
 * always have a PlayerRef available. For console commands, asPlayer() returns empty.
 */
public class HytaleGameCommandSource implements GameCommandSource {

    private final CommandContext handle;
    private final PlayerRef playerRef;

    /**
     * Creates a command source from a CommandContext and optional PlayerRef.
     *
     * @param handle The Hytale command context
     * @param playerRef The player who executed the command, or null for console
     */
    public HytaleGameCommandSource(CommandContext handle, PlayerRef playerRef) {
        this.handle = Objects.requireNonNull(handle, "CommandContext handle must not be null");
        this.playerRef = playerRef;
    }

    /**
     * Gets the underlying CommandContext. Only call from platform layer code.
     */
    public CommandContext getHandle() {
        return handle;
    }

    /**
     * Gets the underlying PlayerRef, if present. Only call from platform layer code.
     */
    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Override
    public Optional<GamePlayer> asPlayer() {
        if (playerRef != null) {
            return Optional.of(new HytaleGamePlayer(playerRef));
        }
        return Optional.empty();
    }

    @Override
    public boolean hasPermission(int level) {
        // Hytale uses string-based permissions via context.sender().hasPermission(String).
        // For numeric OP level compatibility, we check if the sender has operator status.
        // Level 0 = everyone, level 2+ = operator equivalent.
        if (level <= 0) {
            return true;
        }
        if (playerRef != null) {
            return handle.sender().hasPermission("verbatim.admin");
        }
        // Console always has permission
        return true;
    }
}
