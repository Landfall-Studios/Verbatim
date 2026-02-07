package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Hytale implementation of GamePlayer wrapping a PlayerRef.
 */
public class HytaleGamePlayer implements GamePlayer {

    private final PlayerRef handle;

    public HytaleGamePlayer(PlayerRef handle) {
        this.handle = Objects.requireNonNull(handle, "PlayerRef handle must not be null");
    }

    /**
     * Gets the underlying PlayerRef. Only call from platform layer code.
     */
    public PlayerRef getHandle() {
        return handle;
    }

    @Override
    public String getUsername() {
        return handle.getUsername();
    }

    @Override
    public String getDisplayName() {
        // Hytale doesn't have a separate display name system like Minecraft;
        // returns username as the display name.
        return handle.getUsername();
    }

    @Override
    public UUID getUUID() {
        return handle.getUuid();
    }

    @Override
    public GameComponent getDisplayNameComponent() {
        return new HytaleGameComponentImpl(Message.raw(handle.getUsername()));
    }

    @Override
    public GameComponent getNameComponent() {
        return new HytaleGameComponentImpl(Message.raw(handle.getUsername()));
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
        return "HytaleGamePlayer{" + getUsername() + ", " + getUUID() + "}";
    }
}
