package world.landfall.verbatim.platform.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.level.ServerPlayer;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.command.VerbatimCommandHandlers;
import world.landfall.verbatim.util.NicknameService;
import world.landfall.verbatim.context.GameColor;
import static world.landfall.verbatim.context.GameText.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Forge 1.20.1 Brigadier command registration.
 * Wraps Minecraft types and delegates to VerbatimCommandHandlers.
 */
public class ForgeCommandRegistrar {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /channels
        LiteralArgumentBuilder<CommandSourceStack> listAllChannelsCommand =
            Commands.literal("channels")
                .executes(context -> VerbatimCommandHandlers.listChannels(wrapSource(context.getSource())));
        dispatcher.register(listAllChannelsCommand);

        // /channel
        LiteralArgumentBuilder<CommandSourceStack> channelCommand = Commands.literal("channel")
            .then(Commands.literal("help")
                .executes(context -> VerbatimCommandHandlers.showHelp(wrapSource(context.getSource()))))
            .then(Commands.literal("list")
                .executes(context -> VerbatimCommandHandlers.listChannels(wrapSource(context.getSource()))))
            .then(Commands.literal("focus")
                .then(Commands.argument("channelName", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList()), builder))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only.")); return 0;
                        }
                        ChatChannelManager.focusChannel(new ForgeGamePlayer(player), StringArgumentType.getString(context, "channelName"));
                        return 1;
                    })))
            .then(Commands.literal("join")
                .then(Commands.argument("channelName", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList()), builder))
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only.")); return 0;
                        }
                        ChatChannelManager.joinChannel(new ForgeGamePlayer(player), StringArgumentType.getString(context, "channelName"));
                        return 1;
                    })))
            .then(Commands.literal("leave")
                .then(Commands.argument("channelName", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            return SharedSuggestionProvider.suggest(ChatChannelManager.getJoinedChannels(new ForgeGamePlayer(player)), builder);
                        } return SharedSuggestionProvider.suggest(new String[]{}, builder);
                    })
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only.")); return 0;
                        }
                        ChatChannelManager.leaveChannelCmd(new ForgeGamePlayer(player), StringArgumentType.getString(context, "channelName"));
                        return 1;
                    }))
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only.")); return 0;
                    }
                    ForgeGamePlayer gamePlayer = new ForgeGamePlayer(player);
                    ChatChannelManager.getFocusedChannelConfig(gamePlayer).ifPresentOrElse(focused -> {
                        ChatChannelManager.leaveChannelCmd(gamePlayer, focused.name);
                    }, () -> Verbatim.gameContext.sendMessage(gamePlayer, text("You are not focused on any channel to leave.").withColor(GameColor.YELLOW)));
                    return 1;
                }))
            .executes(context -> VerbatimCommandHandlers.showHelp(wrapSource(context.getSource())));

        dispatcher.register(channelCommand);
        dispatcher.register(Commands.literal(Verbatim.MODID + "channels").redirect(listAllChannelsCommand.build()));
        dispatcher.register(Commands.literal(Verbatim.MODID + "channel").redirect(channelCommand.build()));

        // /msg
        LiteralArgumentBuilder<CommandSourceStack> msgCommand = Commands.literal("msg")
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                    if (targets.isEmpty()) {
                        Verbatim.gameContext.sendMessage(new ForgeGamePlayer(sender), text("No valid player targets.").withColor(GameColor.RED));
                        return 0;
                    }
                    ChatChannelManager.focusDm(new ForgeGamePlayer(sender), targets.iterator().next().getUUID());
                    return 1;
                })
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                        String message = MessageArgument.getMessage(context, "message").getString();
                        int successes = 0;
                        ForgeGamePlayer gameSender = new ForgeGamePlayer(sender);
                        for (ServerPlayer target : targets) {
                            successes += VerbatimCommandHandlers.sendDirectMessage(gameSender, new ForgeGamePlayer(target), message);
                        }
                        return successes;
                    }))
            );

        // /tell
        LiteralArgumentBuilder<CommandSourceStack> tellCommand = Commands.literal("tell")
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                    if (targets.isEmpty()) {
                        Verbatim.gameContext.sendMessage(new ForgeGamePlayer(sender), text("No valid player targets.").withColor(GameColor.RED));
                        return 0;
                    }
                    ChatChannelManager.focusDm(new ForgeGamePlayer(sender), targets.iterator().next().getUUID());
                    return 1;
                })
                .then(Commands.argument("message", MessageArgument.message())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                        String message = MessageArgument.getMessage(context, "message").getString();
                        int successes = 0;
                        ForgeGamePlayer gameSender = new ForgeGamePlayer(sender);
                        for (ServerPlayer target : targets) {
                            successes += VerbatimCommandHandlers.sendDirectMessage(gameSender, new ForgeGamePlayer(target), message);
                        }
                        return successes;
                    }))
            );

        // /r
        LiteralArgumentBuilder<CommandSourceStack> replyCommand = Commands.literal("r")
            .then(Commands.argument("message", MessageArgument.message())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    String message = MessageArgument.getMessage(context, "message").getString();
                    return VerbatimCommandHandlers.replyToLastDm(new ForgeGamePlayer(sender), message);
                }))
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer sender)) {
                    Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                    return 0;
                }
                ChatChannelManager.handleDPrefix(new ForgeGamePlayer(sender));
                return 1;
            });

        dispatcher.register(msgCommand);
        dispatcher.register(tellCommand);
        dispatcher.register(replyCommand);

        dispatcher.register(Commands.literal("w").redirect(msgCommand.build()));

        // /list
        dispatcher.register(Commands.literal("list")
            .executes(context -> VerbatimCommandHandlers.executeCustomListCommand(wrapSource(context.getSource()))));

        // /vlist
        LiteralArgumentBuilder<CommandSourceStack> verbatimListCommand =
            Commands.literal("vlist")
                .executes(context -> VerbatimCommandHandlers.listOnlinePlayers(wrapSource(context.getSource())));
        dispatcher.register(verbatimListCommand);

        // /chlist <target>
        LiteralArgumentBuilder<CommandSourceStack> chListCommand = Commands.literal("chlist")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return Verbatim.permissionService.hasPermission(new ForgeGamePlayer(player), VerbatimCommandHandlers.PERM_ADMIN_CHLIST, 2);
                }
                return source.hasPermission(2);
            })
            .then(Commands.argument("target", StringArgumentType.string())
                .suggests((context, builder) -> {
                    List<String> suggestions = new ArrayList<>();
                    Verbatim.gameContext.getAllOnlinePlayers().forEach(player -> suggestions.add(player.getUsername()));
                    ChatChannelManager.getAllChannelConfigs().forEach(channel -> suggestions.add(channel.name));
                    return SharedSuggestionProvider.suggest(suggestions, builder);
                })
                .executes(context -> {
                    String targetName = StringArgumentType.getString(context, "target");
                    return VerbatimCommandHandlers.executeChList(wrapSource(context.getSource()), targetName);
                })
            );
        dispatcher.register(chListCommand);

        // /chkick <player> <channel>
        LiteralArgumentBuilder<CommandSourceStack> chKickCommand = Commands.literal("chkick")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return Verbatim.permissionService.hasPermission(new ForgeGamePlayer(player), VerbatimCommandHandlers.PERM_ADMIN_CHKICK, 2);
                }
                return source.hasPermission(2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("channel", StringArgumentType.string())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList()), builder))
                    .executes(context -> {
                        ServerPlayer playerToKick = EntityArgument.getPlayer(context, "player");
                        String channelName = StringArgumentType.getString(context, "channel");
                        ServerPlayer executor = context.getSource().getPlayer();
                        return VerbatimCommandHandlers.executeChKick(
                            wrapSource(context.getSource()),
                            new ForgeGamePlayer(playerToKick),
                            channelName,
                            executor != null ? new ForgeGamePlayer(executor) : null);
                    })
                )
            );
        dispatcher.register(chKickCommand);

        // /ignore
        LiteralArgumentBuilder<CommandSourceStack> ignoreCommand = Commands.literal("ignore")
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        return VerbatimCommandHandlers.executeIgnoreAdd(new ForgeGamePlayer(player), new ForgeGamePlayer(target));
                    })))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String targetName = StringArgumentType.getString(context, "player");
                        return VerbatimCommandHandlers.executeIgnoreRemove(new ForgeGamePlayer(player), targetName);
                    })))
            .then(Commands.literal("list")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    return VerbatimCommandHandlers.executeIgnoreList(new ForgeGamePlayer(player));
                }));
        dispatcher.register(ignoreCommand);

        // /fav
        LiteralArgumentBuilder<CommandSourceStack> favCommand = Commands.literal("fav")
            .then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        return VerbatimCommandHandlers.executeFavAdd(new ForgeGamePlayer(player), new ForgeGamePlayer(target));
                    })))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> {
                        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String targetName = StringArgumentType.getString(context, "player");
                        return VerbatimCommandHandlers.executeFavRemove(new ForgeGamePlayer(player), targetName);
                    })))
            .then(Commands.literal("list")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    return VerbatimCommandHandlers.executeFavList(new ForgeGamePlayer(player));
                }));
        dispatcher.register(favCommand);

        // /nick
        LiteralArgumentBuilder<CommandSourceStack> nickCommand = Commands.literal("nick")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return Verbatim.permissionService.hasPermission(new ForgeGamePlayer(player), NicknameService.PERM_NICK, 0);
                }
                return false;
            })
            .then(Commands.literal("clear")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    return VerbatimCommandHandlers.executeNickClear(new ForgeGamePlayer(player));
                }))
            .then(Commands.argument("nickname", StringArgumentType.greedyString())
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    String nickname = StringArgumentType.getString(context, "nickname");
                    return VerbatimCommandHandlers.executeNickSet(new ForgeGamePlayer(player), nickname);
                }))
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                    return 0;
                }
                return VerbatimCommandHandlers.executeNickShow(new ForgeGamePlayer(player));
            });
        dispatcher.register(nickCommand);

        // /mail
        LiteralArgumentBuilder<CommandSourceStack> mailCommand = Commands.literal("mail")
            .then(Commands.literal("send")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            String message = StringArgumentType.getString(context, "message");
                            return VerbatimCommandHandlers.executeMailSend(new ForgeGamePlayer(player), targetName, message);
                        }))))
            .then(Commands.literal("read")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    return VerbatimCommandHandlers.executeMailRead(new ForgeGamePlayer(player));
                }))
            .then(Commands.literal("clear")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    return VerbatimCommandHandlers.executeMailClear(new ForgeGamePlayer(player));
                }))
            .executes(context -> VerbatimCommandHandlers.executeMailHelp(wrapSource(context.getSource())));
        dispatcher.register(mailCommand);
        dispatcher.register(Commands.literal(Verbatim.MODID + "mail").redirect(mailCommand.build()));
    }

    private static ForgeGameCommandSource wrapSource(CommandSourceStack source) {
        return new ForgeGameCommandSource(source);
    }
}
