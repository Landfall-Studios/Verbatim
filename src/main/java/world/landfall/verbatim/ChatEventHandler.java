package world.landfall.verbatim;

import world.landfall.verbatim.chat.FocusTarget;
import world.landfall.verbatim.chat.ChatFocus;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.specialchannels.FormattedMessageDetails;
import world.landfall.verbatim.discord.DiscordBot;
import world.landfall.verbatim.util.NicknameService;
import static world.landfall.verbatim.context.GameText.*;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public class ChatEventHandler {

    public static void onPlayerLogin(GamePlayer player) {
        if (!ChatChannelManager.isInitialized()) {
            Verbatim.LOGGER.warn("[Verbatim ChatEvent] ChatChannelManager not yet initialized during login for {}. Skipping channel setup.", player.getUsername());
            return;
        }

        ChatChannelManager.playerLoggedIn(player);

        if (Verbatim.gameConfig.isCustomJoinLeaveEnabled()) {
            String format = Verbatim.gameConfig.getJoinMessageFormat();
            String displayName = player.getDisplayName();
            String username = player.getUsername();
            String nickname = NicknameService.getNickname(player);
            if (nickname == null) {
                nickname = displayName;
            }

            String formatted = format
                .replace("{player}", displayName)
                .replace("{username}", username)
                .replace("{nickname}", nickname);

            GameComponent message = Verbatim.chatFormatter.parseColors(formatted);
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
                        Verbatim.gameContext.createText("\uD83D\uDDE8 Focused channel: ")
                            .append(Verbatim.chatFormatter.parseColors(config.displayPrefix))
                            .append(Verbatim.gameContext.createText(" " + config.name).withColor(GameColor.YELLOW))
                    );
                }
            } else if (focus instanceof ChatFocus && ((ChatFocus) focus).getType() == ChatFocus.FocusType.DM) {
                Verbatim.gameContext.sendMessage(player,
                    Verbatim.gameContext.createText("\uD83D\uDCAC Focused DM: ")
                        .append(Verbatim.gameContext.createText(focus.getDisplayName()).withColor(GameColor.YELLOW))
                );
            }
        });

        Set<String> joinedChannels = ChatChannelManager.getJoinedChannels(player);
        if (!joinedChannels.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, Verbatim.gameContext.createText("\uD83D\uDCDE Joined channels: ").withColor(GameColor.GRAY));
            for (String joinedChannelName : joinedChannels) {
                ChatChannelManager.getChannelConfigByName(joinedChannelName).ifPresent(jc -> {
                    Verbatim.gameContext.sendMessage(player,
                        Verbatim.gameContext.createText("  - ")
                            .append(Verbatim.chatFormatter.parseColors(jc.displayPrefix))
                            .append(Verbatim.gameContext.createText(" " + jc.name).withColor(GameColor.DARK_AQUA)));
                });
            }
        }
    }

    public static void onPlayerLogout(GamePlayer player) {
        if (Verbatim.gameConfig.isCustomJoinLeaveEnabled()) {
            String format = Verbatim.gameConfig.getLeaveMessageFormat();
            String displayName = player.getDisplayName();
            String username = player.getUsername();
            String nickname = NicknameService.getNickname(player);
            if (nickname == null) {
                nickname = displayName;
            }

            String formatted = format
                .replace("{player}", displayName)
                .replace("{username}", username)
                .replace("{nickname}", nickname);

            GameComponent message = Verbatim.chatFormatter.parseColors(formatted);
            Verbatim.gameContext.broadcastMessage(message, false);
        }

        if (DiscordBot.isEnabled()) {
            DiscordBot.sendPlayerConnectionStatusToDiscord(player, false);
        }
        ChatChannelManager.playerLoggedOut(player);
        NicknameService.onPlayerLogout(player.getUUID());
    }

    public static void onChat(GamePlayer sender, String rawMessageText) {
        Verbatim.LOGGER.debug("[Verbatim ChatEvent] Raw message from {}: {}", sender.getUsername(), rawMessageText);

        if (!ChatChannelManager.isInitialized()) {
            Verbatim.LOGGER.warn("[Verbatim ChatEvent] ChatChannelManager not yet initialized. Deferring message from {}.", sender.getUsername());
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
                    return;
                }

                Optional<FocusTarget> currentFocus = ChatChannelManager.getFocus(sender);
                if (currentFocus.isPresent() && currentFocus.get() instanceof ChatFocus && ((ChatFocus) currentFocus.get()).getType() == ChatFocus.FocusType.DM) {
                    targetFocusOpt = currentFocus;
                } else {
                    return;
                }
            } else if ("g".equals(potentialPrefix)) {
                ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                if (defaultChannel != null) {
                    ChatChannelManager.focusChannel(sender, defaultChannel.name);
                    targetFocusOpt = Optional.of(ChatFocus.createChannelFocus(defaultChannel.name));
                    messageContent = rawMessageText.substring(separatorIndex + 1).trim();

                    if (messageContent.isEmpty()) {
                        return;
                    }
                } else {
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("No default channel configured for 'g:' prefix.").withColor(GameColor.RED));
                    return;
                }
            } else {
                Optional<ChatChannelManager.ChannelConfig> targetChannelByShortcut = ChatChannelManager.getChannelConfigByShortcut(potentialPrefix);

                if (targetChannelByShortcut.isPresent()) {
                    ChatChannelManager.ChannelConfig prospectiveChannel = targetChannelByShortcut.get();
                    ChatChannelManager.focusChannel(sender, prospectiveChannel.name);

                    if (ChatChannelManager.isJoined(sender, prospectiveChannel.name)) {
                        targetFocusOpt = Optional.of(ChatFocus.createChannelFocus(prospectiveChannel.name));
                        messageContent = rawMessageText.substring(separatorIndex + 1).trim();

                        if (messageContent.isEmpty()) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }

        if (targetFocusOpt.isEmpty()) {
            targetFocusOpt = ChatChannelManager.getFocus(sender);
            if (targetFocusOpt.isEmpty()) {
                ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                if (defaultChannel != null) {
                    ChatChannelManager.focusChannel(sender, defaultChannel.name);
                    targetFocusOpt = Optional.of(ChatFocus.createChannelFocus(defaultChannel.name));
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("You were not focused on anything. Message sent to default: ")
                        .append(Verbatim.chatFormatter.parseColors(defaultChannel.displayPrefix))
                        .append(Verbatim.gameContext.createText(" " + defaultChannel.name).withColor(GameColor.YELLOW)));
                } else {
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Error: No active or default channel. Message not sent.").withColor(GameColor.RED));
                    return;
                }
            }
        }

        FocusTarget finalTarget = targetFocusOpt.get();

        try {
            if (finalTarget instanceof ChatFocus && ((ChatFocus) finalTarget).getType() == ChatFocus.FocusType.DM) {
                ChatFocus dmFocus = (ChatFocus) finalTarget;
                GamePlayer targetPlayer = ChatChannelManager.getPlayerByUUID(dmFocus.getTargetPlayerId());

                if (targetPlayer == null) {
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Cannot send DM: Target player is not online.").withColor(GameColor.RED));
                    return;
                }

                ChatChannelManager.setLastIncomingDmSender(targetPlayer, sender.getUUID());

                GameComponent senderMessage = Verbatim.gameContext.createText("[You -> ")
                    .withColor(GameColor.LIGHT_PURPLE)
                    .append(Verbatim.gameContext.createText(targetPlayer.getUsername()).withColor(GameColor.YELLOW))
                    .append(Verbatim.gameContext.createText("]: ").withColor(GameColor.LIGHT_PURPLE))
                    .append(Verbatim.chatFormatter.parsePlayerInputWithPermissions("&f", messageContent, sender));

                GameComponent recipientMessage = Verbatim.gameContext.createText("[")
                    .withColor(GameColor.LIGHT_PURPLE)
                    .append(Verbatim.gameContext.createText(sender.getUsername()).withColor(GameColor.YELLOW))
                    .append(Verbatim.gameContext.createText(" -> You]: ").withColor(GameColor.LIGHT_PURPLE))
                    .append(Verbatim.chatFormatter.parsePlayerInputWithPermissions("&f", messageContent, sender));

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
                    ChatChannelManager.autoLeaveChannel(sender, finalTargetChannel.name);
                    Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("You no longer have permission to send messages in '")
                        .append(Verbatim.chatFormatter.parseColors(finalTargetChannel.displayPrefix + " " + finalTargetChannel.name))
                        .append(Verbatim.gameContext.createText("'. Message not sent.").withColor(GameColor.RED)));
                    return;
                }

                if (DiscordBot.isEnabled() && "global".equals(finalTargetChannel.name)) {
                    DiscordBot.sendPlayerChatMessageToDiscord(sender, messageContent);
                }

                Optional<FormattedMessageDetails> specialFormatResult = Verbatim.channelFormatter.formatLocalMessage(sender, finalTargetChannel, messageContent);

                GameComponent finalMessage;
                int effectiveRange;

                if (specialFormatResult.isPresent()) {
                    FormattedMessageDetails details = specialFormatResult.get();
                    finalMessage = details.getFormattedMessage();
                    effectiveRange = details.effectiveRange;
                } else {
                    effectiveRange = finalTargetChannel.range;
                    finalMessage = empty()
                        .append(Verbatim.chatFormatter.parseColors(finalTargetChannel.displayPrefix))
                        .append(text(" "))
                        .append(Verbatim.chatFormatter.createPlayerNameComponent(sender, finalTargetChannel.nameColor, false, finalTargetChannel.nameStyle))
                        .append(Verbatim.chatFormatter.parseColors(finalTargetChannel.separatorColor + finalTargetChannel.separator))
                        .append(Verbatim.chatFormatter.parsePlayerInputWithPermissions(finalTargetChannel.messageColor, messageContent, sender));
                }

                if (!Verbatim.gameContext.isServerAvailable()) {
                    return;
                }

                for (GamePlayer recipient : Verbatim.gameContext.getAllOnlinePlayers()) {
                    if (ChatChannelManager.isJoined(recipient, finalTargetChannel.name)) {
                        if (finalTargetChannel.alwaysOn || !finalTargetChannel.permission.isPresent() || Verbatim.permissionService.hasPermission(recipient, finalTargetChannel.permission.get(), 2)) {
                            if (effectiveRange >= 0) {
                                double distSqr = Verbatim.gameContext.getDistanceSquared(recipient, sender);
                                if (recipient.equals(sender)) {
                                    Verbatim.gameContext.sendMessage(recipient, finalMessage);
                                } else {
                                    GameComponent messageToSend = specialFormatResult
                                        .map(details -> details.getMessageForDistance(distSqr))
                                        .orElseGet(() -> distSqr <= (long) effectiveRange * effectiveRange ? finalMessage : null);

                                    if (messageToSend != null) {
                                        Verbatim.gameContext.sendMessage(recipient, messageToSend);
                                    }
                                }
                            } else {
                                Verbatim.gameContext.sendMessage(recipient, finalMessage);
                            }
                        } else {
                            ChatChannelManager.autoLeaveChannel(recipient, finalTargetChannel.name);
                        }
                    }
                }
            }
        } catch (NoClassDefFoundError e) {
            Verbatim.LOGGER.error("[Verbatim ChatEvent] Class loading error during message processing.", e);
            Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("Chat system is still initializing. Please try again in a moment.").withColor(GameColor.YELLOW));
        } catch (Exception e) {
            Verbatim.LOGGER.error("[Verbatim ChatEvent] Unexpected error during message processing.", e);
            Verbatim.gameContext.sendMessage(sender, Verbatim.gameContext.createText("An error occurred while processing your message.").withColor(GameColor.RED));
        }
    }

    public static void onConfigReload() {
        ChatChannelManager.loadConfiguredChannels();
        if (Verbatim.gameContext.isServerAvailable()) {
            for (GamePlayer player : Verbatim.gameContext.getAllOnlinePlayers()) {
                Set<String> currentJoined = ChatChannelManager.getJoinedChannels(player);
                for (String joinedChannelName : new HashSet<>(currentJoined)) {
                    ChatChannelManager.getChannelConfigByName(joinedChannelName).ifPresent(config -> {
                        if (!config.alwaysOn && config.permission.isPresent() && !Verbatim.permissionService.hasPermission(player, config.permission.get(), 2)) {
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
                        ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                        if (defaultChannel != null) ChatChannelManager.focusChannel(player, defaultChannel.name);
                    }
                }, () -> {
                    ChatChannelManager.ChannelConfig defaultChannel = ChatChannelManager.getDefaultChannelConfig();
                    if (defaultChannel != null) ChatChannelManager.focusChannel(player, defaultChannel.name);
                });
            }
        }
    }
}
