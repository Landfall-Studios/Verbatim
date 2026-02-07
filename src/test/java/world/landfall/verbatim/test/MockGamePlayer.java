package world.landfall.verbatim.test;

import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;

import java.util.UUID;

/**
 * Mock GamePlayer for unit testing.
 */
public class MockGamePlayer implements GamePlayer {
    private final UUID uuid;
    private final String username;
    private final String displayName;
    private double x, y, z;

    public MockGamePlayer(String username) {
        this(UUID.randomUUID(), username, username);
    }

    public MockGamePlayer(String username, String displayName) {
        this(UUID.randomUUID(), username, displayName);
    }

    public MockGamePlayer(UUID uuid, String username, String displayName) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = displayName;
    }

    public MockGamePlayer atPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public GameComponent getDisplayNameComponent() {
        return Verbatim.gameContext.createText(displayName);
    }

    @Override
    public GameComponent getNameComponent() {
        return Verbatim.gameContext.createText(username);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GamePlayer other) {
            return uuid.equals(other.getUUID());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
