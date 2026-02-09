package world.landfall.verbatim.platform.forge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Forge 1.20.1 implementation of GameCommandSource wrapping CommandSourceStack.
 */
public class ForgeGameCommandSource implements GameCommandSource {

    private final CommandSourceStack handle;

    public ForgeGameCommandSource(CommandSourceStack handle) {
        this.handle = Objects.requireNonNull(handle, "CommandSourceStack handle must not be null");
    }

    public CommandSourceStack getHandle() {
        return handle;
    }

    @Override
    public Optional<GamePlayer> asPlayer() {
        if (handle.getEntity() instanceof ServerPlayer player) {
            return Optional.of(new ForgeGamePlayer(player));
        }
        return Optional.empty();
    }

    @Override
    public boolean hasPermission(int level) {
        return handle.hasPermission(level);
    }
}
