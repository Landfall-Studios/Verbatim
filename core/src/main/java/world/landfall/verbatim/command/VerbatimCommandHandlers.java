package world.landfall.verbatim.command;

import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.util.NicknameService;
import world.landfall.verbatim.util.FormattingCodeUtils;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;
import static world.landfall.verbatim.context.GameText.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * Platform-independent command handler methods.
 * These are called from the platform-specific command registrar.
 */
public class VerbatimCommandHandlers {

    public static final String PERM_ADMIN_CHLIST = "verbatim.admin.chlist";
    public static final String PERM_ADMIN_CHKICK = "verbatim.admin.chkick";

    public static int executeCustomListCommand(GameCommandSource source) {
        List<GamePlayer> onlinePlayers = Verbatim.gameContext.getAllOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            Verbatim.gameContext.sendCommandSuccess(source, text("There are no players currently online.").withColor(GameColor.YELLOW), true);
            return 1;
        }

        GameComponent message = text("Online Players (" + onlinePlayers.size() + "):").withColor(GameColor.GOLD);

        boolean anyPlayerHasCustomDisplayName = false;
        for (GamePlayer player : onlinePlayers) {
            String username = player.getUsername();
            String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(player.getDisplayName());
            if (!username.equals(strippedDisplayName)) {
                anyPlayerHasCustomDisplayName = true;
                break;
            }
        }

        for (GamePlayer player : onlinePlayers) {
            String username = player.getUsername();
            String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(player.getDisplayName());
            boolean currentPlayerHasCustomDisplayName = !username.equals(strippedDisplayName);

            message = message.append("\n - ");

            String clickCommand = "/msg " + username + " ";

            if (anyPlayerHasCustomDisplayName) {
                if (currentPlayerHasCustomDisplayName) {
                    message = message.append(text(strippedDisplayName)
                        .withColor(GameColor.YELLOW)
                        .withClickSuggestCommand(clickCommand));
                    message = message.append(text(" (" + username + ")").withColor(GameColor.GRAY));
                } else {
                    message = message.append(text(username)
                        .withColor(GameColor.GRAY)
                        .withClickSuggestCommand(clickCommand));
                }
            } else {
                message = message.append(text(username)
                    .withColor(GameColor.YELLOW)
                    .withClickSuggestCommand(clickCommand));
            }
        }

        Verbatim.gameContext.sendCommandSuccess(source, message, true);
        return onlinePlayers.size();
    }

    public static int listOnlinePlayers(GameCommandSource source) {
        List<GamePlayer> onlinePlayers = Verbatim.gameContext.getAllOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            Verbatim.gameContext.sendCommandSuccess(source, text("There are no players currently online. (vlist)").withColor(GameColor.YELLOW), true);
            return 1;
        }

        GameComponent message = text("Online Players (" + onlinePlayers.size() + ") [vlist]:\n").withColor(GameColor.GOLD);

        for (GamePlayer player : onlinePlayers) {
            String username = player.getUsername();
            String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(player.getDisplayName());
            String formattedName = username;
            if (!username.equals(strippedDisplayName)) {
                formattedName = strippedDisplayName + " (" + username + ")";
            }
            message = message.append(text(" - " + formattedName + "\n").withColor(GameColor.GREEN));
        }
        Verbatim.gameContext.sendCommandSuccess(source, message, true);
        return onlinePlayers.size();
    }

    public static int listChannels(GameCommandSource source) {
        GameComponent message = text("Available Channels (Focusable/Joinable):\n").withColor(GameColor.GOLD);
        Collection<ChatChannelManager.ChannelConfig> allChannels = ChatChannelManager.getAllChannelConfigs();
        if (allChannels.isEmpty()) {
            Verbatim.gameContext.sendCommandFailure(source, text("No chat channels are currently configured.").withColor(GameColor.RED));
            return 0;
        }
        for (ChatChannelManager.ChannelConfig channel : allChannels) {
            message = message.append(Verbatim.chatFormatter.parseColors(channel.displayPrefix))
                 .append(text(" " + channel.name).withColor(GameColor.YELLOW))
                 .append(text(" (Shortcut: " + channel.shortcut + ")").withColor(GameColor.GRAY));
            if (channel.range >= 0) {
                message = message.append(text(" - Range: ").withColor(GameColor.GRAY))
                     .append(text(String.valueOf(channel.range)).withColor(GameColor.AQUA));
            }
            if (channel.alwaysOn) {
                message = message.append(text(" (Always On, Public)").withColor(GameColor.DARK_GRAY));
            } else if (channel.permission.isEmpty()) {
                 message = message.append(text(" (Public)").withColor(GameColor.GREEN));
            } else {
                 message = message.append(text(" (Permission: ").withColor(GameColor.GRAY))
                    .append(text(channel.permission.get()).withItalic(true))
                    .append(text(")").withColor(GameColor.GRAY));
            }
            message = message.append("\n");
        }

        Optional<GamePlayer> playerOpt = source.asPlayer();
        if (playerOpt.isPresent()) {
            GamePlayer player = playerOpt.get();
            Set<String> joined = ChatChannelManager.getJoinedChannels(player);
            final GameComponent[] messageHolder = {message};
            ChatChannelManager.getFocusedChannelConfig(player).ifPresent(focused -> {
                 messageHolder[0] = messageHolder[0].append(text("\nYour Focused Channel: ").withColor(GameColor.BLUE))
                    .append(Verbatim.chatFormatter.parseColors(focused.displayPrefix))
                    .append(text(" " + focused.name).withBold(true));
            });
            message = messageHolder[0];
            if (!joined.isEmpty()) {
                message = message.append(text("\nYour Joined Channels:\n").withColor(GameColor.BLUE));
                for (String joinedName : joined) {
                    final GameComponent[] innerHolder = {message};
                    ChatChannelManager.getChannelConfigByName(joinedName).ifPresent(jc -> {
                        innerHolder[0] = innerHolder[0].append("  - ")
                            .append(Verbatim.chatFormatter.parseColors(jc.displayPrefix))
                            .append(text(" " + jc.name).withColor(GameColor.DARK_AQUA)).append("\n");
                    });
                    message = innerHolder[0];
                }
            }
        }
        Verbatim.gameContext.sendCommandSuccess(source, message, false);
        return 1;
    }

    public static int showHelp(GameCommandSource source) {
        GameComponent helpMessage = text("Verbatim Channel Commands:\n").withColor(GameColor.GOLD)
            .append("/channels or /channel list - Lists all available channels & your status.\n")
            .append("/channel focus <channelName> - Sets your active typing channel (also joins it).\n")
            .append("/channel join <channelName> - Joins a channel to receive messages.\n")
            .append("/channel leave <channelName> - Leaves a joined channel.\n")
            .append("/channel leave - Leaves your currently focused channel (if not alwaysOn).\n")
            .append("/channel help - Shows this help message.\n\n")
            .append(text("Direct Message Commands:\n").withColor(GameColor.AQUA))
            .append("/msg <player> [message] - Focus DM with player (and send message if provided).\n")
            .append("/tell <player> [message] - Same as /msg.\n")
            .append("/r [message] - Reply to last DM sender (and send message if provided).\n\n")
            .append(text("Chat Prefixes:\n").withColor(GameColor.GREEN))
            .append("d: - Focus on last DM sender (same as /r without message).\n")
            .append("g: - Switch to global chat.\n")
            .append(text("Use shortcuts like ").withColor(GameColor.GRAY))
            .append(text("g: your message").withItalic(true))
            .append(text(" to send to global (if shortcut is 'g') and focus it.").withColor(GameColor.GRAY));
        Verbatim.gameContext.sendCommandSuccess(source, helpMessage, false);
        return 1;
    }

    public static int sendDirectMessage(GamePlayer sender, GamePlayer target, String message) {
        ChatChannelManager.focusDm(sender, target.getUUID());

        ChatChannelManager.setLastIncomingDmSender(target, sender.getUUID());

        GameComponent senderMessage = text("[You -> ")
            .withColor(GameColor.LIGHT_PURPLE)
            .append(text(target.getUsername()).withColor(GameColor.YELLOW))
            .append(text("]: ").withColor(GameColor.LIGHT_PURPLE))
            .append(Verbatim.chatFormatter.parsePlayerInputWithPermissions("&f", message, sender));

        GameComponent recipientMessage = text("[")
            .withColor(GameColor.LIGHT_PURPLE)
            .append(text(sender.getUsername()).withColor(GameColor.YELLOW))
            .append(text(" -> You]: ").withColor(GameColor.LIGHT_PURPLE))
            .append(Verbatim.chatFormatter.parsePlayerInputWithPermissions("&f", message, sender));

        Verbatim.gameContext.sendMessage(sender, senderMessage);
        Verbatim.gameContext.sendMessage(target, recipientMessage);

        Verbatim.LOGGER.debug("[Verbatim DM Command] DM sent from {} to {}: {}", sender.getUsername(), target.getUsername(), message);
        return 1;
    }

    public static int replyToLastDm(GamePlayer sender, String message) {
        Optional<java.util.UUID> lastSenderOpt = ChatChannelManager.getLastIncomingDmSender(sender);
        if (lastSenderOpt.isEmpty()) {
            Verbatim.gameContext.sendMessage(sender, text("No recent DMs to reply to.").withColor(GameColor.YELLOW));
            return 0;
        }

        GamePlayer target = ChatChannelManager.getPlayerByUUID(lastSenderOpt.get());
        if (target == null) {
            Verbatim.gameContext.sendMessage(sender, text("Cannot reply: Target player is not online.").withColor(GameColor.RED));
            return 0;
        }

        return sendDirectMessage(sender, target, message);
    }

    public static int executeChList(GameCommandSource source, String targetName) {
        GamePlayer targetPlayer = Verbatim.gameContext.getPlayerByName(targetName);

        if (targetPlayer != null) {
            Set<String> joinedChannels = ChatChannelManager.getJoinedChannels(targetPlayer);
            if (joinedChannels.isEmpty()) {
                Verbatim.gameContext.sendCommandSuccess(source, text(targetPlayer.getUsername() + " is not in any channels.").withColor(GameColor.YELLOW), false);
                return 1;
            }
            GameComponent message = text("Channels for " + targetPlayer.getUsername() + ":\n").withColor(GameColor.GOLD);
            for (String channelName : joinedChannels) {
                final GameComponent[] messageHolder = {message};
                ChatChannelManager.getChannelConfigByName(channelName).ifPresent(config -> {
                    messageHolder[0] = messageHolder[0].append(text(" - " + config.name).withColor(GameColor.AQUA))
                           .append(text(" (" + config.displayPrefix + ")\n").withColor(GameColor.GRAY));
                });
                message = messageHolder[0];
            }
            Verbatim.gameContext.sendCommandSuccess(source, message, false);
        } else {
            Optional<ChatChannelManager.ChannelConfig> channelConfigOpt = ChatChannelManager.getChannelConfigByName(targetName);
            if (channelConfigOpt.isPresent()) {
                ChatChannelManager.ChannelConfig channelConfig = channelConfigOpt.get();
                List<GamePlayer> playersInChannel = ChatChannelManager.getPlayersInChannel(targetName);

                if (playersInChannel.isEmpty()) {
                    Verbatim.gameContext.sendCommandSuccess(source, text("No players are in channel " + channelConfig.name + ".").withColor(GameColor.YELLOW), false);
                    return 1;
                }
                GameComponent message = text("Players in channel " + channelConfig.name + ":\n").withColor(GameColor.GOLD);
                for (GamePlayer p : playersInChannel) {
                    message = message.append(text(" - " + p.getUsername() + "\n").withColor(GameColor.AQUA));
                }
                Verbatim.gameContext.sendCommandSuccess(source, message, false);
            } else {
                Verbatim.gameContext.sendCommandFailure(source, text("Target '" + targetName + "' is not a valid online player or channel name."));
                return 0;
            }
        }
        return 1;
    }

    public static int executeChKick(GameCommandSource source, GamePlayer playerToKick, String channelName, GamePlayer executor) {
        boolean success = ChatChannelManager.adminKickPlayerFromChannel(playerToKick, channelName, executor);
        if (success) {
            Verbatim.gameContext.sendCommandSuccess(source, text("Kicked " + playerToKick.getUsername() + " from channel " + channelName + ".").withColor(GameColor.GREEN), true);
        } else {
             if (!ChatChannelManager.getChannelConfigByName(channelName).map(c -> c.alwaysOn).orElse(false)) {
                 Verbatim.gameContext.sendCommandFailure(source, text("Failed to kick " + playerToKick.getUsername() + " from " + channelName + ". Player might not be in it or channel is always-on."));
             }
        }
        return success ? 1 : 0;
    }

    public static int executeNickShow(GamePlayer player) {
        String currentNickname = NicknameService.getNickname(player);
        if (currentNickname != null) {
            GameComponent message = text("Your current nickname: ").withColor(GameColor.YELLOW)
                .append(Verbatim.chatFormatter.parseColors(currentNickname));
            Verbatim.gameContext.sendMessage(player, message);
        } else {
            Verbatim.gameContext.sendMessage(player, text("You don't have a nickname set.").withColor(GameColor.YELLOW));
        }
        return 1;
    }

    public static int executeNickSet(GamePlayer player, String nickname) {
        if (nickname.length() > 64) {
            Verbatim.gameContext.sendMessage(player, text("Nickname is too long. Maximum 64 characters.").withColor(GameColor.RED));
            return 0;
        }

        String processedNickname = NicknameService.setNickname(player, nickname);

        if (processedNickname != null) {
            GameComponent message = text("Nickname set to: ").withColor(GameColor.GREEN)
                .append(Verbatim.chatFormatter.parseColors(processedNickname));
            Verbatim.gameContext.sendMessage(player, message);

            if (!processedNickname.equals(nickname)) {
                Verbatim.gameContext.sendMessage(player, text("Note: Color codes were removed due to missing permissions.").withColor(GameColor.GRAY));
            }
        } else {
            Verbatim.gameContext.sendMessage(player, text("Failed to set nickname.").withColor(GameColor.RED));
            return 0;
        }
        return 1;
    }

    public static int executeNickClear(GamePlayer player) {
        if (NicknameService.hasNickname(player)) {
            NicknameService.clearNickname(player);
            Verbatim.gameContext.sendMessage(player, text("Nickname cleared.").withColor(GameColor.GREEN));
        } else {
            Verbatim.gameContext.sendMessage(player, text("You don't have a nickname set.").withColor(GameColor.YELLOW));
        }
        return 1;
    }
}
