package world.landfall.verbatim.platform.paper;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Paper implementation of GamePlayer wrapping a Bukkit Player.
 */
public class PaperGamePlayer implements GamePlayer {

    private final Player handle;

    public PaperGamePlayer(Player handle) {
        this.handle = Objects.requireNonNull(handle, "Player handle must not be null");
    }

    /**
     * Gets the underlying Bukkit Player. Only call from platform layer code.
     */
    public Player getHandle() {
        return handle;
    }

    @Override
    public String getUsername() {
        return handle.getName();
    }

    @Override
    public String getDisplayName() {
        // Paper uses Adventure components for display name
        return PlainTextComponentSerializer.plainText().serialize(handle.displayName());
    }

    @Override
    public UUID getUUID() {
        return handle.getUniqueId();
    }

    @Override
    public GameComponent getDisplayNameComponent() {
        return PaperGameComponentImpl.wrap(handle.displayName());
    }

    @Override
    public GameComponent getNameComponent() {
        return PaperGameComponentImpl.literal(handle.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GamePlayer other)) return false;
        return getUUID().equals(other.getUUID());
    }

    @Override
    public int hashCode() {
        return getUUID().hashCode();
    }

    @Override
    public String toString() {
        return "PaperGamePlayer{" + getUsername() + ", " + getUUID() + "}";
    }
}
