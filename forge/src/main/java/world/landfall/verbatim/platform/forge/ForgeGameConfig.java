package world.landfall.verbatim.platform.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import world.landfall.verbatim.context.GameConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge 1.20.1 implementation of GameConfig, wrapping ForgeVerbatimConfig (ForgeConfigSpec).
 */
public class ForgeGameConfig implements GameConfig {

    @Override
    public String getDefaultChannelName() {
        return ForgeVerbatimConfig.DEFAULT_CHANNEL_NAME.get();
    }

    @Override
    public List<Map<String, Object>> getChannelDefinitions() {
        List<? extends UnmodifiableConfig> rawChannels = ForgeVerbatimConfig.CHANNELS.get();
        List<Map<String, Object>> result = new ArrayList<>();

        if (rawChannels == null) {
            return result;
        }

        for (UnmodifiableConfig config : rawChannels) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", config.get("name"));
            map.put("displayPrefix", config.get("displayPrefix"));
            map.put("shortcut", config.get("shortcut"));

            if (config.contains("permission")) map.put("permission", config.get("permission"));
            if (config.contains("range")) map.put("range", config.get("range"));
            if (config.contains("nameColor")) map.put("nameColor", config.get("nameColor"));
            if (config.contains("separator")) map.put("separator", config.get("separator"));
            if (config.contains("separatorColor")) map.put("separatorColor", config.get("separatorColor"));
            if (config.contains("messageColor")) map.put("messageColor", config.get("messageColor"));
            if (config.contains("alwaysOn")) map.put("alwaysOn", config.get("alwaysOn"));
            if (config.contains("mature")) map.put("mature", config.get("mature"));
            if (config.contains("specialChannelType")) map.put("specialChannelType", config.get("specialChannelType"));
            if (config.contains("nameStyle")) map.put("nameStyle", config.get("nameStyle"));

            result.add(map);
        }

        return result;
    }

    @Override
    public boolean isDiscordEnabled() {
        return ForgeVerbatimConfig.DISCORD_BOT_ENABLED.get();
    }

    @Override
    public String getDiscordBotToken() {
        return ForgeVerbatimConfig.DISCORD_BOT_TOKEN.get();
    }

    @Override
    public String getDiscordChannelId() {
        return ForgeVerbatimConfig.DISCORD_CHANNEL_ID.get();
    }

    @Override
    public String getDiscordMessagePrefix() {
        return ForgeVerbatimConfig.DISCORD_MESSAGE_PREFIX.get();
    }

    @Override
    public String getDiscordMessageSeparator() {
        return ForgeVerbatimConfig.DISCORD_MESSAGE_SEPARATOR.get();
    }

    @Override
    public boolean isDiscordUseEmbedMode() {
        return ForgeVerbatimConfig.DISCORD_USE_EMBED_MODE.get();
    }

    @Override
    public String getDiscordNameStyle() {
        return ForgeVerbatimConfig.DISCORD_NAME_STYLE.get();
    }

    @Override
    public boolean isCustomJoinLeaveEnabled() {
        return ForgeVerbatimConfig.CUSTOM_JOIN_LEAVE_MESSAGES_ENABLED.get();
    }

    @Override
    public String getJoinMessageFormat() {
        return ForgeVerbatimConfig.JOIN_MESSAGE_FORMAT.get();
    }

    @Override
    public String getLeaveMessageFormat() {
        return ForgeVerbatimConfig.LEAVE_MESSAGE_FORMAT.get();
    }
}
