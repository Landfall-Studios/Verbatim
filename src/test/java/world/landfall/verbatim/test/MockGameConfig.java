package world.landfall.verbatim.test;

import world.landfall.verbatim.context.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock GameConfig for unit testing.
 */
public class MockGameConfig implements GameConfig {
    private String defaultChannelName = "global";
    private List<Map<String, Object>> channelDefinitions = new ArrayList<>();
    private boolean discordEnabled = false;
    private boolean customJoinLeaveEnabled = false;
    private String joinMessageFormat = "{player} joined";
    private String leaveMessageFormat = "{player} left";

    public void setDefaultChannelName(String name) {
        this.defaultChannelName = name;
    }

    public void setChannelDefinitions(List<Map<String, Object>> definitions) {
        this.channelDefinitions = definitions;
    }

    public void setDiscordEnabled(boolean enabled) {
        this.discordEnabled = enabled;
    }

    public void setCustomJoinLeaveEnabled(boolean enabled) {
        this.customJoinLeaveEnabled = enabled;
    }

    public void setJoinMessageFormat(String format) {
        this.joinMessageFormat = format;
    }

    public void setLeaveMessageFormat(String format) {
        this.leaveMessageFormat = format;
    }

    @Override
    public String getDefaultChannelName() {
        return defaultChannelName;
    }

    @Override
    public List<Map<String, Object>> getChannelDefinitions() {
        return channelDefinitions;
    }

    @Override
    public boolean isDiscordEnabled() {
        return discordEnabled;
    }

    @Override
    public String getDiscordBotToken() {
        return "";
    }

    @Override
    public String getDiscordChannelId() {
        return "";
    }

    @Override
    public String getDiscordMessagePrefix() {
        return "";
    }

    @Override
    public String getDiscordMessageSeparator() {
        return ": ";
    }

    @Override
    public boolean isDiscordUseEmbedMode() {
        return false;
    }

    @Override
    public String getDiscordNameStyle() {
        return "displayName";
    }

    @Override
    public boolean isCustomJoinLeaveEnabled() {
        return customJoinLeaveEnabled;
    }

    @Override
    public String getJoinMessageFormat() {
        return joinMessageFormat;
    }

    @Override
    public String getLeaveMessageFormat() {
        return leaveMessageFormat;
    }
}
