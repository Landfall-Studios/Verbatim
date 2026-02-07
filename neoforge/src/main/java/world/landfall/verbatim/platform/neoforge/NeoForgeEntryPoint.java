package world.landfall.verbatim.platform.neoforge;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.discord.DiscordBot;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;

@Mod(Verbatim.MODID)
public class NeoForgeEntryPoint {

    public NeoForgeEntryPoint(IEventBus modEventBus, ModContainer modContainer) {
        Configurator.setLevel("world.landfall.verbatim", Level.DEBUG);
        Verbatim.LOGGER.info("[Verbatim] Debug logging enabled");

        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.SERVER, VerbatimConfig.SPEC);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(NeoForgeChatEvents.class);

        // Initialize platform-independent services
        Verbatim.gameContext = new NeoForgeGameContextImpl();
        Verbatim.gameConfig = new NeoForgeGameConfig();
        Verbatim.chatFormatter = new NeoForgeChatFormatter();
        Verbatim.channelFormatter = new NeoForgeLocalChannelFormatter();
        Verbatim.permissionService = new NeoForgePermissionService();
        Verbatim.prefixService = new NeoForgePrefixService();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Verbatim.LOGGER.info("[Verbatim] Common setup complete.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Verbatim.LOGGER.info("Registering Verbatim commands");
        NeoForgeCommandRegistrar.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Verbatim.LOGGER.info("Server is starting!");
        Verbatim.LOGGER.info("Loading chat channel configurations...");
        ChatChannelManager.loadConfiguredChannels();

        Verbatim.LOGGER.info("Initializing Discord Bot...");
        DiscordBot.init();

        Verbatim.LOGGER.info("[Verbatim] Permission nodes will be handled by {} (if available) or vanilla OP levels.",
                    Verbatim.permissionService.isPermissionSystemAvailable() ? "LuckPerms" : "vanilla OP checks");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        Verbatim.LOGGER.info("Server is stopping! Shutting down Discord Bot...");
        DiscordBot.shutdown();
    }
}
