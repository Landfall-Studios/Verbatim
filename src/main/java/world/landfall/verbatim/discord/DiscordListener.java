package world.landfall.verbatim.discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.FormattingCodeUtils;
import static world.landfall.verbatim.context.GameText.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!DiscordBot.isEnabled()) return;

        User author = event.getAuthor();
        if (author.isBot() || author.isSystem()) {
            return;
        }

        String configuredChannelId = Verbatim.gameConfig.getDiscordChannelId();
        if (!event.getChannel().getId().equals(configuredChannelId)) {
            return;
        }

        String originalMessageContent = event.getMessage().getContentRaw()
            .replace("[", "\\[")
            .replace("]", "\\]");

        String processedContent = originalMessageContent;
        for (CustomEmoji emoji : event.getMessage().getMentions().getCustomEmojis()) {
            String emojiMention = emoji.getAsMention();
            String emojiName = ":" + emoji.getName() + ":";
            processedContent = processedContent.replace(emojiMention, emojiName);
        }

        if (processedContent.trim().isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            return;
        }

        String authorName;
        Member member = event.getMember();
        String nickname = member != null ? member.getNickname() : null;
        if (nickname != null && !nickname.isEmpty()) {
            authorName = nickname;
        } else {
            authorName = author.getName();
        }

        String prefixStr = DiscordBot.getDiscordMessagePrefix();
        if (prefixStr == null) prefixStr = "";

        String separatorStr = DiscordBot.getDiscordMessageSeparator();
        if (separatorStr == null) separatorStr = ": ";

        GameComponent finalMessage = empty();
        int currentLength = 0;
        final int MAX_LENGTH = 256;
        final String TRUNCATION_MARKER = "...";
        final int TRUNCATION_MARKER_LEN = TRUNCATION_MARKER.length();

        if (!prefixStr.isEmpty()) {
            GameComponent prefixComponent = Verbatim.chatFormatter.parseColors(prefixStr + " ");
            finalMessage = finalMessage.append(prefixComponent);
            currentLength += FormattingCodeUtils.stripFormattingCodes(prefixComponent.getString()).length();
        }

        finalMessage = finalMessage.append(text(authorName));
        currentLength += authorName.length();

        finalMessage = finalMessage.append(Verbatim.chatFormatter.parseColors(separatorStr));
        currentLength += FormattingCodeUtils.stripFormattingCodes(separatorStr).length();

        int remainingLength = MAX_LENGTH - currentLength - TRUNCATION_MARKER_LEN;

        String contentStr = processedContent.trim();
        if (!contentStr.isEmpty()) {
            if (contentStr.length() > remainingLength) {
                contentStr = contentStr.substring(0, remainingLength);
                finalMessage = finalMessage.append(text(contentStr));
                finalMessage = finalMessage.append(text(TRUNCATION_MARKER).withColor(GameColor.DARK_GRAY));
                remainingLength = 0;
            } else {
                finalMessage = finalMessage.append(text(contentStr));
                remainingLength -= contentStr.length();
                if (!event.getMessage().getAttachments().isEmpty() && remainingLength > 1) {
                    finalMessage = finalMessage.append(text(" "));
                    remainingLength--;
                }
            }
        }

        if (remainingLength > 0) {
            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            for (Message.Attachment attachment : attachments) {
                String fileName = attachment.getFileName();
                String proxyUrl = attachment.getProxyUrl();

                if (fileName.length() + 2 > remainingLength) {
                    finalMessage = finalMessage.append(text(TRUNCATION_MARKER).withColor(GameColor.DARK_GRAY));
                    break;
                }

                GameComponent attachmentComponent = text("[" + fileName + "]")
                    .withColor(GameColor.DARK_GRAY)
                    .withClickOpenUrl(proxyUrl)
                    .withHoverText("Click to open " + fileName);

                finalMessage = finalMessage.append(attachmentComponent);
                remainingLength -= (fileName.length() + 2);

                if (attachments.indexOf(attachment) < attachments.size() - 1 && remainingLength > 1) {
                    finalMessage = finalMessage.append(text(" "));
                    remainingLength--;
                }
            }
        }

        if (Verbatim.gameContext.isServerAvailable()) {
            Verbatim.gameContext.broadcastMessage(finalMessage, false);
            Verbatim.LOGGER.debug("[Discord -> Game] {} ({}) relayed to game chat.", authorName, author.getId());
        } else {
            Verbatim.LOGGER.warn("[Verbatim Discord] Server instance is null, cannot send message to game.");
        }
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (!DiscordBot.isEnabled()) {
            event.reply("The Discord bot integration is currently disabled.").setEphemeral(true).queue();
            return;
        }

        if (event.getName().equals("list")) {
            if (!Verbatim.gameContext.isServerAvailable()) {
                event.reply("Could not connect to the Minecraft server to fetch the player list.").setEphemeral(true).queue();
                return;
            }

            List<GamePlayer> onlinePlayers = Verbatim.gameContext.getAllOnlinePlayers();

            if (onlinePlayers.isEmpty()) {
                event.reply("There are no players currently online on the Minecraft server.").setEphemeral(true).queue();
                return;
            }

            String playerListString = onlinePlayers.stream()
                .map(player -> {
                    String username = player.getUsername();
                    String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(player.getDisplayName());
                    if (!username.equals(strippedDisplayName)) {
                        return strippedDisplayName + " (" + username + ")";
                    }
                    return username;
                })
                .collect(Collectors.joining("\n- ", "**Online Players (" + onlinePlayers.size() + "):**\n- ", ""));

            if (playerListString.length() > 1990) {
                playerListString = playerListString.substring(0, 1990) + "... (list truncated)";
            }

            event.reply(playerListString).setEphemeral(true).queue();
        }
    }
}
