package world.landfall.verbatim;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import world.landfall.verbatim.chat.FocusTarget;
import world.landfall.verbatim.chat.ChatFocus;
import world.landfall.verbatim.context.GameColor;
import static world.landfall.verbatim.context.GameText.*;

public class ChatChannelManager {
    // Flag to track if the manager has been properly initialized
    private static boolean isInitialized = false;
    
    private static final Map<UUID, FocusTarget> playerFocus = new HashMap<>();
    private static final Map<UUID, Set<String>> joinedChannels = new HashMap<>();
    private static final Map<UUID, UUID> lastIncomingDmSender = new HashMap<>();

    public static class ChannelConfig {
        public final String name;
        public final String displayPrefix;
        public final String shortcut;
        public final Optional<String> permission;
        public final int range; // -1 for global/no range
        public final String nameColor; 
        public final String separator; 
        public final String separatorColor; 
        public final String messageColor; 
        public final boolean alwaysOn; // If true, cannot be left via /leave and permission is IGNORED (public)
        public final boolean mature; // If true, shows mature content warning when joining
        public final Optional<String> specialChannelType; // For special channel behaviors like "local"
        public final NameStyle nameStyle; // How player names should be displayed in this channel

        public ChannelConfig(String name, String displayPrefix, String shortcut, String permission, Number range,
                             String nameColor, String separator, String separatorColor, String messageColor, Boolean alwaysOn, Boolean mature, String specialChannelType, String nameStyle) {
            this.name = name;
            this.displayPrefix = displayPrefix;
            this.shortcut = shortcut;
            this.alwaysOn = (alwaysOn == null) ? false : alwaysOn;
            this.mature = (mature == null) ? false : mature;
            // If alwaysOn is true or permission is null/empty, treat as no permission required
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

    private static final Map<String, ChannelConfig> channelConfigsByName = new HashMap<>();
    private static final Map<String, ChannelConfig> channelConfigsByShortcut = new HashMap<>();

    public static void loadConfiguredChannels() {
        channelConfigsByName.clear();
        channelConfigsByShortcut.clear();
        
        List<? extends UnmodifiableConfig> channelsFromConfig = VerbatimConfig.CHANNELS.get();
        Verbatim.LOGGER.info("Loading {} channel definitions from config.", channelsFromConfig.size());

        for (UnmodifiableConfig channelConf : channelsFromConfig) {
            try {
                String name = (String) channelConf.get("name");
                String displayPrefix = (String) channelConf.get("displayPrefix");
                String shortcut = (String) channelConf.get("shortcut");
                String permissionStr = channelConf.getOptional("permission").map(String::valueOf).orElse(null);
                Object rangeObj = channelConf.get("range"); 
                Number range = (rangeObj instanceof Number) ? (Number)rangeObj : null;
                String nameColor = channelConf.getOptional("nameColor").map(String::valueOf).orElse(null);
                String separator = channelConf.getOptional("separator").map(String::valueOf).orElse(null);
                String separatorColor = channelConf.getOptional("separatorColor").map(String::valueOf).orElse(null);
                String messageColor = channelConf.getOptional("messageColor").map(String::valueOf).orElse(null);
                Boolean alwaysOn = channelConf.getOptional("alwaysOn").map(v -> (Boolean)v).orElse(false);
                Boolean mature = channelConf.getOptional("mature").map(v -> (Boolean)v).orElse(false);
                String specialChannelType = channelConf.getOptional("specialChannelType").map(String::valueOf).orElse(null);
                String nameStyle = channelConf.getOptional("nameStyle").map(String::valueOf).orElse(null);

                if (name != null && !name.isEmpty() && displayPrefix != null && shortcut != null && !shortcut.isEmpty()) {
                    ChannelConfig parsedConfig = new ChannelConfig(name, displayPrefix, shortcut, permissionStr, range,
                                                                 nameColor, separator, separatorColor, messageColor, alwaysOn, mature, specialChannelType, nameStyle);
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
                    Verbatim.LOGGER.warn("Invalid channel definition (values not matching expected types or missing after validation) from UnmodifiableConfig: {}. Skipping.", channelConf.valueMap());
                }
            } catch (Exception e) { 
                Verbatim.LOGGER.error("Unexpected error parsing channel definition from UnmodifiableConfig: {}", channelConf.valueMap(), e);
            }
        }
        Verbatim.LOGGER.info("Finished loading chat channels. Total loaded: {}", channelConfigsByName.size());
        // Mark as initialized after successful loading
        isInitialized = true;
        // After reloading configs, re-evaluate joined channels for all online players
        // This is now primarily handled by ChatEvents.onConfigReload to also handle focusing default
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
        String defaultChannelName = VerbatimConfig.DEFAULT_CHANNEL_NAME.get();
        ChannelConfig defaultConfig = channelConfigsByName.get(defaultChannelName);
        if (defaultConfig == null) {
            Verbatim.LOGGER.warn("[ChatChannelManager] Default channel named '{}' not found. Falling back.", defaultChannelName);
            if (!channelConfigsByName.isEmpty()) {
                defaultConfig = channelConfigsByName.values().stream().filter(c -> c.alwaysOn).findFirst()
                                .orElse(channelConfigsByName.values().iterator().next()); // Prefer alwaysOn as default fallback
                Verbatim.LOGGER.warn("[ChatChannelManager] Using first available (preferably alwaysOn) channel '{}' as fallback default.", defaultConfig.name);
            } else {
                Verbatim.LOGGER.error("[ChatChannelManager] CRITICAL: No channels loaded. Cannot determine a default channel.");
                return null; 
            }
        }
        return defaultConfig;
    }

    public static void playerLoggedIn(ServerPlayer player) {
        loadPlayerChannelState(player);
        ensurePlayerIsInADefaultFocus(player);
    }
    
    private static void loadPlayerChannelState(ServerPlayer player) {
        Set<String> loadedJoinedChannels = new HashSet<>();
        String loadedFocusedChannel = null;
        try {
            if (player.getPersistentData().contains("verbatim:joined_channels")) {
                String[] joined = player.getPersistentData().getString("verbatim:joined_channels").split(",");
                for (String chName : joined) {
                    if (!chName.isEmpty() && channelConfigsByName.containsKey(chName)) {
                        loadedJoinedChannels.add(chName);
                    }
                }
            }
            if (player.getPersistentData().contains("verbatim:focused_channel")) {
                loadedFocusedChannel = player.getPersistentData().getString("verbatim:focused_channel");
                if (!channelConfigsByName.containsKey(loadedFocusedChannel)) {
                    loadedFocusedChannel = null; // Invalid focused channel
                }
            }
        } catch (Exception e) {
            Verbatim.LOGGER.error("[ChatChannelManager] Error loading player channel state for {}: {}", Verbatim.gameContext.getPlayerUsername(player), e.getMessage());
        }

        joinedChannels.put(Verbatim.gameContext.getPlayerUUID(player), loadedJoinedChannels);

        // Ensure all alwaysOn channels are joined by default, and permission is checked for others
        for (ChannelConfig config : channelConfigsByName.values()) {
            if (config.alwaysOn) {
                boolean wasJoined = loadedJoinedChannels.contains(config.name);
                internalJoinChannel(player, config.name, true); // Force join alwaysOn, skip permission check
                
                // Show mature content warning if this is a mature channel and the player wasn't already joined
                if (!wasJoined && config.mature) {
                    Verbatim.gameContext.sendMessage(player, text("⚠ WARNING: This channel may contain mature content!").withColor(GameColor.GOLD).withBold(true));
                    Verbatim.gameContext.sendMessage(player, text("By remaining in this channel, you confirm that you are 18+ and okay with seeing messages posted here.").withColor(GameColor.YELLOW));
                    Verbatim.gameContext.sendMessage(player, text("If you are not comfortable with this, please leave immediately using: ").withColor(GameColor.YELLOW)
                        .append(text("/channel leave").withColor(GameColor.WHITE).withUnderlined(true)));
                }
            } else if (loadedJoinedChannels.contains(config.name)) {
                // If it was in their saved list and not alwaysOn, check permission now
                // Only check permission if the channel actually has a permission requirement
                if (config.permission.isPresent() && !Verbatim.permissionService.hasPermission(player, config.permission.get(), 2)) {
                    Verbatim.LOGGER.info("[ChatChannelManager] Player {} lost permission for saved joined channel '{}' on login. Removing.", Verbatim.gameContext.getPlayerUsername(player), config.name);
                    internalLeaveChannel(player, config.name); // Silently remove, don't message yet
                }
            }
        }

        if (loadedFocusedChannel != null && getJoinedChannels(player).contains(loadedFocusedChannel)) {
            playerFocus.put(Verbatim.gameContext.getPlayerUUID(player), ChatFocus.createChannelFocus(loadedFocusedChannel));
        } else {
             playerFocus.remove(Verbatim.gameContext.getPlayerUUID(player)); // Will be set by ensurePlayerIsInADefaultFocus
        }
        savePlayerChannelState(player);
    }

    private static void ensurePlayerIsInADefaultFocus(ServerPlayer player) {
        FocusTarget currentFocus = playerFocus.get(Verbatim.gameContext.getPlayerUUID(player));
        if (currentFocus == null || !currentFocus.isValid() || 
            (currentFocus instanceof ChatFocus && !isJoined(player, ((ChatFocus) currentFocus).getChannelName()))) {
            ChannelConfig defaultChannel = getDefaultChannelConfig();
            if (defaultChannel != null) {
                Verbatim.LOGGER.info("[ChatChannelManager] Player {} focus invalid or not joined. Focusing to default '{}'.", Verbatim.gameContext.getPlayerUsername(player), defaultChannel.name);
                focusChannel(player, defaultChannel.name); // This will also join if not already
            } else {
                Verbatim.LOGGER.error("[ChatChannelManager] Player {} needs focus reset, but no default channel available!", Verbatim.gameContext.getPlayerUsername(player));
            }
        }
    }

    private static void savePlayerChannelState(ServerPlayer player) {
        Set<String> currentJoined = joinedChannels.getOrDefault(Verbatim.gameContext.getPlayerUUID(player), new HashSet<>());
        player.getPersistentData().putString("verbatim:joined_channels", String.join(",", currentJoined));
        FocusTarget currentFocused = playerFocus.get(Verbatim.gameContext.getPlayerUUID(player));
        if (currentFocused instanceof ChatFocus) {
            ChatFocus chatFocus = (ChatFocus) currentFocused;
            if (chatFocus.getType() == ChatFocus.FocusType.CHANNEL) {
                String channelName = chatFocus.getChannelName();
                if (channelName != null) {
                    player.getPersistentData().putString("verbatim:focused_channel", channelName);
                } else {
                    player.getPersistentData().remove("verbatim:focused_channel");
                }
            } else {
                // For DM focus, remove the focused_channel key since it's not a channel
                player.getPersistentData().remove("verbatim:focused_channel");
            }
        } else {
            player.getPersistentData().remove("verbatim:focused_channel");
        }
    }

    public static Set<String> getJoinedChannels(ServerPlayer player) {
        return joinedChannels.getOrDefault(Verbatim.gameContext.getPlayerUUID(player), new HashSet<>());
    }

    public static List<ChannelConfig> getJoinedChannelConfigs(ServerPlayer player) {
        return getJoinedChannels(player).stream()
            .map(ChatChannelManager::getChannelConfigByName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    public static Optional<ChannelConfig> getFocusedChannelConfig(ServerPlayer player) {
        FocusTarget focus = playerFocus.get(Verbatim.gameContext.getPlayerUUID(player));
        if (focus instanceof ChatFocus && ((ChatFocus) focus).getType() == ChatFocus.FocusType.CHANNEL) {
            return getChannelConfigByName(((ChatFocus) focus).getChannelName());
        }
        return Optional.empty();
    }

    public static boolean isJoined(ServerPlayer player, String channelName) {
        return getJoinedChannels(player).contains(channelName);
    }
    
    // Returns true if successfully joined, false if no permission or channel doesn't exist.
    private static boolean internalJoinChannel(ServerPlayer player, String channelName, boolean forceJoin) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) return false;

        // Only check permission if the channel has one and isn't being force joined
        if (!forceJoin && config.permission.isPresent() && !Verbatim.permissionService.hasPermission(player, config.permission.get(), 2)) {
            return false;
        }
        joinedChannels.computeIfAbsent(Verbatim.gameContext.getPlayerUUID(player), k -> new HashSet<>()).add(channelName);
        savePlayerChannelState(player);
        return true;
    }

    // Public facing join, with feedback messages
    public static boolean joinChannel(ServerPlayer player, String channelName) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) {
            Verbatim.gameContext.sendMessage(player, text("Channel '" + channelName + "' not found.").withColor(GameColor.RED));
            return false;
        }
        if (isJoined(player, channelName)) {
            Verbatim.gameContext.sendMessage(player, text("Already joined to channel: ").withColor(GameColor.YELLOW)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name)));
            return true; // Already joined
        }

        // Only check permission if the channel has one and isn't alwaysOn
        if (config.alwaysOn || !config.permission.isPresent() || Verbatim.permissionService.hasPermission(player, config.permission.get(), 2)) {
            internalJoinChannel(player, channelName, config.alwaysOn);
            Verbatim.gameContext.sendMessage(player, text("Joined channel: ").withColor(GameColor.GREEN)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name)));

            // Show mature content warning if this is a mature channel
            if (config.mature) {
                Verbatim.gameContext.sendMessage(player, text("⚠ WARNING: This channel may contain mature content. ⚠").withColor(GameColor.GOLD).withBold(true));
                Verbatim.gameContext.sendMessage(player, text("By remaining in this channel, you confirm that you are 18+ and okay with seeing messages posted here.").withColor(GameColor.YELLOW));
                Verbatim.gameContext.sendMessage(player, text("If you are not comfortable with this, please leave immediately using: ").withColor(GameColor.YELLOW)
                    .append(text("/channel leave").withColor(GameColor.WHITE).withUnderlined(true)));
            }
            return true;
        } else {
            Verbatim.gameContext.sendMessage(player, text("You do not have permission to join channel: ").withColor(GameColor.RED)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name)));
            return false;
        }
    }
    
    // Internal leave, no feedback, bypasses alwaysOn check for auto-leave due to permission loss
    public static void autoLeaveChannel(ServerPlayer player, String channelName) {
        internalLeaveChannel(player, channelName);
        // If the channel they were auto-left from was their focus, reset focus
        FocusTarget currentFocus = playerFocus.get(Verbatim.gameContext.getPlayerUUID(player));
        if (currentFocus instanceof ChatFocus && channelName.equals(((ChatFocus) currentFocus).getChannelName())) {
            playerFocus.remove(Verbatim.gameContext.getPlayerUUID(player));
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

    private static void internalLeaveChannel(ServerPlayer player, String channelName) {
        joinedChannels.computeIfPresent(Verbatim.gameContext.getPlayerUUID(player), (k, v) -> { 
            v.remove(channelName); 
            return v.isEmpty() ? null : v; 
        });
        if (joinedChannels.get(Verbatim.gameContext.getPlayerUUID(player)) == null) {
            joinedChannels.remove(Verbatim.gameContext.getPlayerUUID(player));
        }
        // Do not remove focus here, autoLeaveChannel handles focus reset if needed.
        savePlayerChannelState(player);
    }

    // Public facing leave, with feedback, respects alwaysOn
    public static boolean leaveChannelCmd(ServerPlayer player, String channelName) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) {
            Verbatim.gameContext.sendMessage(player, text("Channel '" + channelName + "' not found.").withColor(GameColor.RED));
            return false;
        }
        if (config.alwaysOn) {
            Verbatim.gameContext.sendMessage(player, text("Cannot leave channel '").withColor(GameColor.RED)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name))
                .append(text("' as it is marked always-on.").withColor(GameColor.RED)));
            return false;
        }
        if (!isJoined(player, channelName)) {
            Verbatim.gameContext.sendMessage(player, text("You are not currently in channel: ").withColor(GameColor.YELLOW)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name)));
            return false;
        }

        internalLeaveChannel(player, channelName);
        Verbatim.gameContext.sendMessage(player, text("Left channel: ").withColor(GameColor.YELLOW)
            .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name)));

        // If they left their focused channel, reset focus to default
        FocusTarget currentFocus = playerFocus.get(Verbatim.gameContext.getPlayerUUID(player));
        if (currentFocus instanceof ChatFocus && channelName.equals(((ChatFocus) currentFocus).getChannelName())) {
            playerFocus.remove(Verbatim.gameContext.getPlayerUUID(player));
            ensurePlayerIsInADefaultFocus(player); // This will also message the player about new focus
        }
        savePlayerChannelState(player);
        return true;
    }

    public static void focusChannel(ServerPlayer player, String channelName) {
        ChannelConfig config = channelConfigsByName.get(channelName);
        if (config == null) {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus channel '" + channelName + "': Not found.").withColor(GameColor.RED));
            return;
        }

        // Only check permission if the channel has one and isn't alwaysOn
        if (config.alwaysOn || !config.permission.isPresent() || Verbatim.permissionService.hasPermission(player, config.permission.get(), 2)) {
            boolean wasJoined = isJoined(player, channelName);
            internalJoinChannel(player, channelName, config.alwaysOn); // Ensure joined (force if alwaysOn)
            playerFocus.put(Verbatim.gameContext.getPlayerUUID(player), ChatFocus.createChannelFocus(channelName));
            savePlayerChannelState(player);
            Verbatim.gameContext.sendMessage(player, text("Focused channel: ").withColor(GameColor.GREEN)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name)));

            // Show mature content warning if this is a mature channel and the player wasn't already joined
            if (!wasJoined && config.mature) {
                Verbatim.gameContext.sendMessage(player, text("⚠ WARNING: This channel may contain mature content. ⚠").withColor(GameColor.GOLD).withBold(true));
                Verbatim.gameContext.sendMessage(player, text("By remaining in this channel, you confirm that you are 18+ and okay with seeing messages posted here.").withColor(GameColor.YELLOW));
                Verbatim.gameContext.sendMessage(player, text("If you are not comfortable with this, please leave immediately using: ").withColor(GameColor.YELLOW)
                    .append(text("/channel leave").withColor(GameColor.WHITE).withUnderlined(true)));
            }
        } else {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus channel '").withColor(GameColor.RED)
                .append(ChatFormattingUtils.parseColors(config.displayPrefix + " " + config.name))
                .append(text("': You do not have permission.").withColor(GameColor.RED)));
        }
    }

    public static void playerLoggedOut(ServerPlayer player) {
        savePlayerChannelState(player); // Ensure state is saved on logout
        playerFocus.remove(Verbatim.gameContext.getPlayerUUID(player));
        joinedChannels.remove(Verbatim.gameContext.getPlayerUUID(player));
        lastIncomingDmSender.remove(Verbatim.gameContext.getPlayerUUID(player));
    }

    // New DM-related methods
    public static void focusDm(ServerPlayer player, UUID targetPlayerId) {
        ServerPlayer targetPlayer = getPlayerByUUID(targetPlayerId);
        if (targetPlayer == null) {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus DM: Target player is not online.").withColor(GameColor.RED));
            return;
        }

        playerFocus.put(Verbatim.gameContext.getPlayerUUID(player), ChatFocus.createDmFocus(targetPlayerId));
        Verbatim.gameContext.sendMessage(player, text("Focused DM with: ")
            .append(text(Verbatim.gameContext.getPlayerUsername(targetPlayer)).withColor(GameColor.YELLOW)));
    }

    public static void focusDm(ServerPlayer player, String targetPlayerName) {
        ServerPlayer targetPlayer = Verbatim.gameContext.getPlayerByName(targetPlayerName);
        if (targetPlayer == null) {
            Verbatim.gameContext.sendMessage(player, text("Cannot focus DM: Player '" + targetPlayerName + "' is not online.").withColor(GameColor.RED));
            return;
        }

        focusDm(player, targetPlayer.getUUID());
    }
    
    public static Optional<FocusTarget> getFocus(ServerPlayer player) {
        return Optional.ofNullable(playerFocus.get(Verbatim.gameContext.getPlayerUUID(player)));
    }
    
    public static void setLastIncomingDmSender(ServerPlayer recipient, UUID senderId) {
        lastIncomingDmSender.put(recipient.getUUID(), senderId);
    }
    
    public static Optional<UUID> getLastIncomingDmSender(ServerPlayer player) {
        return Optional.ofNullable(lastIncomingDmSender.get(Verbatim.gameContext.getPlayerUUID(player)));
    }
    
    public static void handleDPrefix(ServerPlayer player) {
        FocusTarget currentFocus = playerFocus.get(Verbatim.gameContext.getPlayerUUID(player));
        UUID lastSender = lastIncomingDmSender.get(Verbatim.gameContext.getPlayerUUID(player));

        if (lastSender == null) {
            Verbatim.gameContext.sendMessage(player, text("No recent DMs to reply to.").withColor(GameColor.YELLOW));
            return;
        }
        
        // If currently in DM mode with someone different than last sender, switch to last sender
        if (currentFocus instanceof ChatFocus && ((ChatFocus) currentFocus).getType() == ChatFocus.FocusType.DM) {
            UUID currentDmTarget = ((ChatFocus) currentFocus).getTargetPlayerId();
            if (!currentDmTarget.equals(lastSender)) {
                focusDm(player, lastSender);
                return;
            }
        }
        
        // If not in DM mode or already DMing the last sender, focus on last sender
        focusDm(player, lastSender);
    }

    public static List<ServerPlayer> getPlayersInChannel(MinecraftServer server, String channelName) {
        List<ServerPlayer> playersInChannel = new ArrayList<>();
        if (channelName == null || channelName.isEmpty()) {
            return playersInChannel; // Return empty list if channelName is invalid
        }
        // Check if the channel itself exists, otherwise no point iterating players
        if (!channelConfigsByName.containsKey(channelName)) {
            return playersInChannel;
        }

        for (ServerPlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
            if (isJoined(player, channelName)) {
                playersInChannel.add(player);
            }
        }
        return playersInChannel;
    }

    public static boolean adminKickPlayerFromChannel(ServerPlayer playerToKick, String channelName, ServerPlayer executor) {
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
                Verbatim.gameContext.sendMessage(executor, text(Verbatim.gameContext.getPlayerUsername(playerToKick) + " is not in channel '" + channelName + "'.").withColor(GameColor.RED));
            }
            return false;
        }

        // Perform the kick
        internalLeaveChannel(playerToKick, channelName); // This handles removing from joinedChannels and saving state

        // Notify the kicked player
        Verbatim.gameContext.sendMessage(playerToKick, text("You have been kicked from channel '" + channelName + "' by " + (executor != null ? Verbatim.gameContext.getPlayerUsername(executor) : "an administrator") + ".").withColor(GameColor.YELLOW));

        // Check if the kicked channel was the player's focus and reset if necessary
        FocusTarget currentFocus = playerFocus.get(Verbatim.gameContext.getPlayerUUID(playerToKick));
        if (currentFocus instanceof ChatFocus && channelName.equals(((ChatFocus) currentFocus).getChannelName())) {
            playerFocus.remove(Verbatim.gameContext.getPlayerUUID(playerToKick));
            ensurePlayerIsInADefaultFocus(playerToKick); // This will also save player state
            Verbatim.gameContext.sendMessage(playerToKick, text("Your focus was reset as you were kicked from your focused channel.").withColor(GameColor.YELLOW));
        } else {
            savePlayerChannelState(playerToKick); // Ensure state is saved even if focus didn't change
        }

        Verbatim.LOGGER.info("Player {} was kicked from channel {} by {}.", Verbatim.gameContext.getPlayerUsername(playerToKick), channelName, (executor != null ? Verbatim.gameContext.getPlayerUsername(executor) : "CONSOLE"));
        return true;
    }

    public static ServerPlayer getPlayerByUUID(UUID playerId) {
        return Verbatim.gameContext.getPlayerByUUID(playerId);
    }
}
