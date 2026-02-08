package world.landfall.verbatim.platform.paper;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.command.VerbatimCommandHandlers;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.util.NicknameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static world.landfall.verbatim.context.GameText.text;

/**
 * Paper command registrar using Paper Brigadier API.
 *
 * Paper provides Brigadier via {@code LifecycleEvents.COMMANDS} and
 * {@code io.papermc.paper.command.brigadier.Commands}.
 * Uses {@code StringArgumentType.word()} for player names (no EntityArgument on Paper)
 * and {@code StringArgumentType.greedyString()} for messages.
 */
@SuppressWarnings("UnstableApiUsage")
public class PaperCommandRegistrar {

    public static void register(Commands commands) {
        // /channels - list all channels
        commands.register(
            Commands.literal("channels")
                .executes(context -> VerbatimCommandHandlers.listChannels(wrapSource(context.getSource())))
                .build(),
            "Lists all available chat channels"
        );

        // /channel [list|help|join|leave|focus]
        commands.register(
            Commands.literal("channel")
                .then(Commands.literal("list")
                    .executes(context -> VerbatimCommandHandlers.listChannels(wrapSource(context.getSource()))))
                .then(Commands.literal("help")
                    .executes(context -> VerbatimCommandHandlers.showHelp(wrapSource(context.getSource()))))
                .then(Commands.literal("focus")
                    .then(Commands.argument("channelName", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> names = ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList());
                            for (String name : names) {
                                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            ChatChannelManager.focusChannel(new PaperGamePlayer(player), StringArgumentType.getString(context, "channelName"));
                            return 1;
                        })))
                .then(Commands.literal("join")
                    .then(Commands.argument("channelName", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> names = ChatChannelManager.getAllChannelConfigs().stream().map(c -> c.name).collect(Collectors.toList());
                            for (String name : names) {
                                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            ChatChannelManager.joinChannel(new PaperGamePlayer(player), StringArgumentType.getString(context, "channelName"));
                            return 1;
                        })))
                .then(Commands.literal("leave")
                    .then(Commands.argument("channelName", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            if (context.getSource().getSender() instanceof Player player) {
                                for (String ch : ChatChannelManager.getJoinedChannels(new PaperGamePlayer(player))) {
                                    if (ch.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                        builder.suggest(ch);
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            ChatChannelManager.leaveChannelCmd(new PaperGamePlayer(player), StringArgumentType.getString(context, "channelName"));
                            return 1;
                        }))
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        PaperGamePlayer gamePlayer = new PaperGamePlayer(player);
                        ChatChannelManager.getFocusedChannelConfig(gamePlayer).ifPresentOrElse(
                            focused -> ChatChannelManager.leaveChannelCmd(gamePlayer, focused.name),
                            () -> Verbatim.gameContext.sendMessage(gamePlayer, text("You are not focused on any channel to leave.").withColor(GameColor.YELLOW))
                        );
                        return 1;
                    }))
                .executes(context -> VerbatimCommandHandlers.showHelp(wrapSource(context.getSource())))
                .build(),
            "Channel management commands"
        );

        // /msg <player> [message]
        commands.register(
            Commands.literal("msg")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(p.getName());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player sender)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String targetName = StringArgumentType.getString(context, "player");
                        Player target = Bukkit.getPlayerExact(targetName);
                        if (target == null) {
                            Verbatim.gameContext.sendMessage(new PaperGamePlayer(sender), text("Player not found.").withColor(GameColor.RED));
                            return 0;
                        }
                        ChatChannelManager.focusDm(new PaperGamePlayer(sender), target.getUniqueId());
                        return 1;
                    })
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player sender)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            String message = StringArgumentType.getString(context, "message");
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                Verbatim.gameContext.sendMessage(new PaperGamePlayer(sender), text("Player not found.").withColor(GameColor.RED));
                                return 0;
                            }
                            return VerbatimCommandHandlers.sendDirectMessage(new PaperGamePlayer(sender), new PaperGamePlayer(target), message);
                        })))
                .build(),
            "Send a direct message to a player"
        );

        // /tell (alias for /msg)
        commands.register(
            Commands.literal("tell")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(p.getName());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player sender)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String targetName = StringArgumentType.getString(context, "player");
                        Player target = Bukkit.getPlayerExact(targetName);
                        if (target == null) {
                            Verbatim.gameContext.sendMessage(new PaperGamePlayer(sender), text("Player not found.").withColor(GameColor.RED));
                            return 0;
                        }
                        ChatChannelManager.focusDm(new PaperGamePlayer(sender), target.getUniqueId());
                        return 1;
                    })
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player sender)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            String message = StringArgumentType.getString(context, "message");
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                Verbatim.gameContext.sendMessage(new PaperGamePlayer(sender), text("Player not found.").withColor(GameColor.RED));
                                return 0;
                            }
                            return VerbatimCommandHandlers.sendDirectMessage(new PaperGamePlayer(sender), new PaperGamePlayer(target), message);
                        })))
                .build(),
            "Send a direct message to a player"
        );

        // /r [message] - reply to last DM
        commands.register(
            Commands.literal("r")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player sender)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String message = StringArgumentType.getString(context, "message");
                        return VerbatimCommandHandlers.replyToLastDm(new PaperGamePlayer(sender), message);
                    }))
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player sender)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    ChatChannelManager.handleDPrefix(new PaperGamePlayer(sender));
                    return 1;
                })
                .build(),
            "Reply to the last player who messaged you"
        );

        // /w (alias for /msg)
        commands.register(
            Commands.literal("w")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(p.getName());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player sender)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String targetName = StringArgumentType.getString(context, "player");
                        Player target = Bukkit.getPlayerExact(targetName);
                        if (target == null) {
                            Verbatim.gameContext.sendMessage(new PaperGamePlayer(sender), text("Player not found.").withColor(GameColor.RED));
                            return 0;
                        }
                        ChatChannelManager.focusDm(new PaperGamePlayer(sender), target.getUniqueId());
                        return 1;
                    })
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player sender)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            String message = StringArgumentType.getString(context, "message");
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                Verbatim.gameContext.sendMessage(new PaperGamePlayer(sender), text("Player not found.").withColor(GameColor.RED));
                                return 0;
                            }
                            return VerbatimCommandHandlers.sendDirectMessage(new PaperGamePlayer(sender), new PaperGamePlayer(target), message);
                        })))
                .build(),
            "Send a direct message to a player"
        );

        // /list - override default list command
        commands.register(
            Commands.literal("list")
                .executes(context -> VerbatimCommandHandlers.executeCustomListCommand(wrapSource(context.getSource())))
                .build(),
            "Lists online players"
        );

        // /vlist
        commands.register(
            Commands.literal("vlist")
                .executes(context -> VerbatimCommandHandlers.listOnlinePlayers(wrapSource(context.getSource())))
                .build(),
            "Lists online players (Verbatim format)"
        );

        // /chlist <target> - admin command
        commands.register(
            Commands.literal("chlist")
                .requires(source -> {
                    if (source.getSender() instanceof Player player) {
                        return Verbatim.permissionService.hasPermission(new PaperGamePlayer(player), VerbatimCommandHandlers.PERM_ADMIN_CHLIST, 2);
                    }
                    return true; // Console
                })
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(p.getName());
                            }
                        }
                        for (ChatChannelManager.ChannelConfig config : ChatChannelManager.getAllChannelConfigs()) {
                            if (config.name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(config.name);
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        String targetName = StringArgumentType.getString(context, "target");
                        return VerbatimCommandHandlers.executeChList(wrapSource(context.getSource()), targetName);
                    }))
                .build(),
            "Admin: list channels for a player or players in a channel"
        );

        // /chkick <player> <channel> - admin command
        commands.register(
            Commands.literal("chkick")
                .requires(source -> {
                    if (source.getSender() instanceof Player player) {
                        return Verbatim.permissionService.hasPermission(new PaperGamePlayer(player), VerbatimCommandHandlers.PERM_ADMIN_CHKICK, 2);
                    }
                    return true; // Console
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(p.getName());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("channel", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (ChatChannelManager.ChannelConfig config : ChatChannelManager.getAllChannelConfigs()) {
                                if (config.name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(config.name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            String channelName = StringArgumentType.getString(context, "channel");
                            Player playerToKick = Bukkit.getPlayerExact(playerName);
                            if (playerToKick == null) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Player not found."));
                                return 0;
                            }
                            PaperGamePlayer executor = null;
                            if (context.getSource().getSender() instanceof Player execPlayer) {
                                executor = new PaperGamePlayer(execPlayer);
                            }
                            return VerbatimCommandHandlers.executeChKick(
                                wrapSource(context.getSource()),
                                new PaperGamePlayer(playerToKick),
                                channelName,
                                executor);
                        })))
                .build(),
            "Admin: kick a player from a channel"
        );

        // /ignore [add|remove|list]
        commands.register(
            Commands.literal("ignore")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(p.getName());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Player not found."));
                                return 0;
                            }
                            return VerbatimCommandHandlers.executeIgnoreAdd(new PaperGamePlayer(player), new PaperGamePlayer(target));
                        })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            return VerbatimCommandHandlers.executeIgnoreRemove(new PaperGamePlayer(player), targetName);
                        })))
                .then(Commands.literal("list")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        return VerbatimCommandHandlers.executeIgnoreList(new PaperGamePlayer(player));
                    }))
                .build(),
            "Manage your ignore list"
        );

        // /fav [add|remove|list]
        commands.register(
            Commands.literal("fav")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                                    builder.suggest(p.getName());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Player not found."));
                                return 0;
                            }
                            return VerbatimCommandHandlers.executeFavAdd(new PaperGamePlayer(player), new PaperGamePlayer(target));
                        })))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                return 0;
                            }
                            String targetName = StringArgumentType.getString(context, "player");
                            return VerbatimCommandHandlers.executeFavRemove(new PaperGamePlayer(player), targetName);
                        })))
                .then(Commands.literal("list")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        return VerbatimCommandHandlers.executeFavList(new PaperGamePlayer(player));
                    }))
                .build(),
            "Manage your favorites list"
        );

        // /nick [clear|<nickname>]
        commands.register(
            Commands.literal("nick")
                .requires(source -> {
                    if (source.getSender() instanceof Player player) {
                        return Verbatim.permissionService.hasPermission(new PaperGamePlayer(player), NicknameService.PERM_NICK, 0);
                    }
                    return false;
                })
                .then(Commands.literal("clear")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        return VerbatimCommandHandlers.executeNickClear(new PaperGamePlayer(player));
                    }))
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        String nickname = StringArgumentType.getString(context, "nickname");
                        return VerbatimCommandHandlers.executeNickSet(new PaperGamePlayer(player), nickname);
                    }))
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                        return 0;
                    }
                    return VerbatimCommandHandlers.executeNickShow(new PaperGamePlayer(player));
                })
                .build(),
            "Set, show, or clear your nickname"
        );

        // /mail [send|read|clear]
        commands.register(
            Commands.literal("mail")
                .then(Commands.literal("send")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(context -> {
                                if (!(context.getSource().getSender() instanceof Player player)) {
                                    Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                                    return 0;
                                }
                                String targetName = StringArgumentType.getString(context, "player");
                                String message = StringArgumentType.getString(context, "message");
                                return VerbatimCommandHandlers.executeMailSend(new PaperGamePlayer(player), targetName, message);
                            }))))
                .then(Commands.literal("read")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        return VerbatimCommandHandlers.executeMailRead(new PaperGamePlayer(player));
                    }))
                .then(Commands.literal("clear")
                    .executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player player)) {
                            Verbatim.gameContext.sendCommandFailure(wrapSource(context.getSource()), text("Players only."));
                            return 0;
                        }
                        return VerbatimCommandHandlers.executeMailClear(new PaperGamePlayer(player));
                    }))
                .executes(context -> VerbatimCommandHandlers.executeMailHelp(wrapSource(context.getSource())))
                .build(),
            "Offline mail system"
        );
    }

    private static PaperGameCommandSource wrapSource(CommandSourceStack source) {
        return new PaperGameCommandSource(source);
    }
}
