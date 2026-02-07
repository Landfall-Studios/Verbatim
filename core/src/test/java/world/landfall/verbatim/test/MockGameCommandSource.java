package world.landfall.verbatim.test;

import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Optional;

/**
 * Mock GameCommandSource for unit testing.
 */
public class MockGameCommandSource implements GameCommandSource {
    private final MockGamePlayer player;
    private final int permissionLevel;

    public MockGameCommandSource(MockGamePlayer player) {
        this(player, 0);
    }

    public MockGameCommandSource(MockGamePlayer player, int permissionLevel) {
        this.player = player;
        this.permissionLevel = permissionLevel;
    }

    @Override
    public Optional<GamePlayer> asPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public boolean hasPermission(int level) {
        return permissionLevel >= level;
    }
}
