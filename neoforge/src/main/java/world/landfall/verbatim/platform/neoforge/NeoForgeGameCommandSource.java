package world.landfall.verbatim.platform.neoforge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * NeoForge implementation of GameCommandSource wrapping CommandSourceStack.
 */
public class NeoForgeGameCommandSource implements GameCommandSource {

    private final CommandSourceStack handle;

    public NeoForgeGameCommandSource(CommandSourceStack handle) {
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
        if (handle.getEntity() instanceof ServerPlayer player) {
            return Optional.of(new NeoForgeGamePlayer(player));
        }
        return Optional.empty();
    }

    @Override
    public boolean hasPermission(int level) {
        return handle.hasPermission(level);
    }
}
