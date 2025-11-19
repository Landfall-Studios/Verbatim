package world.landfall.verbatim;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import world.landfall.verbatim.chat.FocusTarget;
import world.landfall.verbatim.chat.ChatFocus;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.specialchannels.FormattedMessageDetails;
import world.landfall.verbatim.specialchannels.LocalChannelFormatter;
import world.landfall.verbatim.discord.DiscordBot;
import static world.landfall.verbatim.context.GameText.*;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public class ChatEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!ChatChannelManager.isInitialized()) {
                Verbatim.LOGGER.warn("[Verbatim ChatEvent] ChatChannelManager not yet initialized during login for {}. Skipping channel setup.", Verbatim.gameContext.getPlayerUsername(player));
                return;
            }

            ChatChannelManager.playerLoggedIn(player);

            if (VerbatimConfig.CUSTOM_JOIN_LEAVE_MESSAGES_ENABLED.get()) {
                String format = VerbatimConfig.JOIN_MESSAGE_FORMAT.get();
                String displayName = Verbatim.gameContext.getPlayerDisplayName(player);
                String username = Verbatim.gameContext.getPlayerUsername(player);
                String nickname = world.landfall.verbatim.util.NicknameService.getNickname(player);
                if (nickname == null) {
                    nickname = displayName;
                }

                String formatted = format
                    .replace("{player}", displayName)
                    .replace("{username}", username)
                    .replace("{nickname}", nickname);

                GameComponent message = ChatFormattingUtils.parseColors(formatted);

                Verbatim.gameContext.broadcastMessage(message, false);
            }

            if (DiscordBot.isEnabled()) {
                DiscordBot.sendPlayerConnectionStatusToDiscord(player, true);
            }

            ChatChannelManager.getFocus(player).ifPresent(focus -> {
                if (focus instanceof ChatFocus && ((ChatFocus) focus).getType() == ChatFocus.FocusType.CHANNEL) {
                    ChatChannelManager.ChannelConfig config = ChatChannelManager.getChannelConfigByName(((ChatFocus) focus).getChannelName()).orElse(null);
                    if (config != null) {
                        Verbatim.gameContext.sendMessage(player,
                            Verbatim.gameContext.createText("🗨 Focused channel: ")
                                .append(ChatFormattingUtils.parseColors(config.displayPrefix))
                                .append(Verbatim.gameContext.createText(" " + config.name).withColor(GameColor.YELLOW))
                        );
                    }
                } else if (focus instanceof ChatFocus && ((ChatFocus) focus).getType() == ChatFocus.FocusType.DM) {
                    Verbatim.gameContext.sendMessage(player,
                        Verbatim.gameContext.createText("💬 Focused DM: ")
                            .append(Verbatim.gameContext.createText(focus.getDisplayName()).withColor(GameColor.YELLOW))
                    );
                }
            });

            Set<String> joinedChannels = ChatChannelManager.getJoinedChannels(player);
            if (!joinedChannels.isEmpty()) {
                Verbatim.gameContext.sendMessage(player, Verbatim.gameContext.createText("📞 Joined channels: ").withColor(GameColor.GRAY));
                for (String joinedChannelName : joinedChannels) {
                    ChatChannelManager.getChannelConfigByName(joinedChannelName).ifPresent(jc -> {
                        Verbatim.gameContext.sendMessage(player,
                            Verbatim.gameContext.createText("  - ")
                                .append(ChatFormattingUtils.parseColors(jc.displayPrefix))
                                .append(Verbatim.gameContext.createText(" " + jc.name).withColor(GameColor.DARK_AQUA)));
                    });
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (VerbatimConfig.CUSTOM_JOIN_LEAVE_MESSAGES_ENABLED.get()) {
                String format = VerbatimConfig.LEAVE_MESSAGE_FORMAT.get();
                String displayName = Verbatim.gameContext.getPlayerDisplayName(player);
                String username = Verbatim.gameContext.getPlayerUsername(player);
                String nickname = world.landfall.verbatim.util.NicknameService.getNickname(player);
                if (nickname == null) {
                    nickname = displayName;
                }

                String formatted = format
                    .replace("{player}", displayName)
                    .replace("{username}", username)
                    .replace("{nickname}", nickname);

                GameComponent message = ChatFormattingUtils.parseColors(formatted);

                Verbatim.gameContext.broadcastMessage(message, false);
            }

            if (DiscordBot.isEnabled()) {
                DiscordBot.sendPlayerConnectionStatusToDiscord(player, false);
            }
            ChatChannelManager.playerLoggedOut(player);
            world.landfall.verbatim.util.NicknameService.onPlayerLogout(Verbatim.gameContext.getPlayerUUID(player));
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String rawMessageText = event.getMessage().getString();
        Verbatim.LOGGER.debug("[Verbatim ChatEvent] Raw message from {}: {}", Verbatim.gameContext.getPlayerUsername(sender), rawMessageText);
        event.setCanceled(true);

        if (!ChatChannelManager.isInitialized()) {
            Verbatim.LOGGER.warn("[Verbatim ChatEvent] ChatChannelManager not yet initialized. Deferring message from {}.", Verbatim.gameContext.getPlayerUsername(sender));
            Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Chat system is still initializing. Please try again in a moment.").withColor(GameColor.YELLOW));
            return;
        }

        String messageContent = rawMessageText;
        Optional<FocusTarget> targetFocusOpt = Optional.empty();

        int colonIndex = rawMessageText.indexOf(':');
        int semicolonIndex = rawMessageText.indexOf(';');
        int separatorIndex = -1;

        if (colonIndex != -1 && semicolonIndex != -1) {
            separatorIndex = Math.min(colonIndex, semicolonIndex);
        } else if (colonIndex != -1) {
            separatorIndex = colonIndex;
        } else if (semicolonIndex != -1) {
            separatorIndex = semicolonIndex;
        }

        if (separatorIndex != -1 && separatorIndex > 0) { 
            String potentialPrefix = rawMessageText.substring(0, separatorIndex);
            
            if ("d".equals(potentialPrefix)) {
                ChatChannelManager.handleDPrefix(sender);
                messageContent = rawMessageText.substring(separatorIndex + 1).trim();
                
                if (messageContent.isEmpty()) {
                    Verbatim.LOGGER.debug("[Verbatim ChatEvent] d: prefix used with no message. Focus changed only.");
                    return; 
                }
                
                Optional<FocusTarget> currentFocus = ChatChannelManager.getFocus(sender);
                if (currentFocus.isPresent() && currentFocus.get() instanceof ChatFocus && ((ChatFocus) currentFocus.get()).getType() == ChatFocus.FocusType.DM) {
                    targetFocusOpt = currentFocus;
                } else {
                    Verbatim.LOGGER.debug("[Verbatim ChatEvent] d: prefix failed to establish DM focus. Message not sent.");
                    return;
                }
            }
            else if ("g".equals(potentialPrefix)) { 
                ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                if (defaultChannel != null) {
                    ChatChannelManager.focusChannel(sender, defaultChannel.name); 
                    targetFocusOpt = Optional.of(ChatFocus.createChannelFocus(defaultChannel.name));
                    messageContent = rawMessageText.substring(separatorIndex + 1).trim();
                    
                    if (messageContent.isEmpty()) {
                        Verbatim.LOGGER.debug("[Verbatim ChatEvent] g: prefix used with no message. Focus changed only.");
                        return; 
                    }
                } else {
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("No default channel configured for 'g:' prefix.").withColor(GameColor.RED));
                    return;
                }
            }
            else {
                Optional<ChatChannelManager.ChannelConfig> targetChannelByShortcut = ChatChannelManager.getChannelConfigByShortcut(potentialPrefix);

                if (targetChannelByShortcut.isPresent()) {
                    ChatChannelManager.ChannelConfig prospectiveChannel = targetChannelByShortcut.get();
                    Verbatim.LOGGER.debug("[Verbatim ChatEvent] Shortcut '{}' targets channel: {}. Checking permission...", potentialPrefix, prospectiveChannel.name);
                    
                    ChatChannelManager.focusChannel(sender, prospectiveChannel.name);
                    
                    if (ChatChannelManager.isJoined(sender, prospectiveChannel.name)) {
                        targetFocusOpt = Optional.of(ChatFocus.createChannelFocus(prospectiveChannel.name));
                        messageContent = rawMessageText.substring(separatorIndex + 1).trim();
                        Verbatim.LOGGER.debug("[Verbatim ChatEvent] Shortcut permission GRANTED for '{}'. Player focused. Message content: \"{}\"", prospectiveChannel.name, messageContent);
                        
                        if (messageContent.isEmpty()) {
                            Verbatim.LOGGER.debug("[Verbatim ChatEvent] Message content empty after shortcut processing for '{}'. No message to send.", prospectiveChannel.name);
                            return; 
                        }
                    } else {
                        return; 
                    }
                } else {
                    Verbatim.LOGGER.debug("[Verbatim ChatEvent] No channel found for shortcut: {}", potentialPrefix);
                }
            }
        }

        if (targetFocusOpt.isEmpty()) {
            targetFocusOpt = ChatChannelManager.getFocus(sender);
            if (targetFocusOpt.isEmpty()) {
                Verbatim.LOGGER.error("[Verbatim ChatEvent] Player {} has no focus. Attempting to set to default.", Verbatim.gameContext.getPlayerUsername(sender));
                ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                if (defaultChannel != null) {
                    ChatChannelManager.focusChannel(sender, defaultChannel.name);
                    targetFocusOpt = Optional.of(ChatFocus.createChannelFocus(defaultChannel.name));
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("You were not focused on anything. Message sent to default: ")
                        .append(ChatFormattingUtils.parseColors(defaultChannel.displayPrefix))
                        .append(Verbatim.gameContext.createText(" " + defaultChannel.name).withColor(GameColor.YELLOW)));
                } else {
                    Verbatim.LOGGER.error("[Verbatim ChatEvent] CRITICAL: No default channel to focus for {}. Cannot send message.", Verbatim.gameContext.getPlayerUsername(sender));
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Error: No active or default channel. Message not sent.").withColor(GameColor.RED));
                    return;
                }
            }
        }
        
        FocusTarget finalTarget = targetFocusOpt.get();

        try {
            if (finalTarget instanceof ChatFocus && ((ChatFocus) finalTarget).getType() == ChatFocus.FocusType.DM) {
                ChatFocus dmFocus = (ChatFocus) finalTarget;
                ServerPlayer targetPlayer = ChatChannelManager.getPlayerByUUID(dmFocus.getTargetPlayerId());

                if (targetPlayer == null) {
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Cannot send DM: Target player is not online.").withColor(GameColor.RED));
                    return;
                }

                ChatChannelManager.setLastIncomingDmSender(targetPlayer, Verbatim.gameContext.getPlayerUUID(sender));

                GameComponent senderMessage = Verbatim.gameContext.createText("[You -> ")
                    .withColor(GameColor.LIGHT_PURPLE)
                    .append(Verbatim.gameContext.createText(Verbatim.gameContext.getPlayerUsername(targetPlayer)).withColor(GameColor.YELLOW))
                    .append(Verbatim.gameContext.createText("]: ").withColor(GameColor.LIGHT_PURPLE))
                    .append(ChatFormattingUtils.parsePlayerInputWithPermissions("&f", messageContent, sender));

                GameComponent recipientMessage = Verbatim.gameContext.createText("[")
                    .withColor(GameColor.LIGHT_PURPLE)
                    .append(Verbatim.gameContext.createText(Verbatim.gameContext.getPlayerUsername(sender)).withColor(GameColor.YELLOW))
                    .append(Verbatim.gameContext.createText(" -> You]: ").withColor(GameColor.LIGHT_PURPLE))
                    .append(ChatFormattingUtils.parsePlayerInputWithPermissions("&f", messageContent, sender));

                Verbatim.gameContext.sendMessage(sender, senderMessage);
                Verbatim.gameContext.sendMessage(targetPlayer, recipientMessage);
                return;
            }

            if (finalTarget instanceof ChatFocus && ((ChatFocus) finalTarget).getType() == ChatFocus.FocusType.CHANNEL) {
                ChatFocus channelFocus = (ChatFocus) finalTarget;
                Optional<ChatChannelManager.ChannelConfig> channelConfigOpt = ChatChannelManager.getChannelConfigByName(channelFocus.getChannelName());
                
                if (channelConfigOpt.isEmpty()) {
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Error: Focused channel no longer exists.").withColor(GameColor.RED));
                    return;
                }
                
                ChatChannelManager.ChannelConfig finalTargetChannel = channelConfigOpt.get();

                if (!finalTargetChannel.alwaysOn && finalTargetChannel.permission.isPresent() && !Verbatim.permissionService.hasPermission(sender, finalTargetChannel.permission.get(), 2)) {
                    Verbatim.LOGGER.info("[Verbatim ChatEvent] Player {} lost permission to send to target channel '{}'. Auto-leaving & focusing default.", Verbatim.gameContext.getPlayerUsername(sender), finalTargetChannel.name);
                    ChatChannelManager.autoLeaveChannel(sender, finalTargetChannel.name);
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("You no longer have permission to send messages in '")
                        .append(ChatFormattingUtils.parseColors(finalTargetChannel.displayPrefix + " " + finalTargetChannel.name))
                        .append(Verbatim.gameContext.createText("'. Message not sent.").withColor(GameColor.RED)));
                    return;
                }

                if (DiscordBot.isEnabled() && "global".equals(finalTargetChannel.name)) {
                    DiscordBot.sendPlayerChatMessageToDiscord(sender, messageContent);
                }

                Optional<FormattedMessageDetails> specialFormatResult = LocalChannelFormatter.formatLocalMessage(sender, finalTargetChannel, messageContent);

                GameComponent finalMessage;
                int effectiveRange;

                if (specialFormatResult.isPresent()) {
                    FormattedMessageDetails details = specialFormatResult.get();
                    finalMessage = details.getFormattedMessageAsGameComponent();
                    effectiveRange = details.effectiveRange;
                } else {
                    effectiveRange = finalTargetChannel.range;
                    finalMessage = empty()
                        .append(ChatFormattingUtils.parseColors(finalTargetChannel.displayPrefix))
                        .append(text(" "))
                        .append(ChatFormattingUtils.createPlayerNameComponent(sender, finalTargetChannel.nameColor, false, finalTargetChannel.nameStyle))
                        .append(ChatFormattingUtils.parseColors(finalTargetChannel.separatorColor + finalTargetChannel.separator))
                        .append(ChatFormattingUtils.parsePlayerInputWithPermissions(finalTargetChannel.messageColor, messageContent, sender));
                }

                MinecraftServer server = Verbatim.gameContext.getServer();
                if (server == null) {
                    Verbatim.LOGGER.error("[Verbatim ChatEvent] Server instance is null while processing message from {}", Verbatim.gameContext.getPlayerUsername(sender));
                    return;
                }

                for (ServerPlayer recipient : Verbatim.gameContext.getAllOnlinePlayers()) {
                    if (ChatChannelManager.isJoined(recipient, finalTargetChannel.name)) {
                        if (finalTargetChannel.alwaysOn || !finalTargetChannel.permission.isPresent() || Verbatim.permissionService.hasPermission(recipient, finalTargetChannel.permission.get(), 2)) {
                            if (effectiveRange >= 0) {
                                double distSqr = Verbatim.gameContext.getDistanceSquared(recipient, sender);
                                if (recipient.equals(sender)) {
                                    Verbatim.gameContext.sendMessage(recipient, finalMessage);
                                } else {
                                    GameComponent messageToSend = specialFormatResult
                                        .map(details -> details.getMessageForDistanceAsGameComponent(distSqr))
                                        .orElseGet(() -> distSqr <= effectiveRange * effectiveRange ? finalMessage : null);

                                    if (messageToSend != null) {
                                        Verbatim.gameContext.sendMessage(recipient, messageToSend);
                                    }
                                }
                            } else {
                                Verbatim.gameContext.sendMessage(recipient, finalMessage);
                            }
                        } else {
                            Verbatim.LOGGER.info("[Verbatim ChatEvent] Recipient {} is joined to '{}' but lost permission. Auto-leaving.", Verbatim.gameContext.getPlayerUsername(recipient), finalTargetChannel.name);
                            ChatChannelManager.autoLeaveChannel(recipient, finalTargetChannel.name);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            Verbatim.LOGGER.error("[Verbatim ChatEvent] Class loading error during message processing. Chat system may still be initializing.", e);
            Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Chat system is still initializing. Please try again in a moment.").withColor(GameColor.YELLOW));
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim ChatEvent] Unexpected error during message processing.", e);
            Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("An error occurred while processing your message.").withColor(GameColor.RED));
        }
    }
    
    public static void onConfigReload() {
        ChatChannelManager.loadConfiguredChannels();
        MinecraftServer server = Verbatim.gameContext.getServer();
        if (server != null) {
            for (ServerPlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
                Set<String> currentJoined = ChatChannelManager.getJoinedChannels(player);
                for (String joinedChannelName : new HashSet<>(currentJoined)) {
                    ChatChannelManager.getChannelConfigByName(joinedChannelName).ifPresent(config -> {
                        if (!config.alwaysOn && config.permission.isPresent() && !Verbatim.permissionService.hasPermission(player, config.permission.get(), 2)) {
                            Verbatim.LOGGER.info("[Verbatim ConfigReload] Player {} lost permission for joined channel '{}' after config reload. Auto-leaving.", Verbatim.gameContext.getPlayerUsername(player), config.name);
                            ChatChannelManager.autoLeaveChannel(player, config.name);
                        }
                    });
                }

                for (ChatChannelManager.ChannelConfig config : ChatChannelManager.getAllChannelConfigs()) {
                    if (config.alwaysOn) {
                        ChatChannelManager.joinChannel(player, config.name);
                    }
                }

                ChatChannelManager.getFocusedChannelConfig(player).ifPresentOrElse(focusedConfig -> {
                    if (!ChatChannelManager.isJoined(player, focusedConfig.name)) {
                        Verbatim.LOGGER.info("[Verbatim ConfigReload] Player {}'s focused channel '{}' is no longer joined. Resetting focus.", Verbatim.gameContext.getPlayerUsername(player), focusedConfig.name);
                        ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                        if (defaultChannel != null) ChatChannelManager.focusChannel(player, defaultChannel.name);
                    }
                }, () -> {
                    Verbatim.LOGGER.info("[Verbatim ConfigReload] Player {} has no focused channel. Resetting focus.", Verbatim.gameContext.getPlayerUsername(player));
                    ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                    if (defaultChannel != null) ChatChannelManager.focusChannel(player, defaultChannel.name);
                });
            }
            Verbatim.LOGGER.info("[Verbatim ConfigReload] Player channel states re-evaluated.");
        }
    }
}
