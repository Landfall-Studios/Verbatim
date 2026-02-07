package world.landfall.verbatim.context;

import java.util.Optional;

/**
 * Platform-independent command source abstraction.
 * Replaces direct CommandSourceStack references in business logic.
 */
public interface GameCommandSource {
    Optional<GamePlayer> asPlayer();
    boolean hasPermission(int level);
}
