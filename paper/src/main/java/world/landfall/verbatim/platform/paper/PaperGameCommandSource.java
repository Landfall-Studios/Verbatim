package world.landfall.verbatim.platform.paper;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Paper implementation of GameCommandSource wrapping Paper's CommandSourceStack.
 */
@SuppressWarnings("UnstableApiUsage")
public class PaperGameCommandSource implements GameCommandSource {

    private final CommandSourceStack handle;

    public PaperGameCommandSource(CommandSourceStack handle) {
        this.handle = Objects.requireNonNull(handle, "CommandSourceStack handle must not be null");
    }

    /**
     * Gets the underlying CommandSourceStack. Only call from platform layer code.
     */
    public CommandSourceStack getHandle() {
        return handle;
    }

    @Override
    public Optional<GamePlayer> asPlayer() {
        if (handle.getSender() instanceof Player player) {
            return Optional.of(new PaperGamePlayer(player));
        }
        return Optional.empty();
    }

    @Override
    public boolean hasPermission(int level) {
        if (level <= 0) {
            return true;
        }
        if (handle.getSender() instanceof Player player) {
            // Level 2+ maps to isOp() as a fallback (same as NeoForge)
            return player.isOp();
        }
        // Console always has permission
        return true;
    }
}
