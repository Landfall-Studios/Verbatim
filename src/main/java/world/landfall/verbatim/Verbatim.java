package world.landfall.verbatim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.landfall.verbatim.context.ChatFormatter;
import world.landfall.verbatim.context.GameConfig;
import world.landfall.verbatim.context.GameContext;
import world.landfall.verbatim.specialchannels.ChannelFormatter;
import world.landfall.verbatim.util.PermissionService;
import world.landfall.verbatim.util.PrefixService;

/**
 * Shared service locator for the Verbatim mod.
 * Platform-independent - all fields are set by the platform-specific entry point.
 */
public final class Verbatim {
    public static final String MODID = "verbatim";
    public static final Logger LOGGER = LoggerFactory.getLogger("Verbatim");

    public static GameContext gameContext;
    public static GameConfig gameConfig;
    public static ChatFormatter chatFormatter;
    public static ChannelFormatter channelFormatter;
    public static PermissionService permissionService;
    public static PrefixService prefixService;

    private Verbatim() {}
}
