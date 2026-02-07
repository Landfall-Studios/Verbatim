package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.specialchannels.ChannelFormatter;
import world.landfall.verbatim.specialchannels.FormattedMessageDetails;

import java.util.Optional;
import java.util.Random;

/**
 * Hytale implementation of ChannelFormatter.
 * Handles formatting for "local" special channels with suffix-based behavior
 * (shouts, whispers, OOC, roleplay) using Hytale's Message API.
 */
public class HytaleLocalChannelFormatter implements ChannelFormatter {
    private static final Random RANDOM = new Random();
    private static final int MAX_FADE_DISTANCE = 15;
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

        double fadeDistance;
        if (effectiveRange <= 15) {
            fadeDistance = effectiveRange * 1.5;
        } else {
            fadeDistance = Math.min(MAX_FADE_DISTANCE, effectiveRange * 0.3);
        }

        double obscurePercentage = (distance - effectiveRange) / fadeDistance;
        obscurePercentage = Math.min(1.0, Math.max(0.0, obscurePercentage));

        // For Hytale, we work at the string level since Message is immutable
        String originalText = originalMessage.getString();
        Message result = Message.raw("");

        for (char c : originalText.toCharArray()) {
            if (RANDOM.nextDouble() < obscurePercentage) {
                result = Message.join(result,
                    Message.raw(String.valueOf(OBSCURE_CHARS.charAt(RANDOM.nextInt(OBSCURE_CHARS.length()))))
                        .color(new Color(0x555555)));
            } else {
                result = Message.join(result,
                    ((HytaleGameComponentImpl) Verbatim.chatFormatter.parseColors(channelMessageColorString + c)).toHytale());
            }
        }
        return HytaleGameComponentImpl.wrap(result);
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

        int effectiveRange = 50;
        String localActionText = "says:";
        String actualMessageContent = originalMessageContent;
        boolean applyPlusStyleFormatting = false;

        String messageAfterSuffixRemoval = originalMessageContent;

        if (originalMessageContent.endsWith("!!")) {
            effectiveRange = 100;
            localActionText = "shouts:";
            messageAfterSuffixRemoval = originalMessageContent;
        } else if (originalMessageContent.endsWith("!")) {
            effectiveRange = 75;
            localActionText = "exclaims:";
            messageAfterSuffixRemoval = originalMessageContent;
        } else if (originalMessageContent.endsWith("*")) {
            effectiveRange = 10;
            localActionText = "whispers:";
            messageAfterSuffixRemoval = originalMessageContent.substring(0, originalMessageContent.length() - 1);
        } else if (originalMessageContent.endsWith("?")) {
            effectiveRange = 50;
            localActionText = "asks:";
            messageAfterSuffixRemoval = originalMessageContent;
        } else if (originalMessageContent.endsWith("$")) {
            effectiveRange = 3;
            localActionText = "mutters:";
            messageAfterSuffixRemoval = originalMessageContent.substring(0, originalMessageContent.length() - 1);
        } else if (originalMessageContent.endsWith("+")) {
            effectiveRange = 50;
            localActionText = "";
            applyPlusStyleFormatting = true;
            messageAfterSuffixRemoval = originalMessageContent.substring(0, originalMessageContent.length() - 1);
        } else if (originalMessageContent.endsWith("))")) {
            effectiveRange = 50;
            localActionText = "";
            applyPlusStyleFormatting = false;
            messageAfterSuffixRemoval = originalMessageContent.substring(0, originalMessageContent.length() - 2);

            String playerName = sender.getUsername();
            String displayName = sender.getDisplayName();

            Message finalMessage = Message.join(
                Message.raw("[OOC] ").color(new Color(0x555555)),
                Message.raw(playerName + " (" + displayName + "): ").color(new Color(0x555555)),
                ((HytaleGameComponentImpl) Verbatim.chatFormatter.parseColors("&8" + messageAfterSuffixRemoval.trim())).toHytale()
            );

            return Optional.of(new FormattedMessageDetails(HytaleGameComponentImpl.wrap(finalMessage), effectiveRange, false, "&8"));
        }

        actualMessageContent = messageAfterSuffixRemoval.trim();

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
                        result = Message.join(result, chatFormatter.makeLinksClickableInternal(currentSegment.toString(), Color.WHITE));
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
                result = Message.join(result, chatFormatter.makeLinksClickableInternal(currentSegment.toString(), Color.WHITE));
            }
        }

        return result;
    }
}
