package world.landfall.verbatim.platform.neoforge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
// VerbatimConfig is in this package
import world.landfall.verbatim.context.GameConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge implementation of GameConfig, wrapping VerbatimConfig (ModConfigSpec).
 */
public class NeoForgeGameConfig implements GameConfig {

    @Override
    public String getDefaultChannelName() {
        return VerbatimConfig.DEFAULT_CHANNEL_NAME.get();
    }

    @Override
    public List<Map<String, Object>> getChannelDefinitions() {
        List<? extends UnmodifiableConfig> rawChannels = VerbatimConfig.CHANNELS.get();
        List<Map<String, Object>> result = new ArrayList<>();

        if (rawChannels == null) {
            return result;
        }

        for (UnmodifiableConfig config : rawChannels) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", config.get("name"));
            map.put("displayPrefix", config.get("displayPrefix"));
            map.put("shortcut", config.get("shortcut"));

            if (config.contains("permission")) {
                map.put("permission", config.get("permission"));
            }
            if (config.contains("range")) {
                map.put("range", config.get("range"));
            }
            if (config.contains("nameColor")) {
                map.put("nameColor", config.get("nameColor"));
            }
            if (config.contains("separator")) {
                map.put("separator", config.get("separator"));
            }
            if (config.contains("separatorColor")) {
                map.put("separatorColor", config.get("separatorColor"));
            }
            if (config.contains("messageColor")) {
                map.put("messageColor", config.get("messageColor"));
            }
            if (config.contains("alwaysOn")) {
                map.put("alwaysOn", config.get("alwaysOn"));
            }
            if (config.contains("mature")) {
                map.put("mature", config.get("mature"));
            }
            if (config.contains("specialChannelType")) {
                map.put("specialChannelType", config.get("specialChannelType"));
            }
            if (config.contains("nameStyle")) {
                map.put("nameStyle", config.get("nameStyle"));
            }

            result.add(map);
        }

        return result;
    }

    @Override
    public boolean isDiscordEnabled() {
        return VerbatimConfig.DISCORD_BOT_ENABLED.get();
    }

    @Override
    public String getDiscordBotToken() {
        return VerbatimConfig.DISCORD_BOT_TOKEN.get();
    }

    @Override
    public String getDiscordChannelId() {
        return VerbatimConfig.DISCORD_CHANNEL_ID.get();
    }

    @Override
    public String getDiscordMessagePrefix() {
        return VerbatimConfig.DISCORD_MESSAGE_PREFIX.get();
    }

    @Override
    public String getDiscordMessageSeparator() {
        return VerbatimConfig.DISCORD_MESSAGE_SEPARATOR.get();
    }

    @Override
    public boolean isDiscordUseEmbedMode() {
        return VerbatimConfig.DISCORD_USE_EMBED_MODE.get();
    }

    @Override
    public String getDiscordNameStyle() {
        return VerbatimConfig.DISCORD_NAME_STYLE.get();
    }

    @Override
    public boolean isCustomJoinLeaveEnabled() {
        return VerbatimConfig.CUSTOM_JOIN_LEAVE_MESSAGES_ENABLED.get();
    }

    @Override
    public String getJoinMessageFormat() {
        return VerbatimConfig.JOIN_MESSAGE_FORMAT.get();
    }

    @Override
    public String getLeaveMessageFormat() {
        return VerbatimConfig.LEAVE_MESSAGE_FORMAT.get();
    }
}
