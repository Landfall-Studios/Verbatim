package world.landfall.verbatim.command;

import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.util.MailService;
import world.landfall.verbatim.util.NicknameService;
import world.landfall.verbatim.util.SocialService;
import world.landfall.verbatim.util.FormattingCodeUtils;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GameCommandSource;
import world.landfall.verbatim.context.GamePlayer;
import static world.landfall.verbatim.context.GameText.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
        if (SocialService.isIgnoring(sender, target.getUUID())) {
            Verbatim.gameContext.sendMessage(sender, text("You have this player ignored. Unignore them to send a DM.").withColor(GameColor.RED));
            return 0;
        }

        if (SocialService.isIgnoring(target, sender.getUUID())) {
            Verbatim.gameContext.sendMessage(sender, text("Cannot send message to this player.").withColor(GameColor.RED));
            if (SocialService.shouldNotifyBlock(target.getUUID(), sender.getUUID())) {
                Verbatim.gameContext.sendMessage(target, text("Blocked a DM from ").withColor(GameColor.GRAY)
                    .append(text(sender.getUsername()).withColor(GameColor.YELLOW))
                    .append(text(" (ignored).").withColor(GameColor.GRAY)));
            }
            return 0;
        }

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

    // === Ignore Commands ===

    public static int executeIgnoreAdd(GamePlayer player, GamePlayer target) {
        if (player.getUUID().equals(target.getUUID())) {
            Verbatim.gameContext.sendMessage(player, text("You cannot ignore yourself.").withColor(GameColor.RED));
            return 0;
        }
        if (SocialService.isIgnoring(player, target.getUUID())) {
            Verbatim.gameContext.sendMessage(player, text("You are already ignoring ").withColor(GameColor.YELLOW)
                .append(text(target.getUsername()).withColor(GameColor.WHITE))
                .append(text(".").withColor(GameColor.YELLOW)));
            return 0;
        }
        SocialService.addIgnore(player, target.getUUID());
        Verbatim.gameContext.sendMessage(player, text("Now ignoring ").withColor(GameColor.GREEN)
            .append(text(target.getUsername()).withColor(GameColor.WHITE))
            .append(text(".").withColor(GameColor.GREEN)));
        return 1;
    }

    public static int executeIgnoreRemove(GamePlayer player, String targetName) {
        Set<UUID> ignored = SocialService.getIgnoredUUIDs(player);
        if (ignored.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, text("Your ignore list is empty.").withColor(GameColor.YELLOW));
            return 0;
        }

        // Try to find by online player name first
        GamePlayer onlineTarget = Verbatim.gameContext.getPlayerByName(targetName);
        if (onlineTarget != null && ignored.contains(onlineTarget.getUUID())) {
            SocialService.removeIgnore(player, onlineTarget.getUUID());
            Verbatim.gameContext.sendMessage(player, text("No longer ignoring ").withColor(GameColor.GREEN)
                .append(text(onlineTarget.getUsername()).withColor(GameColor.WHITE))
                .append(text(".").withColor(GameColor.GREEN)));
            return 1;
        }

        // If not found online, search by name among all online players with matching UUID in ignore list
        // Since we only store UUIDs, we need them to be online to match by name
        Verbatim.gameContext.sendMessage(player, text("Player '").withColor(GameColor.RED)
            .append(text(targetName).withColor(GameColor.WHITE))
            .append(text("' not found in your ignore list or is not online.").withColor(GameColor.RED)));
        return 0;
    }

    public static int executeIgnoreList(GamePlayer player) {
        Set<UUID> ignored = SocialService.getIgnoredUUIDs(player);
        if (ignored.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, text("Your ignore list is empty.").withColor(GameColor.YELLOW));
            return 1;
        }

        GameComponent message = text("Your Ignored Players (" + ignored.size() + "):").withColor(GameColor.GOLD);

        for (UUID uuid : ignored) {
            GamePlayer onlinePlayer = Verbatim.gameContext.getPlayerByUUID(uuid);
            String displayName = onlinePlayer != null ? onlinePlayer.getUsername() : uuid.toString();
            message = message.append(text("\n ")).append(text(" x ").withColor(GameColor.RED))
                .append(text(displayName).withColor(GameColor.GRAY));
        }

        Verbatim.gameContext.sendMessage(player, message);
        return 1;
    }

    // === Favorite Commands ===

    public static int executeFavAdd(GamePlayer player, GamePlayer target) {
        if (player.getUUID().equals(target.getUUID())) {
            Verbatim.gameContext.sendMessage(player, text("You cannot favorite yourself.").withColor(GameColor.RED));
            return 0;
        }
        if (SocialService.isFavorited(player, target.getUUID())) {
            Verbatim.gameContext.sendMessage(player, text("You have already favorited ").withColor(GameColor.YELLOW)
                .append(text(target.getUsername()).withColor(GameColor.WHITE))
                .append(text(".").withColor(GameColor.YELLOW)));
            return 0;
        }
        SocialService.addFavorite(player, target.getUUID(), target.getUsername());
        Verbatim.gameContext.sendMessage(player, text("Added ").withColor(GameColor.GREEN)
            .append(text(target.getUsername()).withColor(GameColor.WHITE))
            .append(text(" to your favorites.").withColor(GameColor.GREEN)));
        return 1;
    }

    public static int executeFavRemove(GamePlayer player, String targetName) {
        Map<UUID, SocialService.FavoriteMeta> meta = SocialService.getFavoriteMeta(player);
        if (meta.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, text("Your favorites list is empty.").withColor(GameColor.YELLOW));
            return 0;
        }

        // Search by name in metadata
        for (Map.Entry<UUID, SocialService.FavoriteMeta> entry : meta.entrySet()) {
            if (entry.getValue().name.equalsIgnoreCase(targetName)) {
                SocialService.removeFavorite(player, entry.getKey());
                Verbatim.gameContext.sendMessage(player, text("Removed ").withColor(GameColor.GREEN)
                    .append(text(entry.getValue().name).withColor(GameColor.WHITE))
                    .append(text(" from your favorites.").withColor(GameColor.GREEN)));
                return 1;
            }
        }

        // Also try by online player name
        GamePlayer onlineTarget = Verbatim.gameContext.getPlayerByName(targetName);
        if (onlineTarget != null && SocialService.isFavorited(player, onlineTarget.getUUID())) {
            SocialService.removeFavorite(player, onlineTarget.getUUID());
            Verbatim.gameContext.sendMessage(player, text("Removed ").withColor(GameColor.GREEN)
                .append(text(onlineTarget.getUsername()).withColor(GameColor.WHITE))
                .append(text(" from your favorites.").withColor(GameColor.GREEN)));
            return 1;
        }

        Verbatim.gameContext.sendMessage(player, text("Player '").withColor(GameColor.RED)
            .append(text(targetName).withColor(GameColor.WHITE))
            .append(text("' not found in your favorites.").withColor(GameColor.RED)));
        return 0;
    }

    public static int executeFavList(GamePlayer player) {
        Set<UUID> favorites = SocialService.getFavoriteUUIDs(player);
        if (favorites.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, text("Your favorites list is empty.").withColor(GameColor.YELLOW));
            return 1;
        }

        Map<UUID, SocialService.FavoriteMeta> meta = SocialService.getFavoriteMeta(player);

        List<Map.Entry<UUID, SocialService.FavoriteMeta>> online = new ArrayList<>();
        List<Map.Entry<UUID, SocialService.FavoriteMeta>> offline = new ArrayList<>();

        for (UUID uuid : favorites) {
            SocialService.FavoriteMeta favMeta = meta.get(uuid);
            String name = favMeta != null ? favMeta.name : uuid.toString();
            long lastSeen = favMeta != null ? favMeta.lastSeen : 0;

            GamePlayer onlinePlayer = Verbatim.gameContext.getPlayerByUUID(uuid);
            Map.Entry<UUID, SocialService.FavoriteMeta> entry = Map.entry(uuid,
                new SocialService.FavoriteMeta(onlinePlayer != null ? onlinePlayer.getUsername() : name, lastSeen));

            if (onlinePlayer != null) {
                online.add(entry);
            } else {
                offline.add(entry);
            }
        }

        GameComponent message = text("Your Favorites (" + favorites.size() + "):").withColor(GameColor.GOLD);

        for (Map.Entry<UUID, SocialService.FavoriteMeta> entry : online) {
            message = message.append(text("\n ")).append(text(" * ").withColor(GameColor.GOLD))
                .append(text(entry.getValue().name).withColor(GameColor.GREEN))
                .append(text(" - Online").withColor(GameColor.GREEN));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a");
        for (Map.Entry<UUID, SocialService.FavoriteMeta> entry : offline) {
            message = message.append(text("\n ")).append(text(" * ").withColor(GameColor.GOLD))
                .append(text(entry.getValue().name).withColor(GameColor.GRAY));
            if (entry.getValue().lastSeen > 0) {
                message = message.append(text(" - Last seen: " + sdf.format(new Date(entry.getValue().lastSeen))).withColor(GameColor.GRAY));
            }
        }

        Verbatim.gameContext.sendMessage(player, message);
        return 1;
    }

    // === Mail Commands ===

    public static int executeMailSend(GamePlayer sender, String targetName, String message) {
        if (message == null || message.trim().isEmpty()) {
            Verbatim.gameContext.sendMessage(sender, text("Usage: /mail send <player> <message>").withColor(GameColor.RED));
            return 0;
        }

        // Resolve target: try online first, then name cache
        UUID targetUUID = null;
        String resolvedName = targetName;

        GamePlayer onlineTarget = Verbatim.gameContext.getPlayerByName(targetName);
        if (onlineTarget != null) {
            targetUUID = onlineTarget.getUUID();
            resolvedName = onlineTarget.getUsername();
        } else {
            targetUUID = MailService.resolvePlayerUUID(targetName);
        }

        if (targetUUID == null) {
            Verbatim.gameContext.sendMessage(sender, text("Player '").withColor(GameColor.RED)
                .append(text(targetName).withColor(GameColor.WHITE))
                .append(text("' not found. They must have logged in at least once.").withColor(GameColor.RED)));
            return 0;
        }

        if (targetUUID.equals(sender.getUUID())) {
            Verbatim.gameContext.sendMessage(sender, text("You cannot send mail to yourself.").withColor(GameColor.RED));
            return 0;
        }

        // Ignore checks (mirroring DM behavior)
        if (SocialService.isIgnoring(sender, targetUUID)) {
            Verbatim.gameContext.sendMessage(sender, text("You have this player ignored. Unignore them to send mail.").withColor(GameColor.RED));
            return 0;
        }
        if (onlineTarget != null && SocialService.isIgnoring(onlineTarget, sender.getUUID())) {
            Verbatim.gameContext.sendMessage(sender, text("Cannot send mail to this player.").withColor(GameColor.RED));
            if (SocialService.shouldNotifyBlock(onlineTarget.getUUID(), sender.getUUID())) {
                Verbatim.gameContext.sendMessage(onlineTarget, text("Blocked mail from ").withColor(GameColor.GRAY)
                    .append(text(sender.getUsername()).withColor(GameColor.YELLOW))
                    .append(text(" (ignored).").withColor(GameColor.GRAY)));
            }
            return 0;
        }

        boolean sent = MailService.sendMail(sender.getUUID(), sender.getUsername(), targetUUID, message.trim());
        if (!sent) {
            Verbatim.gameContext.sendMessage(sender, text("That player's mailbox is full (" + MailService.MAX_MAILBOX_SIZE + " messages max).").withColor(GameColor.RED));
            return 0;
        }

        Verbatim.gameContext.sendMessage(sender, text("Mail sent to ").withColor(GameColor.GREEN)
            .append(text(resolvedName).withColor(GameColor.WHITE))
            .append(text(".").withColor(GameColor.GREEN)));

        // Notify target if online
        if (onlineTarget != null) {
            Verbatim.gameContext.sendMessage(onlineTarget,
                Verbatim.gameContext.createInfoPrefix()
                    .append(text("You have new mail from ").withColor(GameColor.YELLOW))
                    .append(text(sender.getUsername()).withColor(GameColor.WHITE))
                    .append(text(". Type ").withColor(GameColor.YELLOW))
                    .append(text("/mail read").withColor(GameColor.AQUA))
                    .append(text(" to view.").withColor(GameColor.YELLOW)));
        }

        return 1;
    }

    public static int executeMailRead(GamePlayer player) {
        List<MailService.MailMessage> mail = MailService.getMail(player.getUUID());
        if (mail.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, text("Your mailbox is empty.").withColor(GameColor.YELLOW));
            return 1;
        }

        GameComponent message = text("Your Mail (" + mail.size() + "):").withColor(GameColor.GOLD);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a");

        // Show unread first, then read
        List<MailService.MailMessage> unread = new ArrayList<>();
        List<MailService.MailMessage> readMail = new ArrayList<>();
        for (MailService.MailMessage msg : mail) {
            if (!msg.read) unread.add(msg);
            else readMail.add(msg);
        }

        for (MailService.MailMessage msg : unread) {
            message = message.append(text("\n ").append(text("[NEW] ").withColor(GameColor.GREEN)))
                .append(text(msg.senderName).withColor(GameColor.WHITE))
                .append(text(" (" + sdf.format(new Date(msg.timestamp)) + "): ").withColor(GameColor.GRAY))
                .append(text(msg.message).withColor(GameColor.YELLOW));
        }
        for (MailService.MailMessage msg : readMail) {
            message = message.append(text("\n  "))
                .append(text(msg.senderName).withColor(GameColor.GRAY))
                .append(text(" (" + sdf.format(new Date(msg.timestamp)) + "): ").withColor(GameColor.DARK_GRAY))
                .append(text(msg.message).withColor(GameColor.GRAY));
        }

        Verbatim.gameContext.sendMessage(player, message);
        MailService.markAllRead(player.getUUID());
        return 1;
    }

    public static int executeMailClear(GamePlayer player) {
        List<MailService.MailMessage> mail = MailService.getMail(player.getUUID());
        if (mail.isEmpty()) {
            Verbatim.gameContext.sendMessage(player, text("Your mailbox is already empty.").withColor(GameColor.YELLOW));
            return 1;
        }
        MailService.clearMail(player.getUUID());
        Verbatim.gameContext.sendMessage(player, text("Mail cleared.").withColor(GameColor.GREEN));
        return 1;
    }

    public static int executeMailHelp(GameCommandSource source) {
        int unreadCount = 0;
        Optional<GamePlayer> playerOpt = source.asPlayer();
        if (playerOpt.isPresent()) {
            unreadCount = MailService.getUnreadCount(playerOpt.get().getUUID());
        }

        GameComponent helpMessage = text("Mail Commands:\n").withColor(GameColor.GOLD)
            .append(text("/mail send <player> <message>").withColor(GameColor.AQUA))
            .append(text(" - Send mail to a player (online or offline).\n").withColor(GameColor.GRAY))
            .append(text("/mail read").withColor(GameColor.AQUA))
            .append(text(" - Read your mail.\n").withColor(GameColor.GRAY))
            .append(text("/mail clear").withColor(GameColor.AQUA))
            .append(text(" - Clear all your mail.\n").withColor(GameColor.GRAY));

        if (playerOpt.isPresent() && unreadCount > 0) {
            helpMessage = helpMessage.append(text("\nYou have ").withColor(GameColor.YELLOW))
                .append(text(String.valueOf(unreadCount)).withColor(GameColor.GOLD))
                .append(text(" unread message" + (unreadCount != 1 ? "s" : "") + ".").withColor(GameColor.YELLOW));
        }

        Verbatim.gameContext.sendCommandSuccess(source, helpMessage, false);
        return 1;
    }
}
