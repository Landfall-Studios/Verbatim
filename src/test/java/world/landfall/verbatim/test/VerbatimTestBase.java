package world.landfall.verbatim.test;

import org.junit.jupiter.api.BeforeEach;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;

/**
 * Base class for Verbatim unit tests.
 * Sets up mock implementations of all platform services.
 */
public abstract class VerbatimTestBase {
    protected MockGameContext gameContext;
    protected MockGameConfig gameConfig;
    protected MockChatFormatter chatFormatter;
    protected MockChannelFormatter channelFormatter;
    protected MockPermissionService permissionService;

    @BeforeEach
    public void setUp() {
        // Create fresh mocks
        gameContext = new MockGameContext();
        gameConfig = new MockGameConfig();
        chatFormatter = new MockChatFormatter();
        channelFormatter = new MockChannelFormatter();
        permissionService = new MockPermissionService();

        // Wire up Verbatim's service locator
        Verbatim.gameContext = gameContext;
        Verbatim.gameConfig = gameConfig;
        Verbatim.chatFormatter = chatFormatter;
        Verbatim.channelFormatter = channelFormatter;
        Verbatim.permissionService = permissionService;

        // Reset ChatChannelManager state
        ChatChannelManager.reset();
    }

    /**
     * Creates a test player and adds them to the game context.
     */
    protected MockGamePlayer createPlayer(String username) {
        MockGamePlayer player = new MockGamePlayer(username);
        gameContext.addPlayer(player);
        return player;
    }

    /**
     * Creates a test player with a display name and adds them to the game context.
     */
    protected MockGamePlayer createPlayer(String username, String displayName) {
        MockGamePlayer player = new MockGamePlayer(username, displayName);
        gameContext.addPlayer(player);
        return player;
    }
}
