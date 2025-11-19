package world.landfall.verbatim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.ChatFormattingUtils;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.util.NicknameService;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;
import static world.landfall.verbatim.context.GameText.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Optional;
import java.util.ArrayList;

public class VerbatimCommands {

    // Permission Nodes for Admin Commands
    public static final String PERM_ADMIN_CHLIST = "verbatim.admin.chlist";
    public static final String PERM_ADMIN_CHKICK = "verbatim.admin.chkick";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> listAllChannelsCommand =
            Commands.literal("channels")
                .executes(context -> listChannels(context.getSource()));
        dispatcher.register(listAllChannelsCommand);

        LiteralArgumentBuilder<CommandSourceStack> channelCommand = Commands.literal("channel")
            .then(Commands.literal("help")
                .executes(context -> showHelp(context.getSource())))
            .then(Commands.literal("list") // New alias for /channels, or list joined channels
                .executes(context -> listChannels(context.getSource())))
            .then(Commands.literal("focus")
                .then(Commands.argument("channelName", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList()), builder))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only.")); return 0;
                        }
                        ChatChannelManager.focusChannel(player, StringArgumentType.getString(context, "channelName"));
                        return 1;
                    })))
            .then(Commands.literal("join")
                .then(Commands.argument("channelName", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList()), builder))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only.")); return 0;
                        }
                        ChatChannelManager.joinChannel(player, StringArgumentType.getString(context, "channelName"));
                        return 1;
                    })))
            .then(Commands.literal("leave")
                .then(Commands.argument("channelName", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            return SharedSuggestionProvider.suggest(ChatChannelManager.getJoinedChannels(player), builder);
                        } return SharedSuggestionProvider.suggest(new String[]{}, builder);
                    })
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only.")); return 0;
                        }
                        ChatChannelManager.leaveChannelCmd(player, StringArgumentType.getString(context, "channelName"));
                        return 1;
                    }))
                .executes(context -> { // /channel leave (no args) -> leave focused if not alwaysOn
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only.")); return 0;
                    }
                    ChatChannelManager.getFocusedChannelConfig(player).ifPresentOrElse(focused -> {
                        ChatChannelManager.leaveChannelCmd(player, focused.name);
                    }, () -> Verbatim.gameContext.sendMessage(player, text("You are not focused on any channel to leave.").withColor(GameColor.YELLOW)));
                    return 1;
                }))
            .executes(context -> showHelp(context.getSource()));

        dispatcher.register(channelCommand);
        dispatcher.register(Commands.literal(Verbatim.MODID + "channels").redirect(listAllChannelsCommand.build()));
        dispatcher.register(Commands.literal(Verbatim.MODID + "channel").redirect(channelCommand.build()));

        // Override vanilla DM Commands - register these AFTER vanilla commands are registered
        // This will override the vanilla /msg and /tell commands by using the SAME argument tree structure ("targets" & "message") that vanilla uses.
        LiteralArgumentBuilder<CommandSourceStack> msgCommand = Commands.literal("msg")
            .then(Commands.argument("targets", EntityArgument.players())
                // Focus-only variant (no message supplied)
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                        Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                        return 0;
                    }
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                    if (targets.isEmpty()) {
                        Verbatim.gameContext.sendMessage(sender, text("No valid player targets.").withColor(GameColor.RED));
                        return 0;
                    }
                    // Focus on the first target only (consistent with /r behaviour)
                    ChatChannelManager.focusDm(sender, targets.iterator().next().getUUID());
                    return 1;
                })
                // Variant with message text supplied
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                            Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                            return 0;
                        }
                        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                        String message = MessageArgument.getMessage(context, "message").getString();
                        int successes = 0;
                        for (ServerPlayer target : targets) {
                            successes += sendDirectMessage(sender, target, message);
                        }
                        return successes;
                    }))
            );

        LiteralArgumentBuilder<CommandSourceStack> tellCommand = Commands.literal("tell")
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                        Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                        return 0;
                    }
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                    if (targets.isEmpty()) {
                        Verbatim.gameContext.sendMessage(sender, text("No valid player targets.").withColor(GameColor.RED));
                        return 0;
                    }
                    ChatChannelManager.focusDm(sender, targets.iterator().next().getUUID());
                    return 1;
                })
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                            Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                            return 0;
                        }
                        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                        String message = MessageArgument.getMessage(context, "message").getString();
                        int successes = 0;
                        for (ServerPlayer target : targets) {
                            successes += sendDirectMessage(sender, target, message);
                        }
                        return successes;
                    }))
            );

        LiteralArgumentBuilder<CommandSourceStack> replyCommand = Commands.literal("r")
            // Variant with message -> reply immediately
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                        Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                        return 0;
                    }
                    String message = MessageArgument.getMessage(context, "message").getString();
                    return replyToLastDm(sender, message);
                }))
            // No message -> just focus last DM sender (equivalent to d: prefix)
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                    Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                    return 0;
                }
                ChatChannelManager.handleDPrefix(sender);
                return 1;
            });

        // Register our commands - these will override vanilla commands if they exist
        dispatcher.register(msgCommand);
        dispatcher.register(tellCommand);
        dispatcher.register(replyCommand);
        
        // Also register alternative names to ensure we catch all variants
        dispatcher.register(Commands.literal("w").redirect(msgCommand.build())); // /w is another common alias for whisper/msg

        // Override /list command
        dispatcher.register(Commands.literal("list")
            .executes(context -> executeCustomListCommand(context.getSource())));

        // New /vlist command
        LiteralArgumentBuilder<CommandSourceStack> verbatimListCommand =
            Commands.literal("vlist")
                .executes(context -> listOnlinePlayers(context.getSource()));
        dispatcher.register(verbatimListCommand);

        // Admin command: /chlist <target>
        LiteralArgumentBuilder<CommandSourceStack> chListCommand = Commands.literal("chlist")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return Verbatim.permissionService.hasPermission(player, PERM_ADMIN_CHLIST, 2);
                }
                return source.hasPermission(2); // Allow console or other non-players with OP level 2
            })
            .then(Commands.argument("target", StringArgumentType.string())
                .suggests((context, builder) -> {
                    List<String> suggestions = new ArrayList<>();
                    Verbatim.gameContext.getAllOnlinePlayers().forEach(player -> suggestions.add(Verbatim.gameContext.getPlayerUsername(player)));
                    ChatChannelManager.getAllChannelConfigs().forEach(channel -> suggestions.add(channel.name));
                    return SharedSuggestionProvider.suggest(suggestions, builder);
                })
                .executes(context -> {
                    String targetName = StringArgumentType.getString(context, "target");
                    return executeChList(context.getSource(), targetName);
                })
            );
        dispatcher.register(chListCommand);

        // Admin command: /chkick <player> <channel>
        LiteralArgumentBuilder<CommandSourceStack> chKickCommand = Commands.literal("chkick")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return Verbatim.permissionService.hasPermission(player, PERM_ADMIN_CHKICK, 2);
                }
                return source.hasPermission(2); // Allow console or other non-players with OP level 2
            })
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("channel", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList()), builder))
                    .executes(context -> {
                        ServerPlayer playerToKick = EntityArgument.getPlayer(context, "player");
                        String channelName = StringArgumentType.getString(context, "channel");
                        return executeChKick(context.getSource(), playerToKick, channelName);
                    })
                )
            );
        dispatcher.register(chKickCommand);

        // Nickname command: /nick [nickname]
        LiteralArgumentBuilder<CommandSourceStack> nickCommand = Commands.literal("nick")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return Verbatim.permissionService.hasPermission(player, NicknameService.PERM_NICK, 0);
                }
                return false; // Only players can use /nick
            })
            // /nick clear - clear nickname
            .then(Commands.literal("clear")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                        return 0;
                    }
                    return executeNickClear(player);
                }))
            // /nick <nickname> - set nickname
            .then(Commands.argument("nickname", StringArgumentType.greedyString())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                        return 0;
                    }
                    String nickname = StringArgumentType.getString(context, "nickname");
                    return executeNickSet(player, nickname);
                }))
            // /nick - show current nickname
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    Verbatim.gameContext.sendCommandFailure(context.getSource(), text("Players only."));
                    return 0;
                }
                return executeNickShow(player);
            });
        dispatcher.register(nickCommand);
    }

    private static int executeCustomListCommand(CommandSourceStack source) {
        List<ServerPlayer> onlinePlayers = Verbatim.gameContext.getAllOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            Verbatim.gameContext.sendCommandSuccess(source, text("There are no players currently online.").withColor(GameColor.YELLOW), true);
            return 1;
        }

        GameComponent message = text("Online Players (" + onlinePlayers.size() + "):").withColor(GameColor.GOLD);

        boolean anyPlayerHasCustomDisplayName = false;
        for (ServerPlayer player : onlinePlayers) {
            String username = Verbatim.gameContext.getPlayerUsername(player);
            String strippedDisplayName = ChatFormattingUtils.stripFormattingCodes(Verbatim.gameContext.getPlayerDisplayName(player));
            if (!username.equals(strippedDisplayName)) {
                anyPlayerHasCustomDisplayName = true;
                break;
            }
        }

        for (ServerPlayer player : onlinePlayers) {
            String username = Verbatim.gameContext.getPlayerUsername(player);
            String strippedDisplayName = ChatFormattingUtils.stripFormattingCodes(Verbatim.gameContext.getPlayerDisplayName(player));
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

    private static int listOnlinePlayers(CommandSourceStack source) {
        List<ServerPlayer> onlinePlayers = Verbatim.gameContext.getAllOnlinePlayers();

        if (onlinePlayers.isEmpty()) {
            Verbatim.gameContext.sendCommandSuccess(source, text("There are no players currently online. (vlist)").withColor(GameColor.YELLOW), true);
            return 1;
        }

        GameComponent message = text("Online Players (" + onlinePlayers.size() + ") [vlist]:\n").withColor(GameColor.GOLD);

        for (ServerPlayer player : onlinePlayers) {
            String username = Verbatim.gameContext.getPlayerUsername(player);
            String strippedDisplayName = ChatFormattingUtils.stripFormattingCodes(Verbatim.gameContext.getPlayerDisplayName(player));
            String formattedName = username;
            if (!username.equals(strippedDisplayName)) {
                formattedName = strippedDisplayName + " (" + username + ")";
            }
            message = message.append(text(" - " + formattedName + "\n").withColor(GameColor.GREEN));
        }
        Verbatim.gameContext.sendCommandSuccess(source, message, true);
        return onlinePlayers.size();
    }

    private static int listChannels(CommandSourceStack source) {
        GameComponent message = text("Available Channels (Focusable/Joinable):\n").withColor(GameColor.GOLD);
        Collection<ChatChannelManager.ChannelConfig> allChannels = ChatChannelManager.getAllChannelConfigs();
        if (allChannels.isEmpty()) {
            Verbatim.gameContext.sendCommandFailure(source, text("No chat channels are currently configured.").withColor(GameColor.RED));
            return 0;
        }
        for (ChatChannelManager.ChannelConfig channel : allChannels) {
            message = message.append(ChatFormattingUtils.parseColors(channel.displayPrefix))
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

        if (source.getEntity() instanceof ServerPlayer player) {
            Set<String> joined = ChatChannelManager.getJoinedChannels(player);
            final GameComponent[] messageHolder = {message};
            ChatChannelManager.getFocusedChannelConfig(player).ifPresent(focused -> {
                 messageHolder[0] = messageHolder[0].append(text("\nYour Focused Channel: ").withColor(GameColor.BLUE))
                    .append(ChatFormattingUtils.parseColors(focused.displayPrefix))
                    .append(text(" " + focused.name).withBold(true));
            });
            message = messageHolder[0];
            if (!joined.isEmpty()) {
                message = message.append(text("\nYour Joined Channels:\n").withColor(GameColor.BLUE));
                for (String joinedName : joined) {
                    final GameComponent[] innerHolder = {message};
                    ChatChannelManager.getChannelConfigByName(joinedName).ifPresent(jc -> {
                        innerHolder[0] = innerHolder[0].append("  - ")
                            .append(ChatFormattingUtils.parseColors(jc.displayPrefix))
                            .append(text(" " + jc.name).withColor(GameColor.DARK_AQUA)).append("\n");
                    });
                    message = innerHolder[0];
                }
            }
        }
        Verbatim.gameContext.sendCommandSuccess(source, message, false);
        return 1;
    }

    private static int showHelp(CommandSourceStack source) {
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

    private static int sendDirectMessage(ServerPlayer sender, ServerPlayer target, String message) {
        // Focus sender on target
        ChatChannelManager.focusDm(sender, Verbatim.gameContext.getPlayerUUID(target));

        // Update recipient's last incoming DM sender
        ChatChannelManager.setLastIncomingDmSender(target, Verbatim.gameContext.getPlayerUUID(sender));

        // Format and send DM messages
        GameComponent senderMessage = text("[You -> ")
            .withColor(GameColor.LIGHT_PURPLE)
            .append(text(Verbatim.gameContext.getPlayerUsername(target)).withColor(GameColor.YELLOW))
            .append(text("]: ").withColor(GameColor.LIGHT_PURPLE))
            .append(ChatFormattingUtils.parsePlayerInputWithPermissions("&f", message, sender));

        GameComponent recipientMessage = text("[")
            .withColor(GameColor.LIGHT_PURPLE)
            .append(text(Verbatim.gameContext.getPlayerUsername(sender)).withColor(GameColor.YELLOW))
            .append(text(" -> You]: ").withColor(GameColor.LIGHT_PURPLE))
            .append(ChatFormattingUtils.parsePlayerInputWithPermissions("&f", message, sender));

        Verbatim.gameContext.sendMessage(sender, senderMessage);
        Verbatim.gameContext.sendMessage(target, recipientMessage);

        Verbatim.LOGGER.debug("[Verbatim DM Command] DM sent from {} to {}: {}", Verbatim.gameContext.getPlayerUsername(sender), Verbatim.gameContext.getPlayerUsername(target), message);
        return 1;
    }

    private static int replyToLastDm(ServerPlayer sender, String message) {
        Optional<java.util.UUID> lastSenderOpt = ChatChannelManager.getLastIncomingDmSender(sender);
        if (lastSenderOpt.isEmpty()) {
            Verbatim.gameContext.sendMessage(sender, text("No recent DMs to reply to.").withColor(GameColor.YELLOW));
            return 0;
        }

        ServerPlayer target = ChatChannelManager.getPlayerByUUID(lastSenderOpt.get());
        if (target == null) {
            Verbatim.gameContext.sendMessage(sender, text("Cannot reply: Target player is not online.").withColor(GameColor.RED));
            return 0;
        }

        return sendDirectMessage(sender, target, message);
    }

    private static int executeChList(CommandSourceStack source, String targetName) {
        // Determine if targetName is a player or a channel
        MinecraftServer server = source.getServer();
        ServerPlayer targetPlayer = Verbatim.gameContext.getPlayerByName(targetName);

        if (targetPlayer != null) {
            // Target is a player, list channels they are in
            Set<String> joinedChannels = ChatChannelManager.getJoinedChannels(targetPlayer);
            if (joinedChannels.isEmpty()) {
                Verbatim.gameContext.sendCommandSuccess(source, text(Verbatim.gameContext.getPlayerUsername(targetPlayer) + " is not in any channels.").withColor(GameColor.YELLOW), false);
                return 1;
            }
            GameComponent message = text("Channels for " + Verbatim.gameContext.getPlayerUsername(targetPlayer) + ":\n").withColor(GameColor.GOLD);
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
            // Target is potentially a channel, list players in it
            Optional<ChatChannelManager.ChannelConfig> channelConfigOpt = ChatChannelManager.getChannelConfigByName(targetName);
            if (channelConfigOpt.isPresent()) {
                ChatChannelManager.ChannelConfig channelConfig = channelConfigOpt.get();
                List<ServerPlayer> playersInChannel = ChatChannelManager.getPlayersInChannel(server, targetName);

                if (playersInChannel.isEmpty()) {
                    Verbatim.gameContext.sendCommandSuccess(source, text("No players are in channel " + channelConfig.name + ".").withColor(GameColor.YELLOW), false);
                    return 1;
                }
                GameComponent message = text("Players in channel " + channelConfig.name + ":\n").withColor(GameColor.GOLD);
                for (ServerPlayer p : playersInChannel) {
                    message = message.append(text(" - " + Verbatim.gameContext.getPlayerUsername(p) + "\n").withColor(GameColor.AQUA));
                }
                Verbatim.gameContext.sendCommandSuccess(source, message, false);
            } else {
                Verbatim.gameContext.sendCommandFailure(source, text("Target '" + targetName + "' is not a valid online player or channel name."));
                return 0;
            }
        }
        return 1;
    }

    private static int executeChKick(CommandSourceStack source, ServerPlayer playerToKick, String channelName) {
        // This will call a new method in ChatChannelManager
        boolean success = ChatChannelManager.adminKickPlayerFromChannel(playerToKick, channelName, source.getPlayer()); // Pass command executor for feedback/logging
        if (success) {
            Verbatim.gameContext.sendCommandSuccess(source, text("Kicked " + Verbatim.gameContext.getPlayerUsername(playerToKick) + " from channel " + channelName + ".").withColor(GameColor.GREEN), true);
        } else {
            // ChatChannelManager should send more specific failure feedback
            // For example, if channel is alwaysOn, or player not in channel, or channel not found.
            // If ChatChannelManager doesn't send feedback for some reason, this is a fallback.
             if (!ChatChannelManager.getChannelConfigByName(channelName).map(c -> c.alwaysOn).orElse(false)) {
                 Verbatim.gameContext.sendCommandFailure(source, text("Failed to kick " + Verbatim.gameContext.getPlayerUsername(playerToKick) + " from " + channelName + ". Player might not be in it or channel is always-on."));
             }
        }
        return success ? 1 : 0;
    }

    private static int executeNickShow(ServerPlayer player) {
        String currentNickname = NicknameService.getNickname(player);
        if (currentNickname != null) {
            GameComponent message = text("Your current nickname: ").withColor(GameColor.YELLOW)
                .append(ChatFormattingUtils.parseColors(currentNickname));
            Verbatim.gameContext.sendMessage(player, message);
        } else {
            Verbatim.gameContext.sendMessage(player, text("You don't have a nickname set.").withColor(GameColor.YELLOW));
        }
        return 1;
    }

    private static int executeNickSet(ServerPlayer player, String nickname) {
        if (nickname.length() > 64) { // Reasonable length limit
            Verbatim.gameContext.sendMessage(player, text("Nickname is too long. Maximum 64 characters.").withColor(GameColor.RED));
            return 0;
        }

        String processedNickname = NicknameService.setNickname(player, nickname);

        if (processedNickname != null) {
            GameComponent message = text("Nickname set to: ").withColor(GameColor.GREEN)
                .append(ChatFormattingUtils.parseColors(processedNickname));
            Verbatim.gameContext.sendMessage(player, message);

            // Show a note about formatting permissions if they were stripped
            if (!processedNickname.equals(nickname)) {
                Verbatim.gameContext.sendMessage(player, text("Note: Color codes were removed due to missing permissions.").withColor(GameColor.GRAY));
            }
        } else {
            Verbatim.gameContext.sendMessage(player, text("Failed to set nickname.").withColor(GameColor.RED));
            return 0;
        }
        return 1;
    }

    private static int executeNickClear(ServerPlayer player) {
        if (NicknameService.hasNickname(player)) {
            NicknameService.clearNickname(player);
            Verbatim.gameContext.sendMessage(player, text("Nickname cleared.").withColor(GameColor.GREEN));
        } else {
            Verbatim.gameContext.sendMessage(player, text("You don't have a nickname set.").withColor(GameColor.YELLOW));
        }
        return 1;
    }
} 