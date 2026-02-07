package world.landfall.verbatim.context;

import java.util.List;
import java.util.Map;

/**
 * Platform-independent interface for mod configuration access.
 */
public interface GameConfig {

    // === Channel Config ===

    String getDefaultChannelName();

    /**
     * Returns channel definitions as a list of property maps.
     * Each map contains keys: name, displayPrefix, shortcut, permission, range,
     * nameColor, separator, separatorColor, messageColor, alwaysOn, mature,
     * specialChannelType, nameStyle.
     */
    List<Map<String, Object>> getChannelDefinitions();

    // === Discord Config ===

    boolean isDiscordEnabled();
    String getDiscordBotToken();
    String getDiscordChannelId();
    String getDiscordMessagePrefix();
    String getDiscordMessageSeparator();
    boolean isDiscordUseEmbedMode();
    String getDiscordNameStyle();

    // === Join/Leave Messages ===

    boolean isCustomJoinLeaveEnabled();
    String getJoinMessageFormat();
    String getLeaveMessageFormat();
}
