package world.landfall.verbatim.platform.paper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import world.landfall.verbatim.Verbatim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paper configuration wrapper over Bukkit's FileConfiguration (YAML).
 * Reads config.yml values and exposes them as typed getters.
 */
public class PaperVerbatimConfig {

    private final FileConfiguration config;

    public PaperVerbatimConfig(FileConfiguration config) {
        this.config = config;
    }

    public String getDefaultChannelName() {
        return config.getString("default-channel-name", "global");
    }

    public List<Map<String, Object>> getChannels() {
        List<Map<String, Object>> channels = new ArrayList<>();
        List<?> channelList = config.getList("channels");
        if (channelList == null) {
            return channels;
        }

        for (Object entry : channelList) {
            if (entry instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> channelMap = (Map<String, Object>) entry;
                channels.add(channelMap);
            }
        }
        return channels;
    }

    // Discord settings
    public boolean isDiscordBotEnabled() {
        return config.getBoolean("discord.enabled", false);
    }

    public String getDiscordBotToken() {
        return config.getString("discord.bot-token", "");
    }

    public String getDiscordChannelId() {
        return config.getString("discord.channel-id", "");
    }

    public String getDiscordMessagePrefix() {
        return config.getString("discord.message-prefix", "&9[Discord]&r");
    }

    public String getDiscordMessageSeparator() {
        return config.getString("discord.message-separator", "&7 \u00bb ");
    }

    public boolean isDiscordUseEmbedMode() {
        return config.getBoolean("discord.use-embed-mode", false);
    }

    public String getDiscordNameStyle() {
        return config.getString("discord.name-style", "username");
    }

    // Join/leave settings
    public boolean isCustomJoinLeaveMessagesEnabled() {
        return config.getBoolean("join-leave.enabled", false);
    }

    public String getJoinMessageFormat() {
        return config.getString("join-leave.join-format", "&a[+]&r {nickname}");
    }

    public String getLeaveMessageFormat() {
        return config.getString("join-leave.leave-format", "&c[-]&r {nickname}");
    }
}
