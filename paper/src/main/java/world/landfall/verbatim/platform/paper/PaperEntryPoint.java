package world.landfall.verbatim.platform.paper;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.discord.DiscordBot;
import world.landfall.verbatim.util.MailService;

import java.io.File;

/**
 * Paper plugin entry point for the Verbatim chat channel system.
 *
 * Extends JavaPlugin, which is the base class for all Bukkit/Paper plugins.
 * The plugin.yml must point to this class as the main entry.
 */
@SuppressWarnings("UnstableApiUsage")
public class PaperEntryPoint extends JavaPlugin {

    private PaperVerbatimConfig verbatimConfig;
    private PaperGameContextImpl gameContextImpl;
    private PlayerFileStore fileStore;
    private PersistenceScheduler persistenceScheduler;

    @Override
    public void onEnable() {
        // Set up logging before any other code runs
        Verbatim.LOGGER = new PaperLoggerAdapter(getLogger());
        Verbatim.LOGGER.info("[Verbatim] Setting up Verbatim plugin for Paper...");

        // Save default config.yml if it doesn't exist, then load it
        saveDefaultConfig();
        verbatimConfig = new PaperVerbatimConfig(getConfig());

        // Initialize per-player file store for crash-resilient persistence
        File dataDir = getDataFolder();
        File playerStoreDir = new File(dataDir, "playerstore");
        fileStore = new PlayerFileStore(playerStoreDir);

        // Initialize the game context and wire the file store
        gameContextImpl = new PaperGameContextImpl();
        gameContextImpl.setFileStore(fileStore);
        gameContextImpl.setDataDirectory(dataDir.toPath());

        // Wire all platform-independent services via the service locator
        Verbatim.gameContext = gameContextImpl;
        Verbatim.gameConfig = new PaperGameConfig(verbatimConfig);
        Verbatim.chatFormatter = new PaperChatFormatter();
        Verbatim.channelFormatter = new PaperLocalChannelFormatter();
        Verbatim.permissionService = new PaperPermissionService();
        Verbatim.prefixService = new PaperPrefixService();

        // Initialize mail service
        MailService.init(dataDir.toPath());

        // Register chat and player events
        getServer().getPluginManager().registerEvents(new PaperChatEvents(), this);

        // Register commands via Paper's Brigadier lifecycle event
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Verbatim.LOGGER.info("[Verbatim] Registering Verbatim commands...");
            PaperCommandRegistrar.register(event.registrar());
            Verbatim.LOGGER.info("[Verbatim] Commands registered: /channel, /channels, /msg, /tell, /w, /r, /list, /vlist, /chlist, /chkick, /nick, /ignore, /fav, /mail");
        });

        // Load chat channel configurations
        Verbatim.LOGGER.info("[Verbatim] Loading chat channel configurations...");
        ChatChannelManager.loadConfiguredChannels();

        // Start periodic auto-save scheduler
        persistenceScheduler = new PersistenceScheduler(gameContextImpl::saveAllPlayersToDisk);
        persistenceScheduler.start();

        // Initialize Discord bot
        Verbatim.LOGGER.info("[Verbatim] Initializing Discord Bot...");
        DiscordBot.init();

        Verbatim.LOGGER.info("[Verbatim] Using Bukkit native permission system.");
        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin enabled successfully.");
    }

    @Override
    public void onDisable() {
        Verbatim.LOGGER.info("[Verbatim] Shutting down Verbatim plugin...");

        // Stop auto-save scheduler
        if (persistenceScheduler != null) {
            persistenceScheduler.shutdown();
        }

        // Save all online players' channel state before shutdown
        if (Verbatim.gameContext != null) {
            for (GamePlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
                Verbatim.LOGGER.debug("[Verbatim] Pre-shutdown save for player: {}", player.getUsername());
                ChatChannelManager.playerLoggedOut(player);
            }
        }

        // Shut down mail service
        MailService.shutdown();

        // Shut down Discord bot
        DiscordBot.shutdown();

        // Final flush of all player data to disk
        if (gameContextImpl != null) {
            gameContextImpl.saveAllPlayersToDisk();
        }

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin shut down.");
    }
}
