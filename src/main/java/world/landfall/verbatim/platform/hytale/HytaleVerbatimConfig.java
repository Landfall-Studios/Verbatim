package world.landfall.verbatim.platform.hytale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import world.landfall.verbatim.Verbatim;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hytale configuration for the Verbatim plugin.
 * Uses JSON-based config stored in the plugin's data directory.
 *
 * Hytale plugins have a data directory where configs can be stored.
 * This class reads/writes a verbatim-config.json file.
 */
public class HytaleVerbatimConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Config values
    private String defaultChannelName = "global";
    private List<Map<String, Object>> channels = new ArrayList<>();
    private boolean discordBotEnabled = false;
    private String discordBotToken = "";
    private String discordChannelId = "";
    private String discordMessagePrefix = "&9[Discord]&r";
    private String discordMessageSeparator = "&7 \u00bb ";
    private boolean discordUseEmbedMode = false;
    private String discordNameStyle = "username";
    private boolean customJoinLeaveMessagesEnabled = false;
    private String joinMessageFormat = "&a[+]&r {nickname}";
    private String leaveMessageFormat = "&c[-]&r {nickname}";

    /**
     * Creates a config with default values.
     */
    public HytaleVerbatimConfig() {
        // Set up default channels
        Map<String, Object> globalChannel = new HashMap<>();
        globalChannel.put("name", "global");
        globalChannel.put("displayPrefix", "&a[G]");
        globalChannel.put("shortcut", "g");
        globalChannel.put("permission", "");
        globalChannel.put("range", -1);
        globalChannel.put("nameColor", "&e");
        globalChannel.put("separator", " \u00bb ");
        globalChannel.put("separatorColor", "&7");
        globalChannel.put("messageColor", "&f");
        globalChannel.put("alwaysOn", true);
        globalChannel.put("mature", false);
        globalChannel.put("nameStyle", "displayName");
        channels.add(globalChannel);

        Map<String, Object> localChannel = new HashMap<>();
        localChannel.put("name", "local");
        localChannel.put("displayPrefix", "&b[L]");
        localChannel.put("shortcut", "l");
        localChannel.put("permission", "");
        localChannel.put("range", 100);
        localChannel.put("nameColor", "&e");
        localChannel.put("separator", " \u00bb ");
        localChannel.put("separatorColor", "&7");
        localChannel.put("messageColor", "&7");
        localChannel.put("alwaysOn", true);
        localChannel.put("specialChannelType", "local");
        localChannel.put("mature", false);
        localChannel.put("nameStyle", "displayName");
        channels.add(localChannel);

        Map<String, Object> staffChannel = new HashMap<>();
        staffChannel.put("name", "staff");
        staffChannel.put("displayPrefix", "&c[S]");
        staffChannel.put("shortcut", "s");
        staffChannel.put("permission", "verbatim.channel.staff");
        staffChannel.put("range", -1);
        staffChannel.put("nameColor", "&d");
        staffChannel.put("separator", " &m*&r ");
        staffChannel.put("separatorColor", "&5");
        staffChannel.put("messageColor", "&d");
        staffChannel.put("alwaysOn", false);
        staffChannel.put("mature", false);
        staffChannel.put("nameStyle", "displayName");
        channels.add(staffChannel);
    }

    /**
     * Loads config from a JSON file, or creates one with defaults if it doesn't exist.
     */
    public static HytaleVerbatimConfig loadOrCreate(File dataDirectory) {
        File configFile = new File(dataDirectory, "verbatim-config.json");

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                HytaleVerbatimConfig config = GSON.fromJson(reader, HytaleVerbatimConfig.class);
                if (config != null) {
                    Verbatim.LOGGER.info("[Verbatim] Loaded configuration from {}", configFile.getAbsolutePath());
                    return config;
                }
            } catch (Exception e) {
                Verbatim.LOGGER.error("[Verbatim] Failed to load config: {}. Using defaults.", e.getMessage());
            }
        }

        // Create default config and save it
        HytaleVerbatimConfig config = new HytaleVerbatimConfig();
        config.save(dataDirectory);
        return config;
    }

    /**
     * Saves the config to the JSON file.
     */
    public void save(File dataDirectory) {
        dataDirectory.mkdirs();
        File configFile = new File(dataDirectory, "verbatim-config.json");
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
            Verbatim.LOGGER.info("[Verbatim] Saved configuration to {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            Verbatim.LOGGER.error("[Verbatim] Failed to save config: {}", e.getMessage());
        }
    }

    // === Getters ===

    public String getDefaultChannelName() { return defaultChannelName; }
    public List<Map<String, Object>> getChannels() { return channels; }
    public boolean isDiscordBotEnabled() { return discordBotEnabled; }
    public String getDiscordBotToken() { return discordBotToken; }
    public String getDiscordChannelId() { return discordChannelId; }
    public String getDiscordMessagePrefix() { return discordMessagePrefix; }
    public String getDiscordMessageSeparator() { return discordMessageSeparator; }
    public boolean isDiscordUseEmbedMode() { return discordUseEmbedMode; }
    public String getDiscordNameStyle() { return discordNameStyle; }
    public boolean isCustomJoinLeaveMessagesEnabled() { return customJoinLeaveMessagesEnabled; }
    public String getJoinMessageFormat() { return joinMessageFormat; }
    public String getLeaveMessageFormat() { return leaveMessageFormat; }
}
