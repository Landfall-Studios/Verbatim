package world.landfall.verbatim.platform.forge;

import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Forge 1.20.1 implementation of GamePlayer wrapping a ServerPlayer.
 */
public class ForgeGamePlayer implements GamePlayer {

    private final ServerPlayer handle;

    public ForgeGamePlayer(ServerPlayer handle) {
        this.handle = Objects.requireNonNull(handle, "ServerPlayer handle must not be null");
    }

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
        return ForgeGameComponentImpl.wrap(handle.getDisplayName());
    }

    @Override
    public GameComponent getNameComponent() {
        return ForgeGameComponentImpl.wrap(handle.getName());
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
        return "ForgeGamePlayer{" + getUsername() + ", " + getUUID() + "}";
    }
}
