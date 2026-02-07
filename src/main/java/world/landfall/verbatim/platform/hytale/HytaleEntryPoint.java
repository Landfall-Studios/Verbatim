package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.discord.DiscordBot;
import world.landfall.verbatim.util.PrefixService.NoPrefixService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale plugin entry point for the Verbatim chat channel system.
 *
 * Extends JavaPlugin, which is the base class for all Hytale server plugins.
 * The plugin manifest.json must point to this class as the Main entry.
 */
public class HytaleEntryPoint extends JavaPlugin {

    private static final String PLAYER_DATA_FILE = "playerdata.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HytaleVerbatimConfig verbatimConfig;
    private HytaleGameContextImpl gameContextImpl;
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

        // Initialize the game context (manages player data persistence)
        gameContextImpl = new HytaleGameContextImpl();
        loadPlayerData();

        // Wire all platform-independent services via the service locator
        Verbatim.gameContext = gameContextImpl;
        Verbatim.gameConfig = new HytaleGameConfig(verbatimConfig);
        Verbatim.chatFormatter = new HytaleChatFormatter();
        Verbatim.channelFormatter = new HytaleLocalChannelFormatter();
        Verbatim.permissionService = new HytalePermissionService();
        Verbatim.prefixService = new NoPrefixService();

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

        Verbatim.LOGGER.info("[Verbatim] Plugin setup complete. Commands registered: /channel, /channels, /msg, /r, /list, /vlist, /chlist, /chkick, /nick");
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

        Verbatim.LOGGER.info("[Verbatim] Using Hytale native permission system.");

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin started successfully.");
    }

    @Override
    protected void shutdown() {
        Verbatim.LOGGER.info("[Verbatim] Shutting down Verbatim plugin...");

        // Save all online players' channel state BEFORE the file write
        // (disconnect events fire AFTER shutdown(), so we must save state now)
        for (world.landfall.verbatim.context.GamePlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
            Verbatim.LOGGER.debug("[Verbatim] Pre-shutdown save for player: {}", player.getUsername());
            ChatChannelManager.playerLoggedOut(player);
        }

        // Shut down Discord bot
        DiscordBot.shutdown();

        // Save persistent player data to file
        savePlayerData();

        Verbatim.LOGGER.info("[Verbatim] Verbatim plugin shut down.");
        super.shutdown();
    }

    private void loadPlayerData() {
        File file = new File(dataDir, PLAYER_DATA_FILE);
        Verbatim.LOGGER.info("[Verbatim] Looking for player data at: {}", file.getAbsolutePath());
        if (!file.exists()) {
            Verbatim.LOGGER.info("[Verbatim] No player data file found, starting fresh.");
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            Map<String, String> data = GSON.fromJson(reader, type);
            if (data != null) {
                gameContextImpl.loadPersistentData(new ConcurrentHashMap<>(data));
                Verbatim.LOGGER.info("[Verbatim] Loaded player data ({} entries): {}", data.size(), data.keySet());
            } else {
                Verbatim.LOGGER.warn("[Verbatim] Player data file was empty or invalid JSON.");
            }
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim] Failed to load player data: {}", e.getMessage());
        }
    }

    private void savePlayerData() {
        dataDir.mkdirs();
        File file = new File(dataDir, PLAYER_DATA_FILE);
        Verbatim.LOGGER.info("[Verbatim] Saving player data to: {}", file.getAbsolutePath());
        try (FileWriter writer = new FileWriter(file)) {
            ConcurrentHashMap<String, String> data = gameContextImpl.getPersistentDataMap();
            Verbatim.LOGGER.info("[Verbatim] Saving player data ({} entries): {}", data.size(), data.keySet());
            GSON.toJson(data, writer);
            writer.flush();
            Verbatim.LOGGER.info("[Verbatim] Player data saved successfully.");
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim] Failed to save player data: {}", e.getMessage(), e);
        }
    }
}
