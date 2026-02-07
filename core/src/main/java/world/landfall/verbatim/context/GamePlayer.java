package world.landfall.verbatim.context;

import java.util.UUID;

/**
 * Platform-independent player abstraction.
 * Replaces direct ServerPlayer references in business logic.
 */
public interface GamePlayer {
    String getUsername();
    String getDisplayName();
    UUID getUUID();
    GameComponent getDisplayNameComponent();
    GameComponent getNameComponent();
}
