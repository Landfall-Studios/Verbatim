package world.landfall.verbatim.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
 * Paper implementation of ChannelFormatter.
 * Handles formatting for "local" special channels with suffix-based behavior.
 */
public class PaperLocalChannelFormatter implements ChannelFormatter {
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

        Component adventureMsg = ((PaperGameComponentImpl) originalMessage).toAdventure();
        List<Component> children = adventureMsg.children();

        if (children.isEmpty()) {
            return originalMessage.copy();
        }

        int lastIndex = children.size() - 1;
        Component result = Component.empty();

        // Preserve prefix components
        for (int i = 0; i < lastIndex; i++) {
            result = result.append(children.get(i));
        }

        // Obscure only the message content (last child)
        String textToObscure = PlainTextComponentSerializer.plainText().serialize(children.get(lastIndex));
        if (textToObscure.isEmpty()) {
            result = result.append(children.get(lastIndex));
        } else {
            for (char c : textToObscure.toCharArray()) {
                if (RANDOM.nextDouble() < obscurePercentage) {
                    result = result.append(
                        Component.text(String.valueOf(OBSCURE_CHARS.charAt(RANDOM.nextInt(OBSCURE_CHARS.length()))))
                            .color(TextColor.color(0x555555)));
                } else {
                    result = result.append(
                        ((PaperGameComponentImpl) Verbatim.chatFormatter.parseColors(channelMessageColorString + c)).toAdventure());
                }
            }
        }
        return PaperGameComponentImpl.wrap(result);
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

            Component finalMessage = Component.empty()
                .append(Component.text("[OOC] ").color(TextColor.color(0x555555)))
                .append(Component.text(playerName + " (" + displayName + "): ").color(TextColor.color(0x555555)))
                .append(((PaperGameComponentImpl) Verbatim.chatFormatter.parseColors("&8" + suffix.trimmedMessage().trim())).toAdventure());

            return Optional.of(new FormattedMessageDetails(PaperGameComponentImpl.wrap(finalMessage), effectiveRange, false, "&8"));
        }

        String actualMessageContent = suffix.trimmedMessage().trim();

        Component finalMessage = Component.empty()
            .append(((PaperGameComponentImpl) Verbatim.chatFormatter.parseColors(channelConfig.displayPrefix)).toAdventure())
            .append(Component.text(" "))
            .append(((PaperGameComponentImpl) Verbatim.chatFormatter.createPlayerNameComponent(sender, channelConfig.nameColor, false, channelConfig.nameStyle)).toAdventure());

        boolean skipSpaceAfterName = applyPlusStyleFormatting && actualMessageContent.startsWith("'");

        if (!skipSpaceAfterName) {
            finalMessage = finalMessage.append(Component.text(" "));
        }

        if (!localActionText.isEmpty()) {
            finalMessage = finalMessage.append(Component.text(localActionText)).append(Component.text(" "));
        }

        if (applyPlusStyleFormatting) {
            finalMessage = finalMessage.append(formatPlusStyleMessage(actualMessageContent));
            return Optional.of(new FormattedMessageDetails(PaperGameComponentImpl.wrap(finalMessage), effectiveRange, true, null));
        } else {
            finalMessage = finalMessage.append(
                ((PaperGameComponentImpl) Verbatim.chatFormatter.parseColors(channelConfig.messageColor + actualMessageContent)).toAdventure());
            return Optional.of(new FormattedMessageDetails(PaperGameComponentImpl.wrap(finalMessage), effectiveRange, false, channelConfig.messageColor));
        }
    }

    private Component formatPlusStyleMessage(String messageContent) {
        boolean inQuote = false;
        StringBuilder currentSegment = new StringBuilder();
        Component result = Component.empty();

        PaperChatFormatter chatFormatter = (PaperChatFormatter) Verbatim.chatFormatter;

        for (int i = 0; i < messageContent.length(); i++) {
            char c = messageContent.charAt(i);
            if (c == '"') {
                if (currentSegment.length() > 0) {
                    if (inQuote) {
                        result = result.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), TextColor.color(0xAAAAAA)));
                    } else {
                        result = result.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), TextColor.color(0xFFFFFF), false, true));
                    }
                    currentSegment.setLength(0);
                }
                result = result.append(Component.text("\"").color(TextColor.color(0xAAAAAA)));
                inQuote = !inQuote;
            } else {
                currentSegment.append(c);
            }
        }

        if (currentSegment.length() > 0) {
            if (inQuote) {
                result = result.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), TextColor.color(0xAAAAAA)));
            } else {
                result = result.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), TextColor.color(0xFFFFFF), false, true));
            }
        }

        return result;
    }
}
