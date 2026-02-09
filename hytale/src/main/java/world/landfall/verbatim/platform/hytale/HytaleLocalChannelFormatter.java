package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.specialchannels.ChannelFormatter;
import world.landfall.verbatim.specialchannels.FormattedMessageDetails;
import world.landfall.verbatim.specialchannels.LocalMessageSuffix;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Hytale implementation of ChannelFormatter.
 * Handles formatting for "local" special channels with suffix-based behavior
 * (shouts, whispers, OOC, roleplay) using Hytale's Message API.
 */
public class HytaleLocalChannelFormatter implements ChannelFormatter {
    private static final Random RANDOM = new Random();
    private static final int MAX_FADE_DISTANCE = 30;
    private static final String OBSCURE_CHARS = ".";

    @Override
    public GameComponent createDistanceObscuredMessage(
            GameComponent originalMessage,
            double distanceSquared,
            int effectiveRange,
            boolean isRoleplayMessage,
            String channelMessageColorString) {

        if (isRoleplayMessage || effectiveRange < 0) {
            return originalMessage.copy();
        }

        double distance = Math.sqrt(distanceSquared);
        if (distance <= effectiveRange) {
            return originalMessage.copy();
        }

        double obscurePercentage = FormattedMessageDetails.obscurePercentage(
                distance, effectiveRange, MAX_FADE_DISTANCE, 2.0, 0.6);

        // Like the Minecraft version: use getChildren() to find the message structure,
        // keep all prefix components (channel tag, player name, verb) intact,
        // and only obscure the last child (the actual message content).
        Message hytaleMsg = ((HytaleGameComponentImpl) originalMessage).toHytale();
        List<Message> children = hytaleMsg.getChildren();

        if (children == null || children.isEmpty()) {
            return originalMessage.copy();
        }

        int lastIndex = children.size() - 1;
        Message result = Message.raw("");

        // Preserve prefix components
        for (int i = 0; i < lastIndex; i++) {
            result = Message.join(result, children.get(i));
        }

        // Obscure only the message content (last child)
        String textToObscure = extractText(children.get(lastIndex));
        if (textToObscure == null || textToObscure.isEmpty()) {
            result = Message.join(result, children.get(lastIndex));
        } else {
            for (char c : textToObscure.toCharArray()) {
                if (RANDOM.nextDouble() < obscurePercentage) {
                    result = Message.join(result,
                        Message.raw(String.valueOf(OBSCURE_CHARS.charAt(RANDOM.nextInt(OBSCURE_CHARS.length()))))
                            .color(new Color(0x555555)));
                } else {
                    result = Message.join(result,
                        ((HytaleGameComponentImpl) Verbatim.chatFormatter.parseColors(channelMessageColorString + c)).toHytale());
                }
            }
        }
        return HytaleGameComponentImpl.wrap(result);
    }

    /**
     * Recursively extracts plain text from a Message and its children.
     */
    private static String extractText(Message msg) {
        StringBuilder sb = new StringBuilder();
        String raw = msg.getRawText();
        if (raw != null) {
            sb.append(raw);
        }
        List<Message> children = msg.getChildren();
        if (children != null) {
            for (Message child : children) {
                String childText = extractText(child);
                if (childText != null) {
                    sb.append(childText);
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @Override
    public Optional<FormattedMessageDetails> formatLocalMessage(
            GamePlayer sender,
            ChatChannelManager.ChannelConfig channelConfig,
            String originalMessageContent) {

        boolean isLocalSpecialChannel = channelConfig.specialChannelType
            .map("local"::equals)
            .orElse(false);

        if (!isLocalSpecialChannel) {
            return Optional.empty();
        }

        LocalMessageSuffix suffix = LocalMessageSuffix.parse(originalMessageContent);
        int effectiveRange = suffix.effectiveRange();
        String localActionText = suffix.actionText();
        boolean applyPlusStyleFormatting = suffix.isRoleplay();

        if (suffix.isOOC()) {
            String playerName = sender.getUsername();
            String displayName = sender.getDisplayName();

            Message finalMessage = Message.join(
                Message.raw("[OOC] ").color(new Color(0x555555)),
                Message.raw(playerName + " (" + displayName + "): ").color(new Color(0x555555)),
                ((HytaleGameComponentImpl) Verbatim.chatFormatter.parseColors("&8" + suffix.trimmedMessage().trim())).toHytale()
            );

            return Optional.of(new FormattedMessageDetails(HytaleGameComponentImpl.wrap(finalMessage), effectiveRange, false, "&8"));
        }

        String actualMessageContent = suffix.trimmedMessage().trim();

        Message finalMessage = Message.join(
            ((HytaleGameComponentImpl) Verbatim.chatFormatter.parseColors(channelConfig.displayPrefix)).toHytale(),
            Message.raw(" "),
            ((HytaleGameComponentImpl) Verbatim.chatFormatter.createPlayerNameComponent(sender, channelConfig.nameColor, false, channelConfig.nameStyle)).toHytale()
        );

        boolean skipSpaceAfterName = applyPlusStyleFormatting && actualMessageContent.startsWith("'");

        if (!skipSpaceAfterName) {
            finalMessage = Message.join(finalMessage, Message.raw(" "));
        }

        if (!localActionText.isEmpty()) {
            finalMessage = Message.join(finalMessage, Message.raw(localActionText), Message.raw(" "));
        }

        if (applyPlusStyleFormatting) {
            finalMessage = Message.join(finalMessage, formatPlusStyleMessage(actualMessageContent));
            return Optional.of(new FormattedMessageDetails(HytaleGameComponentImpl.wrap(finalMessage), effectiveRange, true, null));
        } else {
            finalMessage = Message.join(finalMessage,
                ((HytaleGameComponentImpl) Verbatim.chatFormatter.parseColors(channelConfig.messageColor + actualMessageContent)).toHytale());
            return Optional.of(new FormattedMessageDetails(HytaleGameComponentImpl.wrap(finalMessage), effectiveRange, false, channelConfig.messageColor));
        }
    }

    private Message formatPlusStyleMessage(String messageContent) {
        boolean inQuote = false;
        StringBuilder currentSegment = new StringBuilder();
        Message result = Message.raw("");

        HytaleChatFormatter chatFormatter = (HytaleChatFormatter) Verbatim.chatFormatter;

        for (int i = 0; i < messageContent.length(); i++) {
            char c = messageContent.charAt(i);
            if (c == '"') {
                if (currentSegment.length() > 0) {
                    if (inQuote) {
                        result = Message.join(result, chatFormatter.makeLinksClickableInternal(currentSegment.toString(), new Color(0xAAAAAA)));
                    } else {
                        result = Message.join(result, chatFormatter.makeLinksClickableInternal(currentSegment.toString(), Color.WHITE, false, true));
                    }
                    currentSegment.setLength(0);
                }
                result = Message.join(result, Message.raw("\"").color(new Color(0xAAAAAA)));
                inQuote = !inQuote;
            } else {
                currentSegment.append(c);
            }
        }

        if (currentSegment.length() > 0) {
            if (inQuote) {
                result = Message.join(result, chatFormatter.makeLinksClickableInternal(currentSegment.toString(), new Color(0xAAAAAA)));
            } else {
                result = Message.join(result, chatFormatter.makeLinksClickableInternal(currentSegment.toString(), Color.WHITE, false, true));
            }
        }

        return result;
    }
}
