package world.landfall.verbatim.platform.neoforge;

import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.context.GameComponent;
// NeoForgeGameComponentImpl is in this package
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * NeoForge implementation of GamePlayer wrapping a ServerPlayer.
 */
public class NeoForgeGamePlayer implements GamePlayer {

    private final ServerPlayer handle;

    public NeoForgeGamePlayer(ServerPlayer handle) {
        this.handle = Objects.requireNonNull(handle, "ServerPlayer handle must not be null");
    }

    /**
     * Gets the underlying ServerPlayer. Only call from platform layer code.
     */
    public ServerPlayer getHandle() {
        return handle;
    }

    @Override
    public String getUsername() {
        return handle.getName().getString();
    }

    @Override
    public String getDisplayName() {
        return handle.getDisplayName().getString();
    }

    @Override
    public UUID getUUID() {
        return handle.getUUID();
    }

    @Override
    public GameComponent getDisplayNameComponent() {
        return NeoForgeGameComponentImpl.wrap(handle.getDisplayName());
    }

    @Override
    public GameComponent getNameComponent() {
        return NeoForgeGameComponentImpl.wrap(handle.getName());
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
        return "NeoForgeGamePlayer{" + getUsername() + ", " + getUUID() + "}";
    }
}
