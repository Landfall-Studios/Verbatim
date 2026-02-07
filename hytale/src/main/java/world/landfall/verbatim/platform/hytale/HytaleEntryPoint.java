package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.discord.DiscordBot;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * Hytale plugin entry point for the Verbatim chat channel system.
 *
 * Extends JavaPlugin, which is the base class for all Hytale server plugins.
 * The plugin manifest.json must point to this class as the Main entry.
 */
public class HytaleEntryPoint extends JavaPlugin {

    private HytaleVerbatimConfig verbatimConfig;
    private HytaleGameContextImpl gameContextImpl;
    private PlayerFileStore fileStore;
    private PersistenceScheduler persistenceScheduler;
    private File dataDir;

    public HytaleEntryPoint(@Nonnull JavaPluginInit init) {
        super(init);
        // Set up Hytale-native logging before any other code runs
        Verbatim.LOGGER = new HytaleLoggerAdapter(getLogger());
    }

    @Override
    protected void setup() {
        super.setup();

        Verbatim.LOGGER.info("[Verbatim] Setting up Verbatim plugin for Hytale...");

        // Load configuration from plugin data directory
        dataDir = getDataDirectory().toFile();
        verbatimConfig = HytaleVerbatimConfig.loadOrCreate(dataDir);

        // Initialize per-player file store for crash-resilient persistence
        File playerStoreDir = new File(dataDir, "playerstore");
        fileStore = new PlayerFileStore(playerStoreDir);

        // Initialize the game context and wire the file store
        gameContextImpl = new HytaleGameContextImpl();
        gameContextImpl.setFileStore(fileStore);

        // Wire all platform-independent services via the service locator
        Verbatim.gameContext = gameContextImpl;
        Verbatim.gameConfig = new HytaleGameConfig(verbatimConfig);
        Verbatim.chatFormatter = new HytaleChatFormatter();
        Verbatim.channelFormatter = new HytaleLocalChannelFormatter();
        Verbatim.permissionService = new HytalePermissionService();
        Verbatim.prefixService = new HytalePrefixService();

        // Register chat and player events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, HytaleChatEvents::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, HytaleChatEvents::onPlayerDisconnect);
        this.getEventRegistry().registerGlobal(PlayerChatEvent.class, HytaleChatEvents::onPlayerChat);

        // Register commands
        // /channel [list|help|join|leave|focus] - main channel command with subcommands
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelCommand());
        // /channels - standalone alias for /channel list
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelsCommand());
        // Direct messages
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.MsgCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ReplyCommand());
        // Player listing
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ListCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.VListCommand());
        // Admin commands
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChListCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChKickCommand());
        // Nicknames
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.NickCommand());
        // Social (ignore & favorites)
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.IgnoreCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.FavCommand());

        Verbatim.LOGGER.info("[Verbatim] Plugin setup complete. Commands registered: /channel, /channels, /msg (/tell), /r, /list, /vlist, /chlist, /chkick, /nick, /ignore, /fav");
    }

    @Override
    protected void start() {
        super.start();

        Verbatim.LOGGER.info("[Verbatim] Starting Verbatim plugin...");

        // Load chat channel configurations
        Verbatim.LOGGER.info("[Verbatim] Loading chat channel configurations...");
        ChatChannelManager.loadConfiguredChannels();

        // Start periodic auto-save scheduler
        persistenceScheduler = new PersistenceScheduler(gameContextImpl::saveAllPlayersToDisk);
        persistenceScheduler.start();

        // Initialize Discord bot
        Verbatim.LOGGER.info("[Verbatim] Initializing Discord Bot...");
        DiscordBot.init();

        Verbatim.LOGGER.info("[Verbatim] Using Hytale native permission system.");

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin started successfully.");
    }

    @Override
    protected void shutdown() {
        Verbatim.LOGGER.info("[Verbatim] Shutting down Verbatim plugin...");

        // Stop auto-save scheduler
        if (persistenceScheduler != null) {
            persistenceScheduler.shutdown();
        }

        // Save all online players' channel state BEFORE the file write
        // (disconnect events fire AFTER shutdown(), so we must save state now)
        for (world.landfall.verbatim.context.GamePlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
            Verbatim.LOGGER.debug("[Verbatim] Pre-shutdown save for player: {}", player.getUsername());
            ChatChannelManager.playerLoggedOut(player);
        }

        // Shut down Discord bot
        DiscordBot.shutdown();

        // Final flush of all player data to disk
        gameContextImpl.saveAllPlayersToDisk();

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin shut down.");
        super.shutdown();
    }
}
