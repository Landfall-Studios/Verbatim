package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.command.VerbatimCommandHandlers;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.util.NicknameService;

import javax.annotation.Nonnull;
import java.awt.Color;

import static world.landfall.verbatim.context.GameText.*;

/**
 * Hytale-specific command registration.
 * Each command extends AbstractPlayerCommand and delegates to VerbatimCommandHandlers.
 *
 * Commands are registered in HytaleEntryPoint's setup() method via
 * this.getCommandRegistry().registerCommand(...).
 */
public class HytaleCommandRegistrar {

    // === Channel Commands ===

    public static class ChannelsCommand extends AbstractPlayerCommand {
        public ChannelsCommand() {
            super("channels", "Lists all available chat channels");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.listChannels(wrapSource(ctx, playerRef));
        }
    }

    public static class ChannelHelpCommand extends AbstractPlayerCommand {
        public ChannelHelpCommand() {
            super("channelhelp", "Shows Verbatim channel help");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.showHelp(wrapSource(ctx, playerRef));
        }
    }

    public static class ChannelFocusCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> channelNameArg;

        public ChannelFocusCommand() {
            super("channelfocus", "Focus on a specific chat channel");
            channelNameArg = withRequiredArg("channelName", "The channel name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String channelName = ctx.get(channelNameArg);
            ChatChannelManager.focusChannel(new HytaleGamePlayer(playerRef), channelName);
        }
    }

    public static class ChannelJoinCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> channelNameArg;

        public ChannelJoinCommand() {
            super("channeljoin", "Join a chat channel");
            channelNameArg = withRequiredArg("channelName", "The channel name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String channelName = ctx.get(channelNameArg);
            ChatChannelManager.joinChannel(new HytaleGamePlayer(playerRef), channelName);
        }
    }

    public static class ChannelLeaveCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> channelNameArg;

        public ChannelLeaveCommand() {
            super("channelleave", "Leave a chat channel");
            channelNameArg = withOptionalArg("channelName", "The channel name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer gamePlayer = new HytaleGamePlayer(playerRef);
            if (!channelNameArg.provided(ctx)) {
                ChatChannelManager.getFocusedChannelConfig(gamePlayer).ifPresentOrElse(
                    focused -> ChatChannelManager.leaveChannelCmd(gamePlayer, focused.name),
                    () -> Verbatim.gameContext.sendMessage(gamePlayer, text("You are not focused on any channel to leave.").withColor(GameColor.YELLOW))
                );
                return;
            }
            ChatChannelManager.leaveChannelCmd(gamePlayer, ctx.get(channelNameArg));
        }
    }

    // === Direct Message Commands ===

    public static class MsgCommand extends AbstractPlayerCommand {
        private final RequiredArg<PlayerRef> targetArg;
        private final OptionalArg<String> messageArg;

        public MsgCommand() {
            super("msg", "Send a direct message to a player");
            targetArg = withRequiredArg("player", "The player to message", ArgTypes.PLAYER_REF);
            messageArg = withOptionalArg("message", "The message to send", ArgTypes.STRING);
            setAllowsExtraArguments(true);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer sender = new HytaleGamePlayer(playerRef);
            PlayerRef targetRef = ctx.get(targetArg);

            var targetPlayer = new HytaleGamePlayer(targetRef);

            if (!messageArg.provided(ctx)) {
                // Just focus DM, no message
                ChatChannelManager.focusDm(sender, targetPlayer.getUUID());
            } else {
                String message = ctx.get(messageArg);
                VerbatimCommandHandlers.sendDirectMessage(sender, targetPlayer, message);
            }
        }
    }

    public static class ReplyCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> messageArg;

        public ReplyCommand() {
            super("r", "Reply to the last player who messaged you");
            messageArg = withOptionalArg("message", "The message to send", ArgTypes.STRING);
            setAllowsExtraArguments(true);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer sender = new HytaleGamePlayer(playerRef);

            if (!messageArg.provided(ctx)) {
                ChatChannelManager.handleDPrefix(sender);
            } else {
                String message = ctx.get(messageArg);
                VerbatimCommandHandlers.replyToLastDm(sender, message);
            }
        }
    }

    // === List Commands ===

    public static class ListCommand extends AbstractPlayerCommand {
        public ListCommand() {
            super("list", "Lists online players");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.executeCustomListCommand(wrapSource(ctx, playerRef));
        }
    }

    public static class VListCommand extends AbstractPlayerCommand {
        public VListCommand() {
            super("vlist", "Lists online players (Verbatim format)");
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.listOnlinePlayers(wrapSource(ctx, playerRef));
        }
    }

    // === Admin Commands ===

    public static class ChListCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> targetArg;

        public ChListCommand() {
            super("chlist", "Admin: list channels for a player or players in a channel");
            targetArg = withRequiredArg("target", "A player name or channel name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer gamePlayer = new HytaleGamePlayer(playerRef);

            if (!ctx.sender().hasPermission(VerbatimCommandHandlers.PERM_ADMIN_CHLIST)) {
                ctx.sendMessage(Message.raw("You do not have permission to use this command.").color(Color.RED));
                return;
            }

            String target = ctx.get(targetArg);
            VerbatimCommandHandlers.executeChList(wrapSource(ctx, playerRef), target);
        }
    }

    public static class ChKickCommand extends AbstractPlayerCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> channelArg;

        public ChKickCommand() {
            super("chkick", "Admin: kick a player from a channel");
            playerArg = withRequiredArg("player", "The player to kick", ArgTypes.PLAYER_REF);
            channelArg = withRequiredArg("channel", "The channel to kick from", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer executor = new HytaleGamePlayer(playerRef);

            if (!ctx.sender().hasPermission(VerbatimCommandHandlers.PERM_ADMIN_CHKICK)) {
                ctx.sendMessage(Message.raw("You do not have permission to use this command.").color(Color.RED));
                return;
            }

            PlayerRef targetRef = ctx.get(playerArg);
            String channelName = ctx.get(channelArg);

            var targetPlayer = new HytaleGamePlayer(targetRef);

            VerbatimCommandHandlers.executeChKick(wrapSource(ctx, playerRef), targetPlayer, channelName, executor);
        }
    }

    // === Nickname Commands ===

    public static class NickCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> nicknameArg;

        public NickCommand() {
            super("nick", "Set, show, or clear your nickname");
            nicknameArg = withOptionalArg("nickname", "The nickname to set, or 'clear' to remove", ArgTypes.STRING);
            setAllowsExtraArguments(true);
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer gamePlayer = new HytaleGamePlayer(playerRef);

            if (!ctx.sender().hasPermission(NicknameService.PERM_NICK)) {
                ctx.sendMessage(Message.raw("You do not have permission to use this command.").color(Color.RED));
                return;
            }

            if (!nicknameArg.provided(ctx)) {
                VerbatimCommandHandlers.executeNickShow(gamePlayer);
            } else {
                String nickname = ctx.get(nicknameArg);
                if ("clear".equalsIgnoreCase(nickname)) {
                    VerbatimCommandHandlers.executeNickClear(gamePlayer);
                } else {
                    VerbatimCommandHandlers.executeNickSet(gamePlayer, nickname);
                }
            }
        }
    }

    // === Helper ===

    private static HytaleGameCommandSource wrapSource(CommandContext ctx, PlayerRef playerRef) {
        return new HytaleGameCommandSource(ctx, playerRef);
    }
}
