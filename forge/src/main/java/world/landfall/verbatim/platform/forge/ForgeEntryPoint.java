package world.landfall.verbatim.platform.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.discord.DiscordBot;
import world.landfall.verbatim.util.MailService;

import java.nio.file.Path;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;

/**
 * Forge 1.20.1 entry point for the Verbatim mod.
 * Registers event handlers, config, and wires all platform services.
 */
@Mod(Verbatim.MODID)
public class ForgeEntryPoint {

    public ForgeEntryPoint() {
        Configurator.setLevel("world.landfall.verbatim", Level.DEBUG);
        Verbatim.LOGGER.info("[Verbatim] Debug logging enabled");

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ForgeVerbatimConfig.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ForgeChatEvents.class);

        // Initialize platform-independent services
        Verbatim.gameContext = new ForgeGameContextImpl();
        Verbatim.gameConfig = new ForgeGameConfig();
        Verbatim.chatFormatter = new ForgeChatFormatter();
        Verbatim.channelFormatter = new ForgeLocalChannelFormatter();
        Verbatim.permissionService = new ForgePermissionService();
        Verbatim.prefixService = new ForgePrefixService();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Verbatim.LOGGER.info("[Verbatim] Common setup complete.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Verbatim.LOGGER.info("Registering Verbatim commands");
        ForgeCommandRegistrar.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Verbatim.LOGGER.info("Server is starting!");

        // Set data directory and initialize mail service
        // Forge 1.20.1: getServerDirectory() returns File, not Path
        Path dataDir = event.getServer().getServerDirectory().toPath().resolve("verbatim");
        ((ForgeGameContextImpl) Verbatim.gameContext).setDataDirectory(dataDir);
        MailService.init(dataDir);

        Verbatim.LOGGER.info("Loading chat channel configurations...");
        ChatChannelManager.loadConfiguredChannels();

        Verbatim.LOGGER.info("Initializing Discord Bot...");
        DiscordBot.init();

        Verbatim.LOGGER.info("[Verbatim] Permission nodes will be handled by {} (if available) or vanilla OP levels.",
            Verbatim.permissionService.isPermissionSystemAvailable() ? "LuckPerms" : "vanilla OP checks");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        Verbatim.LOGGER.info("Server is stopping!");
        MailService.shutdown();
        Verbatim.LOGGER.info("Shutting down Discord Bot...");
        DiscordBot.shutdown();
    }
}
