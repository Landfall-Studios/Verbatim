package world.landfall.verbatim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import world.landfall.verbatim.chat.FocusTarget;
import world.landfall.verbatim.chat.ChatFocus;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GamePlayer;
import static world.landfall.verbatim.context.GameText.*;

public class ChatChannelManager {
    private static volatile boolean isInitialized = false;

    // Data storage keys
    private static final String DATA_JOINED_CHANNELS = "verbatim:joined_channels";
    private static final String DATA_FOCUSED_CHANNEL = "verbatim:focused_channel";

    // Permission level required for channel access (operator level 2)
    static final int CHANNEL_PERMISSION_LEVEL = 2;

    // Thread-safe collections for concurrent access
    private static final Map<UUID, FocusTarget> playerFocus = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> joinedChannels = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> lastIncomingDmSender = new ConcurrentHashMap<>();

    public static class ChannelConfig {
        public final String name;
        public final String displayPrefix;
        public final String shortcut;
        public final Optional<String> permission;
        public final int range;
        public final String nameColor;
        public final String separator;
        public final String separatorColor;
        public final String messageColor;
        public final boolean alwaysOn;
        public final boolean mature;
        public final Optional<String> specialChannelType;
        public final NameStyle nameStyle;

        public ChannelConfig(String name, String displayPrefix, String shortcut, String permission, Number range,
                             String nameColor, String separator, String separatorColor, String messageColor, Boolean alwaysOn, Boolean mature, String specialChannelType, String nameStyle) {
            this.name = name;
            this.displayPrefix = displayPrefix;
            this.shortcut = shortcut;
            this.alwaysOn = (alwaysOn == null) ? false : alwaysOn;
            this.mature = (mature == null) ? false : mature;
            this.permission = (this.alwaysOn || permission == null || permission.trim().isEmpty()) ? Optional.empty() : Optional.of(permission);
            this.range = (range == null) ? -1 : range.intValue();

            this.messageColor = (messageColor == null || messageColor.isEmpty()) ? "&f" : messageColor;
            this.nameColor = (nameColor == null || nameColor.isEmpty()) ? this.messageColor : nameColor;
            this.separator = (separator == null || separator.isEmpty()) ? ": " : separator;
            this.separatorColor = (separatorColor == null || separatorColor.isEmpty()) ? this.messageColor : separatorColor;
            this.specialChannelType = (specialChannelType == null || specialChannelType.isEmpty()) ? Optional.empty() : Optional.of(specialChannelType);
            this.nameStyle = NameStyle.fromConfigValue(nameStyle);
        }
    }

    private static final Map<String, ChannelConfig> channelConfigsByName = new ConcurrentHashMap<>();
    private static final Map<String, ChannelConfig> channelConfigsByShortcut = new ConcurrentHashMap<>();

    /**
     * Resets all state. Used for unit testing.
     */
    public static void reset() {
        isInitialized = false;
        playerFocus.clear();
        joinedChannels.clear();
        lastIncomingDmSender.clear();
        channelConfigsByName.clear();
        channelConfigsByShortcut.clear();
    }

    /**
     * Adds a channel config directly. Used for unit testing.
     */
    public static void addChannelConfig(ChannelConfig config) {
        channelConfigsByName.put(config.name, config);
        if (config.shortcut != null && !config.shortcut.isEmpty()) {
            channelConfigsByShortcut.put(config.shortcut, config);
        }
        isInitialized = true;
    }

    public static void loadConfiguredChannels() {
        channelConfigsByName.clear();
        channelConfigsByShortcut.clear();

        List<Map<String, Object>> channelsFromConfig = Verbatim.gameConfig.getChannelDefinitions();
        Verbatim.LOGGER.info("Loading {} channel definitions from config.", channelsFromConfig.size());

        for (Map<String, Object> channelConf : channelsFromConfig) {
            try {
                String name = (String) channelConf.get("name");
                String displayPrefix = (String) channelConf.get("displayPrefix");
                String shortcut = (String) channelConf.get("shortcut");
                String permissionStr = channelConf.containsKey("permission") ? String.valueOf(channelConf.get("permission")) : null;
                Object rangeObj = channelConf.get("range");
                Number range = (rangeObj instanceof Number) ? (Number) rangeObj : null;
                String nameColor = channelConf.containsKey("nameColor") ? String.valueOf(channelConf.get("nameColor")) : null;
                String separator = channelConf.containsKey("separator") ? String.valueOf(channelConf.get("separator")) : null;
                String separatorColor = channelConf.containsKey("separatorColor") ? String.valueOf(channelConf.get("separatorColor")) : null;
                String messageColor = channelConf.containsKey("messageColor") ? String.valueOf(channelConf.get("messageColor")) : null;
                Boolean alwaysOn = channelConf.containsKey("alwaysOn") ? (Boolean) channelConf.get("alwaysOn") : false;
                Boolean mature = channelConf.containsKey("mature") ? (Boolean) channelConf.get("mature") : false;
                String specialChannelType = channelConf.containsKey("specialChannelType") ? String.valueOf(channelConf.get("specialChannelType")) : null;
                String nameStyleStr = channelConf.containsKey("nameStyle") ? String.valueOf(channelConf.get("nameStyle")) : null;

                if (name != null && !name.isEmpty() && displayPrefix != null && shortcut != null && !shortcut.isEmpty()) {
                    ChannelConfig parsedConfig = new ChannelConfig(name, displayPrefix, shortcut, permissionStr, range,
                                                                 nameColor, separator, separatorColor, messageColor, alwaysOn, mature, specialChannelType, nameStyleStr);
                    if (channelConfigsByName.containsKey(name)) {
                        Verbatim.LOGGER.warn("Duplicate channel name in config: '{}'. Ignoring subsequent definition.", name);
                        continue;
                    }
                    if (channelConfigsByShortcut.containsKey(shortcut)) {
                        Verbatim.LOGGER.warn("Duplicate channel shortcut in config: '{}'. Ignoring subsequent definition.", shortcut);
                        continue;
                    }
                    channelConfigsByName.put(name, parsedConfig);
                    channelConfigsByShortcut.put(shortcut, parsedConfig);
                    Verbatim.LOGGER.debug("Successfully loaded channel: {}", name);
                } else {
                    Verbatim.LOGGER.warn("Invalid channel definition (values not matching expected types or missing after validation). Skipping.");
                }
            } catch (Exception e) {
                Verbatim.LOGGER.error("Unexpected error parsing channel definition", e);
            }
        }
        Verbatim.LOGGER.info("Finished loading chat channels. Total loaded: {}", channelConfigsByName.size());
        isInitialized = true;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static java.util.Collection<ChannelConfig> getAllChannelConfigs() {
        return channelConfigsByName.values();
    }

    public static Optional<ChannelConfig> getChannelConfigByName(String name) {
        return Optional.ofNullable(channelConfigsByName.get(name));
    }

    public static Optional<ChannelConfig> getChannelConfigByShortcut(String shortcut) {
        return Optional.ofNullable(channelConfigsByShortcut.get(shortcut));
    }

    public static ChannelConfig getDefaultChannelConfig() {
        String defaultChannelName = Verbatim.gameConfig.getDefaultChannelName();
        ChannelConfig defaultConfig = channelConfigsByName.get(defaultChannelName);
        if (defaultConfig == null) {
            Verbatim.LOGGER.warn("[ChatChannelManager] Default channel named '{}' not found. Falling back.", defaultChannelName);
            Collection<ChannelConfig> allConfigs = channelConfigsByName.values();
            if (!allConfigs.isEmpty()) {
                // Prefer alwaysOn channel, otherwise use any available channel
                defaultConfig = allConfigs.stream()
                    .filter(c -> c.alwaysOn)
                    .findFirst()
                    .orElseGet(() -> allConfigs.stream().findFirst().orElse(null));
                if (defaultConfig != null) {
                    Verbatim.LOGGER.warn("[ChatChannelManager] Using '{}' as fallback default channel.", defaultConfig.name);
                }
            }
            if (defaultConfig == null) {
                Verbatim.LOGGER.error("[ChatChannelManager] CRITICAL: No channels loaded. Cannot determine a default channel.");
                return null;
            }
        }
        return defaultConfig;
    }

    public static void playerLoggedIn(GamePlayer player) {
        loadPlayerChannelState(player);
        ensurePlayerIsInADefaultFocus(player);
    }

    private static void loadPlayerChannelState(GamePlayer player) {
        Set<String> loadedJoinedChannels = new HashSet<>();
        String loadedFocusedChannel = null;
        try {
            if (Verbatim.gameContext.hasPlayerData(player, DATA_JOINED_CHANNELS)) {
                String[] joined = Verbatim.gameContext.getPlayerStringData(player, DATA_JOINED_CHANNELS).split(",");
                for (String chName : joined) {
                    if (!chName.isEmpty() && channelConfigsByName.containsKey(chName)) {
                        loadedJoinedChannels.add(chName);
                    }
                }
            }
            if (Verbatim.gameContext.hasPlayerData(player, DATA_FOCUSED_CHANNEL)) {
                loadedFocusedChannel = Verbatim.gameContext.getPlayerStringData(player, DATA_FOCUSED_CHANNEL);
                if (!channelConfigsByName.containsKey(loadedFocusedChannel)) {
                    loadedFocusedChannel = null;
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.error("[ChatChannelManager] Error loading player channel state for {}: {}", player.getUsername(), e.getMessage());
        }

        joinedChannels.put(player.getUUID(), loadedJoinedChannels);

        for (ChannelConfig config : channelConfigsByName.values()) {
            if (config.alwaysOn) {
                boolean wasJoined = loadedJoinedChannels.contains(config.name);
                internalJoinChannel(player, config.name, true);

                if (!wasJoined && config.mature) {
                    Verbatim.gameContext.sendMessage(player, Verbatim.gameContext.createWarningPrefix()
                        .append(text("WARNING: This channel may contain mature content!").withColor(GameColor.GOLD).withBold(true)));
                    Verbatim.gameContext.sendMessage(player, text("By remaining in this channel, you confirm that you are 18+ and okay with seeing messages posted here.").withColor(GameColor.YELLOW));
                    Verbatim.gameContext.sendMessage(player, text("If you are not comfortable with this, please leave immediately using: ").withColor(GameColor.YELLOW)
                        .append(text("/channel leave").withColor(GameColor.WHITE).withUnderlined(true)));
                }
            } else if (loadedJoinedChannels.contains(config.name)) {
                if (config.permission.isPresent() && !Verbatim.permissionService.hasPermission(player, config.permission.get(), CHANNEL_PERMISSION_LEVEL)) {
                    Verbatim.LOGGER.info("[ChatChannelManager] Player {} lost permission for saved joined channel '{}' on login. Removing.", player.getUsername(), config.name);
                    internalLeaveChannel(player, config.name);
                }
            }
        }

        if (loadedFocusedChannel != null && getJoinedChannels(player).contains(loadedFocusedChannel)) {
            playerFocus.put(player.getUUID(), ChatFocus.createChannelFocus(loadedFocusedChannel));
        } else {
            playerFocus.remove(player.getUUID());
        }
        savePlayerChannelState(player);
    }

    private static void ensurePlayerIsInADefaultFocus(GamePlayer player) {
        FocusTarget currentFocus = playerFocus.get(player.getUUID());
        if (currentFocus == null || !currentFocus.isValid() ||
            (currentFocus instanceof ChatFocus && !isJoined(player, ((ChatFocus) currentFocus).getChannelName()))) {
            ChannelConfig defaultChannel = getDefaultChannelConfig();
            if (defaultChannel != null) {
                Verbatim.LOGGER.info("[ChatChannelManager] Player {} focus invalid or not joined. Focusing to default '{}'.", player.getUsername(), defaultChannel.name);
                focusChannel(player, defaultChannel.name);
            } else {
                Verbatim.LOGGER.error("[ChatChannelManager] Player {} needs focus reset, but no default channel available!", player.getUsername());
            }
        }
    }

    private static void savePlayerChannelState(GamePlayer player) {
        Set<String> currentJoined = joinedChannels.getOrDefault(player.getUUID(), new HashSet<>());
        Verbatim.gameContext.setPlayerStringData(player, DATA_JOINED_CHANNELS, String.join(",", currentJoined));
        FocusTarget currentFocused = playerFocus.get(player.getUUID());
        if (currentFocused instanceof ChatFocus) {
            ChatFocus chatFocus = (ChatFocus) currentFocused;
            if (chatFocus.getType() == ChatFocus.FocusType.CHANNEL) {
                String channelName = chatFocus.getChannelName();
                if (channelName != null) {
                    Verbatim.gameContext.setPlayerStringData(player, DATA_FOCUSED_CHANNEL, channelName);
                } else {
                    Verbatim.gameContext.removePlayerData(player, DATA_FOCUSED_CHANNEL);
                }
            } else {
                Verbatim.gameContext.removePlayerData(player, DATA_FOCUSED_CHANNEL);
            }
        } else {
            Verbatim.gameContext.removePlayerData(player, DATA_FOCUSED_CHANNEL);
        }
    }

    public static Set<String> getJoinedChannels(GamePlayer player) {
        return joinedChannels.getOrDefault(player.getUUID(), new HashSet<>());
    }

    public static List<ChannelConfig> getJoinedChannelConfigs(GamePlayer player) {
        return getJoinedChannels(player).stream()
            .map(ChatChannelManager::getChannelConfigByName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public static Optional<ChannelConfig> getFocusedChannelConfig(GamePlayer player) {
        FocusTarget focus = playerFocus.get(player.getUUID());
        if (focus instanceof ChatFocus && ((ChatFocus) focus).getType() == ChatFocus.FocusType.CHANNEL) {
            return getChannelConfigByName(((ChatFocus) focus).getChannelName());
        }
        return Optional.empty();
    }

    public static boolean isJoined(GamePlayer player, String channelName) {
        return getJoinedChannels(player).contains(channelName);
    }

    private static boolean internalJoinChannel(GamePlayer player, String channelName, boolean forceJoin) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) return false;

        if (!forceJoin && config.permission.isPresent() && !Verbatim.permissionService.hasPermission(player, config.permission.get(), CHANNEL_PERMISSION_LEVEL)) {
            return false;
        }
        joinedChannels.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(channelName);
        savePlayerChannelState(player);
        return true;
    }

    public static boolean joinChannel(GamePlayer player, String channelName) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) {
            Verbatim.gameContext.sendMessage(player, text("Channel '" + channelName + "' not found.").withColor(GameColor.RED));
            return false;
        }
        if (isJoined(player, channelName)) {
            Verbatim.gameContext.sendMessage(player, text("Already joined to channel: ").withColor(GameColor.YELLOW)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name)));
            return true;
        }

        if (config.alwaysOn || !config.permission.isPresent() || Verbatim.permissionService.hasPermission(player, config.permission.get(), CHANNEL_PERMISSION_LEVEL)) {
            internalJoinChannel(player, channelName, config.alwaysOn);
            Verbatim.gameContext.sendMessage(player, text("Joined channel: ").withColor(GameColor.GREEN)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name)));

            if (config.mature) {
                Verbatim.gameContext.sendMessage(player, Verbatim.gameContext.createWarningPrefix()
                    .append(text("WARNING: This channel may contain mature content.").withColor(GameColor.GOLD).withBold(true)));
                Verbatim.gameContext.sendMessage(player, text("By remaining in this channel, you confirm that you are 18+ and okay with seeing messages posted here.").withColor(GameColor.YELLOW));
                Verbatim.gameContext.sendMessage(player, text("If you are not comfortable with this, please leave immediately using: ").withColor(GameColor.YELLOW)
                    .append(text("/channel leave").withColor(GameColor.WHITE).withUnderlined(true)));
            }
            return true;
        } else {
            Verbatim.gameContext.sendMessage(player, text("You do not have permission to join channel: ").withColor(GameColor.RED)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name)));
            return false;
        }
    }

    public static void autoLeaveChannel(GamePlayer player, String channelName) {
        internalLeaveChannel(player, channelName);
        FocusTarget currentFocus = playerFocus.get(player.getUUID());
        if (currentFocus instanceof ChatFocus && channelName.equals(((ChatFocus) currentFocus).getChannelName())) {
            playerFocus.remove(player.getUUID());
            ensurePlayerIsInADefaultFocus(player);
            Verbatim.gameContext.sendMessage(player, text("You were automatically removed from channel '").withColor(GameColor.RED)
                .append(text(channelName).withColor(GameColor.YELLOW))
                .append(text("' due to permission loss and it was your focus. Focused to default.").withColor(GameColor.RED)));
        } else {
            Verbatim.gameContext.sendMessage(player, text("You were automatically removed from channel '").withColor(GameColor.RED)
                .append(text(channelName).withColor(GameColor.YELLOW))
                .append(text("' due to permission loss.").withColor(GameColor.RED)));
        }
        savePlayerChannelState(player);
    }

    private static void internalLeaveChannel(GamePlayer player, String channelName) {
        joinedChannels.computeIfPresent(player.getUUID(), (k, v) -> {
            v.remove(channelName);
            return v.isEmpty() ? null : v;
        });
        if (joinedChannels.get(player.getUUID()) == null) {
            joinedChannels.remove(player.getUUID());
        }
        savePlayerChannelState(player);
    }

    public static boolean leaveChannelCmd(GamePlayer player, String channelName) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) {
            Verbatim.gameContext.sendMessage(player, text("Channel '" + channelName + "' not found.").withColor(GameColor.RED));
            return false;
        }
        if (config.alwaysOn) {
            Verbatim.gameContext.sendMessage(player, text("Cannot leave channel '").withColor(GameColor.RED)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name))
                .append(text("' as it is marked always-on.").withColor(GameColor.RED)));
            return false;
        }
        if (!isJoined(player, channelName)) {
            Verbatim.gameContext.sendMessage(player, text("You are not currently in channel: ").withColor(GameColor.YELLOW)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name)));
            return false;
        }

        internalLeaveChannel(player, channelName);
        Verbatim.gameContext.sendMessage(player, text("Left channel: ").withColor(GameColor.YELLOW)
            .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name)));

        FocusTarget currentFocus = playerFocus.get(player.getUUID());
        if (currentFocus instanceof ChatFocus && channelName.equals(((ChatFocus) currentFocus).getChannelName())) {
            playerFocus.remove(player.getUUID());
            ensurePlayerIsInADefaultFocus(player);
        }
        savePlayerChannelState(player);
        return true;
    }

    public static void focusChannel(GamePlayer player, String channelName) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus channel '" + channelName + "': Not found.").withColor(GameColor.RED));
            return;
        }

        if (config.alwaysOn || !config.permission.isPresent() || Verbatim.permissionService.hasPermission(player, config.permission.get(), CHANNEL_PERMISSION_LEVEL)) {
            boolean wasJoined = isJoined(player, channelName);
            internalJoinChannel(player, channelName, config.alwaysOn);
            playerFocus.put(player.getUUID(), ChatFocus.createChannelFocus(channelName));
            savePlayerChannelState(player);
            Verbatim.gameContext.sendMessage(player, text("Focused channel: ").withColor(GameColor.GREEN)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name)));

            if (!wasJoined && config.mature) {
                Verbatim.gameContext.sendMessage(player, Verbatim.gameContext.createWarningPrefix()
                    .append(text("WARNING: This channel may contain mature content.").withColor(GameColor.GOLD).withBold(true)));
                Verbatim.gameContext.sendMessage(player, text("By remaining in this channel, you confirm that you are 18+ and okay with seeing messages posted here.").withColor(GameColor.YELLOW));
                Verbatim.gameContext.sendMessage(player, text("If you are not comfortable with this, please leave immediately using: ").withColor(GameColor.YELLOW)
                    .append(text("/channel leave").withColor(GameColor.WHITE).withUnderlined(true)));
            }
        } else {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus channel '").withColor(GameColor.RED)
                .append(Verbatim.chatFormatter.parseColors(config.displayPrefix + " " + config.name))
                .append(text("': You do not have permission.").withColor(GameColor.RED)));
        }
    }

    public static void playerLoggedOut(GamePlayer player) {
        savePlayerChannelState(player);
        playerFocus.remove(player.getUUID());
        joinedChannels.remove(player.getUUID());
        lastIncomingDmSender.remove(player.getUUID());
    }

    public static void focusDm(GamePlayer player, UUID targetPlayerId) {
        GamePlayer targetPlayer = Verbatim.gameContext.getPlayerByUUID(targetPlayerId);
        if (targetPlayer == null) {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus DM: Target player is not online.").withColor(GameColor.RED));
            return;
        }

        playerFocus.put(player.getUUID(), ChatFocus.createDmFocus(targetPlayerId));
        Verbatim.gameContext.sendMessage(player, text("Focused DM with: ")
            .append(text(targetPlayer.getUsername()).withColor(GameColor.YELLOW)));
    }

    public static void focusDm(GamePlayer player, String targetPlayerName) {
        GamePlayer targetPlayer = Verbatim.gameContext.getPlayerByName(targetPlayerName);
        if (targetPlayer == null) {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus DM: Player '" + targetPlayerName + "' is not online.").withColor(GameColor.RED));
            return;
        }

        focusDm(player, targetPlayer.getUUID());
    }

    public static Optional<FocusTarget> getFocus(GamePlayer player) {
        return Optional.ofNullable(playerFocus.get(player.getUUID()));
    }

    public static void setLastIncomingDmSender(GamePlayer recipient, UUID senderId) {
        lastIncomingDmSender.put(recipient.getUUID(), senderId);
    }

    public static Optional<UUID> getLastIncomingDmSender(GamePlayer player) {
        return Optional.ofNullable(lastIncomingDmSender.get(player.getUUID()));
    }

    public static void handleDPrefix(GamePlayer player) {
        FocusTarget currentFocus = playerFocus.get(player.getUUID());
        UUID lastSender = lastIncomingDmSender.get(player.getUUID());

        if (lastSender == null) {
            Verbatim.gameContext.sendMessage(player, text("No recent DMs to reply to.").withColor(GameColor.YELLOW));
            return;
        }

        if (currentFocus instanceof ChatFocus && ((ChatFocus) currentFocus).getType() == ChatFocus.FocusType.DM) {
            UUID currentDmTarget = ((ChatFocus) currentFocus).getTargetPlayerId();
            if (!currentDmTarget.equals(lastSender)) {
                focusDm(player, lastSender);
                return;
            }
        }

        focusDm(player, lastSender);
    }

    public static List<GamePlayer> getPlayersInChannel(String channelName) {
        List<GamePlayer> playersInChannel = new ArrayList<>();
        if (channelName == null || channelName.isEmpty()) {
            return playersInChannel;
        }
        if (!channelConfigsByName.containsKey(channelName)) {
            return playersInChannel;
        }

        for (GamePlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
            if (isJoined(player, channelName)) {
                playersInChannel.add(player);
            }
        }
        return playersInChannel;
    }

    public static boolean adminKickPlayerFromChannel(GamePlayer playerToKick, String channelName, GamePlayer executor) {
        Optional<ChannelConfig> channelConfigOpt = getChannelConfigByName(channelName);

        if (!channelConfigOpt.isPresent()) {
            if (executor != null) {
                Verbatim.gameContext.sendMessage(executor, text("Channel '" + channelName + "' not found.").withColor(GameColor.RED));
            }
            return false;
        }

        ChannelConfig channelConfig = channelConfigOpt.get();

        if (channelConfig.alwaysOn) {
            if (executor != null) {
                Verbatim.gameContext.sendMessage(executor, text("Cannot kick players from '" + channelName + "' as it is an always-on channel.").withColor(GameColor.RED));
            }
            return false;
        }

        if (!isJoined(playerToKick, channelName)) {
            if (executor != null) {
                Verbatim.gameContext.sendMessage(executor, text(playerToKick.getUsername() + " is not in channel '" + channelName + "'.").withColor(GameColor.RED));
            }
            return false;
        }

        internalLeaveChannel(playerToKick, channelName);

        Verbatim.gameContext.sendMessage(playerToKick, text("You have been kicked from channel '" + channelName + "' by " + (executor != null ? executor.getUsername() : "an administrator") + ".").withColor(GameColor.YELLOW));

        FocusTarget currentFocus = playerFocus.get(playerToKick.getUUID());
        if (currentFocus instanceof ChatFocus && channelName.equals(((ChatFocus) currentFocus).getChannelName())) {
            playerFocus.remove(playerToKick.getUUID());
            ensurePlayerIsInADefaultFocus(playerToKick);
            Verbatim.gameContext.sendMessage(playerToKick, text("Your focus was reset as you were kicked from your focused channel.").withColor(GameColor.YELLOW));
        } else {
            savePlayerChannelState(playerToKick);
        }

        Verbatim.LOGGER.info("Player {} was kicked from channel {} by {}.", playerToKick.getUsername(), channelName, (executor != null ? executor.getUsername() : "CONSOLE"));
        return true;
    }

    public static GamePlayer getPlayerByUUID(UUID playerId) {
        return Verbatim.gameContext.getPlayerByUUID(playerId);
    }
}
