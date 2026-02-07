package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
 * Commands are registered in HytaleEntryPoint's setup() method via
 * this.getCommandRegistry().registerCommand(...).
 */
public class HytaleCommandRegistrar {

    // === /channel command collection ===

    public static class ChannelCommand extends AbstractCommandCollection {
        public ChannelCommand() {
            super("channel", "Channel management commands");
            addSubCommand(new ChannelListSubCommand());
            addSubCommand(new ChannelHelpSubCommand());
            addSubCommand(new ChannelJoinSubCommand());
            addSubCommand(new ChannelLeaveSubCommand());
            addSubCommand(new ChannelFocusSubCommand());
        }

        @Override
        protected boolean canGeneratePermission() {
            return false; // Available to all players
        }
    }

    public static class ChannelListSubCommand extends AbstractPlayerCommand {
        public ChannelListSubCommand() {
            super("list", "Lists all available chat channels");
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.listChannels(wrapSource(ctx, playerRef));
        }
    }

    public static class ChannelHelpSubCommand extends AbstractPlayerCommand {
        public ChannelHelpSubCommand() {
            super("help", "Shows Verbatim channel help");
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.showHelp(wrapSource(ctx, playerRef));
        }
    }

    public static class ChannelJoinSubCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> channelNameArg;

        public ChannelJoinSubCommand() {
            super("join", "Join a chat channel");
            channelNameArg = withRequiredArg("channelName", "The channel name", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String channelName = ctx.get(channelNameArg);
            ChatChannelManager.joinChannel(new HytaleGamePlayer(playerRef), channelName);
        }
    }

    public static class ChannelLeaveSubCommand extends AbstractPlayerCommand {
        private final OptionalArg<String> channelNameArg;

        public ChannelLeaveSubCommand() {
            super("leave", "Leave a chat channel");
            channelNameArg = withOptionalArg("channelName", "The channel name", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
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

    public static class ChannelFocusSubCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> channelNameArg;

        public ChannelFocusSubCommand() {
            super("focus", "Focus on a specific chat channel");
            channelNameArg = withRequiredArg("channelName", "The channel name", ArgTypes.STRING);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String channelName = ctx.get(channelNameArg);
            ChatChannelManager.focusChannel(new HytaleGamePlayer(playerRef), channelName);
        }
    }

    // === Standalone /channels command (alias for /channel list) ===

    public static class ChannelsCommand extends AbstractPlayerCommand {
        public ChannelsCommand() {
            super("channels", "Lists all available chat channels");
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            VerbatimCommandHandlers.listChannels(wrapSource(ctx, playerRef));
        }
    }

    // === Direct Message Commands ===

    public static class MsgCommand extends AbstractPlayerCommand {
        private final RequiredArg<PlayerRef> targetArg;

        public MsgCommand() {
            super("msg", "Send a direct message to a player");
            addAliases("tell");
            targetArg = withRequiredArg("player", "The player to message", ArgTypes.PLAYER_REF);
            setAllowsExtraArguments(true);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer sender = new HytaleGamePlayer(playerRef);
            PlayerRef targetRef = ctx.get(targetArg);

            var targetPlayer = new HytaleGamePlayer(targetRef);

            // Extract message from raw input since ArgTypes.STRING only captures one word
            String message = extractMessageAfterToken(ctx.getInputString(), targetPlayer.getUsername());

            if (message == null) {
                // Just focus DM, no message
                ChatChannelManager.focusDm(sender, targetPlayer.getUUID());
            } else {
                VerbatimCommandHandlers.sendDirectMessage(sender, targetPlayer, message);
            }
        }
    }

    public static class ReplyCommand extends AbstractPlayerCommand {

        public ReplyCommand() {
            super("r", "Reply to the last player who messaged you");
            setAllowsExtraArguments(true);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer sender = new HytaleGamePlayer(playerRef);

            // Extract message from raw input since ArgTypes.STRING only captures one word
            String message = extractMessageAfterCommand(ctx.getInputString());

            if (message == null) {
                ChatChannelManager.handleDPrefix(sender);
            } else {
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
        protected boolean canGeneratePermission() {
            return false;
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
        protected boolean canGeneratePermission() {
            return false;
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
        protected boolean canGeneratePermission() {
            return false;
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
        protected boolean canGeneratePermission() {
            return false;
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

        public NickCommand() {
            super("nick", "Set, show, or clear your nickname");
            setAllowsExtraArguments(true);
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            HytaleGamePlayer gamePlayer = new HytaleGamePlayer(playerRef);

            if (!ctx.sender().hasPermission(NicknameService.PERM_NICK)) {
                ctx.sendMessage(Message.raw("You do not have permission to use this command.").color(Color.RED));
                return;
            }

            // Extract nickname from raw input since ArgTypes.STRING only captures one word
            String nickname = extractMessageAfterCommand(ctx.getInputString());

            if (nickname == null) {
                VerbatimCommandHandlers.executeNickClear(gamePlayer);
            } else {
                VerbatimCommandHandlers.executeNickSet(gamePlayer, nickname);
            }
        }
    }

    // === Helpers ===

    private static HytaleGameCommandSource wrapSource(CommandContext ctx, PlayerRef playerRef) {
        return new HytaleGameCommandSource(ctx, playerRef);
    }

    /**
     * Extracts the remaining text after the command name from the raw input string.
     * Used for commands like "/r hello world" where we need the full "hello world".
     * Hytale's ArgTypes.STRING only captures a single word, so we parse from raw input.
     *
     * @param inputString The raw input from CommandContext.getInputString()
     * @return The message text, or null if no text follows the command name
     */
    static String extractMessageAfterCommand(String inputString) {
        if (inputString == null || inputString.isEmpty()) return null;
        String trimmed = inputString.trim();
        // Skip the command name (first token)
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx < 0) return null;
        String remainder = trimmed.substring(spaceIdx + 1).trim();
        return remainder.isEmpty() ? null : remainder;
    }

    /**
     * Extracts the remaining text after a specific token (e.g., a player name) in the raw input.
     * Used for commands like "/msg PlayerName hello world" where we need "hello world".
     *
     * @param inputString The raw input from CommandContext.getInputString()
     * @param token The token to find and skip past (case-insensitive)
     * @return The message text after the token, or null if not found or no text follows
     */
    static String extractMessageAfterToken(String inputString, String token) {
        if (inputString == null || inputString.isEmpty() || token == null) return null;
        // Skip past the command name (first token) to avoid matching player names
        // that coincide with the command name (e.g., player named "msg" in "/msg msg hello")
        int firstSpace = inputString.indexOf(' ');
        if (firstSpace < 0) return null;
        String afterCommand = inputString.substring(firstSpace + 1);
        int idx = afterCommand.toLowerCase().indexOf(token.toLowerCase());
        if (idx < 0) return null;
        int afterToken = idx + token.length();
        if (afterToken >= afterCommand.length()) return null;
        String remainder = afterCommand.substring(afterToken).trim();
        return remainder.isEmpty() ? null : remainder;
    }
}
