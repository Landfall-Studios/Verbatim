package world.landfall.verbatim.platform.neoforge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import world.landfall.verbatim.ChatChannelManager;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.GameComponent;
// NeoForgeGameComponentImpl is in this package
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.specialchannels.ChannelFormatter;
import world.landfall.verbatim.specialchannels.FormattedMessageDetails;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * NeoForge implementation of ChannelFormatter.
 * Handles formatting for "local" special channels with suffix-based behavior.
 */
public class NeoForgeLocalChannelFormatter implements ChannelFormatter {
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

        MutableComponent originalMc = ((NeoForgeGameComponentImpl) originalMessage).toMinecraftMutable();
        List<Component> originalSiblings = originalMc.getSiblings();
        if (originalSiblings.isEmpty()) {
            return originalMessage.copy();
        }

        MutableComponent reconstructedMessage = Component.empty();
        int firstMessageContentComponentIndex = originalSiblings.size() - 1;

        for (int i = 0; i < firstMessageContentComponentIndex; i++) {
            reconstructedMessage.append(originalSiblings.get(i).copy());
        }

        for (int i = firstMessageContentComponentIndex; i < originalSiblings.size(); i++) {
            Component messagePartComponent = originalSiblings.get(i);
            String textToObscure = messagePartComponent.getString();

            for (char c : textToObscure.toCharArray()) {
                if (RANDOM.nextDouble() < obscurePercentage) {
                    reconstructedMessage.append(Component.literal(String.valueOf(OBSCURE_CHARS.charAt(RANDOM.nextInt(OBSCURE_CHARS.length()))))
                        .withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    reconstructedMessage.append(((NeoForgeGameComponentImpl) Verbatim.chatFormatter.parseColors(channelMessageColorString + c)).toMinecraft());
                }
            }
        }
        return NeoForgeGameComponentImpl.wrap(reconstructedMessage);
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

            MutableComponent finalMessage = Component.empty();
            finalMessage.append(Component.literal("[OOC] ").withStyle(ChatFormatting.DARK_GRAY));

            String playerName = sender.getUsername();
            String displayName = sender.getDisplayName();

            finalMessage.append(Component.literal(playerName + " (" + displayName + "): ").withStyle(ChatFormatting.DARK_GRAY));
            finalMessage.append(((NeoForgeGameComponentImpl) Verbatim.chatFormatter.parseColors("&8" + messageAfterSuffixRemoval.trim())).toMinecraft());

            return Optional.of(new FormattedMessageDetails(NeoForgeGameComponentImpl.wrap(finalMessage), effectiveRange, false, "&8"));
        }

        actualMessageContent = messageAfterSuffixRemoval.trim();

        MutableComponent finalMessage = Component.empty();

        finalMessage.append(((NeoForgeGameComponentImpl) Verbatim.chatFormatter.parseColors(channelConfig.displayPrefix)).toMinecraft());
        finalMessage.append(Component.literal(" "));

        Component playerNameComponent = ((NeoForgeGameComponentImpl) Verbatim.chatFormatter.createPlayerNameComponent(sender, channelConfig.nameColor, false, channelConfig.nameStyle)).toMinecraft();
        finalMessage.append(playerNameComponent);

        boolean skipSpaceAfterName = applyPlusStyleFormatting && actualMessageContent.startsWith("'");

        if (!skipSpaceAfterName) {
            finalMessage.append(Component.literal(" "));
        }

        if (!localActionText.isEmpty()) {
            finalMessage.append(Component.literal(localActionText));
            finalMessage.append(Component.literal(" "));
        }

        if (applyPlusStyleFormatting) {
            formatPlusStyleMessage(finalMessage, actualMessageContent, channelConfig.messageColor);
            return Optional.of(new FormattedMessageDetails(NeoForgeGameComponentImpl.wrap(finalMessage), effectiveRange, true, null));
        } else {
            finalMessage.append(((NeoForgeGameComponentImpl) Verbatim.chatFormatter.parseColors(channelConfig.messageColor + actualMessageContent)).toMinecraft());
            return Optional.of(new FormattedMessageDetails(NeoForgeGameComponentImpl.wrap(finalMessage), effectiveRange, false, channelConfig.messageColor));
        }
    }

    private void formatPlusStyleMessage(MutableComponent finalMessage, String messageContent, String baseColorPrefix) {
        boolean inQuote = false;
        StringBuilder currentSegment = new StringBuilder();

        Style grayStyle = Style.EMPTY.withColor(ChatFormatting.GRAY);
        Style whiteItalicStyle = Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(true);

        NeoForgeChatFormatter chatFormatter = (NeoForgeChatFormatter) Verbatim.chatFormatter;

        for (int i = 0; i < messageContent.length(); i++) {
            char c = messageContent.charAt(i);
            if (c == '"') {
                if (currentSegment.length() > 0) {
                    if (inQuote) {
                        finalMessage.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), grayStyle));
                    } else {
                        finalMessage.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), whiteItalicStyle));
                    }
                    currentSegment.setLength(0);
                }
                finalMessage.append(Component.literal("\"").copy().withStyle(ChatFormatting.GRAY));
                inQuote = !inQuote;
            } else {
                currentSegment.append(c);
            }
        }

        if (currentSegment.length() > 0) {
            if (inQuote) {
                finalMessage.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), grayStyle));
            } else {
                finalMessage.append(chatFormatter.makeLinksClickableInternal(currentSegment.toString(), whiteItalicStyle));
            }
        }
    }
}
