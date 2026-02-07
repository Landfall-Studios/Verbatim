package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.discord.DiscordBot;
import world.landfall.verbatim.util.PermissionService;
import world.landfall.verbatim.util.PrefixService;

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

    public HytaleEntryPoint(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        Verbatim.LOGGER.info("[Verbatim] Setting up Verbatim plugin for Hytale...");

        // Load configuration from plugin data directory
        File dataDir = getDataDirectory().toFile();
        verbatimConfig = HytaleVerbatimConfig.loadOrCreate(dataDir);

        // Initialize the game context (manages player data persistence)
        gameContextImpl = new HytaleGameContextImpl();

        // Wire all platform-independent services via the service locator
        Verbatim.gameContext = gameContextImpl;
        Verbatim.gameConfig = new HytaleGameConfig(verbatimConfig);
        Verbatim.chatFormatter = new HytaleChatFormatter();
        Verbatim.channelFormatter = new HytaleLocalChannelFormatter();
        Verbatim.permissionService = new PermissionService();
        Verbatim.prefixService = new PrefixService();

        // Register chat and player events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, HytaleChatEvents::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, HytaleChatEvents::onPlayerDisconnect);
        this.getEventRegistry().registerGlobal(PlayerChatEvent.class, HytaleChatEvents::onPlayerChat);

        // Register commands
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelsCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelHelpCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelFocusCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelJoinCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChannelLeaveCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.MsgCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ReplyCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ListCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.VListCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChListCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.ChKickCommand());
        this.getCommandRegistry().registerCommand(new HytaleCommandRegistrar.NickCommand());

        Verbatim.LOGGER.info("[Verbatim] Plugin setup complete. {} commands registered.", 12);
    }

    @Override
    protected void start() {
        super.start();

        Verbatim.LOGGER.info("[Verbatim] Starting Verbatim plugin...");

        // Load chat channel configurations
        Verbatim.LOGGER.info("[Verbatim] Loading chat channel configurations...");
        ChatChannelManager.loadConfiguredChannels();

        // Initialize Discord bot
        Verbatim.LOGGER.info("[Verbatim] Initializing Discord Bot...");
        DiscordBot.init();

        Verbatim.LOGGER.info("[Verbatim] Permission nodes will be handled by {} (if available) or default deny.",
            Verbatim.permissionService.isLuckPermsAvailable() ? "LuckPerms" : "fallback checks");

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin started successfully.");
    }

    @Override
    protected void shutdown() {
        Verbatim.LOGGER.info("[Verbatim] Shutting down Verbatim plugin...");

        // Shut down Discord bot
        DiscordBot.shutdown();

        // Save persistent player data
        // (In a production setup, this would persist to disk)

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin shut down.");
        super.shutdown();
    }
}
