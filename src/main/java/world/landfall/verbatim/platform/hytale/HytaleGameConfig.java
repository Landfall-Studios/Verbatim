package world.landfall.verbatim.platform.hytale;

import world.landfall.verbatim.context.GameConfig;

import java.util.List;
import java.util.Map;

/**
 * Hytale implementation of GameConfig, wrapping HytaleVerbatimConfig.
 */
public class HytaleGameConfig implements GameConfig {

    private final HytaleVerbatimConfig config;

    public HytaleGameConfig(HytaleVerbatimConfig config) {
        this.config = config;
    }

    @Override
    public String getDefaultChannelName() {
        return config.getDefaultChannelName();
    }

    @Override
    public List<Map<String, Object>> getChannelDefinitions() {
        return config.getChannels();
    }

    @Override
    public boolean isDiscordEnabled() {
        return config.isDiscordBotEnabled();
    }

    @Override
    public String getDiscordBotToken() {
        return config.getDiscordBotToken();
    }

    @Override
    public String getDiscordChannelId() {
        return config.getDiscordChannelId();
    }

    @Override
    public String getDiscordMessagePrefix() {
        return config.getDiscordMessagePrefix();
    }

    @Override
    public String getDiscordMessageSeparator() {
        return config.getDiscordMessageSeparator();
    }

    @Override
    public boolean isDiscordUseEmbedMode() {
        return config.isDiscordUseEmbedMode();
    }

    @Override
    public String getDiscordNameStyle() {
        return config.getDiscordNameStyle();
    }

    @Override
    public boolean isCustomJoinLeaveEnabled() {
        return config.isCustomJoinLeaveMessagesEnabled();
    }

    @Override
    public String getJoinMessageFormat() {
        return config.getJoinMessageFormat();
    }

    @Override
    public String getLeaveMessageFormat() {
        return config.getLeaveMessageFormat();
    }
}
