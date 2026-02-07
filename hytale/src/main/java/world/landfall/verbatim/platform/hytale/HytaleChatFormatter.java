package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import world.landfall.verbatim.NameStyle;
import world.landfall.verbatim.Verbatim;
import world.landfall.verbatim.context.ChatFormatter;
import world.landfall.verbatim.context.GameComponent;
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.FormattingCodeUtils;
import world.landfall.verbatim.util.NicknameService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hytale implementation of ChatFormatter.
 * Uses Hytale's Message API for text formatting, colors, and link handling.
 */
public class HytaleChatFormatter implements ChatFormatter {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Converts a formatting code character to a Hytale Color.
     */
    private static Color codeToColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> new Color(0x000000);       // BLACK
            case '1' -> new Color(0x0000AA);       // DARK_BLUE
            case '2' -> new Color(0x00AA00);       // DARK_GREEN
            case '3' -> new Color(0x00AAAA);       // DARK_AQUA
            case '4' -> new Color(0xAA0000);       // DARK_RED
            case '5' -> new Color(0xAA00AA);       // DARK_PURPLE
            case '6' -> new Color(0xFFAA00);       // GOLD
            case '7' -> new Color(0xAAAAAA);       // GRAY
            case '8' -> new Color(0x555555);       // DARK_GRAY
            case '9' -> new Color(0x5555FF);       // BLUE
            case 'a' -> new Color(0x55FF55);       // GREEN
            case 'b' -> new Color(0x55FFFF);       // AQUA
            case 'c' -> new Color(0xFF5555);       // RED
            case 'd' -> new Color(0xFF55FF);       // LIGHT_PURPLE
            case 'e' -> new Color(0xFFFF55);       // YELLOW
            case 'f' -> Color.WHITE;               // WHITE
            default -> null;
        };
    }

    /**
     * Applies color, bold, and italic styling to a Message segment.
     */
    private static Message applyStyle(Message msg, Color color, boolean bold, boolean italic) {
        if (color != null) msg = msg.color(color);
        if (bold) msg = msg.bold(true);
        if (italic) msg = msg.italic(true);
        return msg;
    }

    /**
     * Makes links in text clickable using Hytale's link tag system.
     * Returns a Message with clickable URLs and applied styling.
     */
    public Message makeLinksClickableInternal(String text, Color baseColor, boolean bold, boolean italic) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) {
            return applyStyle(Message.raw(text), baseColor, bold, italic);
        }

        matcher.reset();
        Message result = Message.raw("");
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String beforeUrl = text.substring(lastEnd, matcher.start());
                result = Message.join(result, applyStyle(Message.raw(beforeUrl), baseColor, bold, italic));
            }

            String url = matcher.group();
            String clickUrl = url.toLowerCase().startsWith("www.") ? "https://" + url : url;

            // Hytale supports clickable links via the Message API
            Message urlComponent = applyStyle(Message.raw(url), new Color(0x5555FF), bold, italic)
                .link(clickUrl);
            result = Message.join(result, urlComponent);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            result = Message.join(result, applyStyle(Message.raw(remaining), baseColor, bold, italic));
        }

        return result;
    }

    /**
     * Overload for callers that don't need bold/italic.
     */
    public Message makeLinksClickableInternal(String text, Color baseColor) {
        return makeLinksClickableInternal(text, baseColor, false, false);
    }

    @Override
    public GameComponent makeLinksClickable(String text, GameComponent baseStyleComponent) {
        Color baseColor = null;
        if (baseStyleComponent instanceof HytaleGameComponentImpl impl) {
            String colorStr = impl.toHytale().getColor();
            if (colorStr != null && !colorStr.isEmpty()) {
                try {
                    baseColor = Color.decode(colorStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return HytaleGameComponentImpl.wrap(makeLinksClickableInternal(text, baseColor));
    }

    private static final String COLOR_SPLIT_REGEX = "(?i)(?=&#[0-9a-f]{6})|(?=&[0-9a-fk-or])";

    @Override
    public GameComponent parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return HytaleGameComponentImpl.empty();
        }

        Message result = Message.raw("");
        String[] parts = text.split(COLOR_SPLIT_REGEX);
        Color currentColor = null;
        boolean currentBold = false;
        boolean currentItalic = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String textContent = part.substring(8);
                try {
                    currentColor = new Color(Integer.parseInt(hex, 16));
                } catch (NumberFormatException ignored) {}
                if (!textContent.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(textContent, currentColor, currentBold, currentItalic));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = Character.toLowerCase(part.charAt(1));
                String textContent = part.substring(2);

                Color color = codeToColor(code);
                if (color != null) {
                    currentColor = color;
                } else if (code == 'l') {
                    currentBold = true;
                } else if (code == 'o') {
                    currentItalic = true;
                } else if (code == 'r') {
                    currentColor = null;
                    currentBold = false;
                    currentItalic = false;
                }

                if (!textContent.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(textContent, currentColor, currentBold, currentItalic));
                }
            } else {
                result = Message.join(result, makeLinksClickableInternal(part, currentColor, currentBold, currentItalic));
            }
        }
        return HytaleGameComponentImpl.wrap(result);
    }

    @Override
    public GameComponent parseColorsWithPermissions(String text, GamePlayer player) {
        if (text == null || text.isEmpty()) {
            return HytaleGameComponentImpl.empty();
        }

        boolean hasColorPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        Message result = Message.raw("");
        String[] parts = text.split(COLOR_SPLIT_REGEX);
        Color currentColor = null;
        boolean currentBold = false;
        boolean currentItalic = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String content = part.substring(8);
                if (hasColorPerm) {
                    try {
                        currentColor = new Color(Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ignored) {}
                }
                if (!content.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(content, currentColor, currentBold, currentItalic));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = Character.toLowerCase(part.charAt(1));
                String content = part.substring(2);

                Color color = codeToColor(code);

                if (color != null && hasColorPerm) {
                    currentColor = color;
                } else if (code == 'l' && hasFormatPerm) {
                    currentBold = true;
                } else if (code == 'o' && hasFormatPerm) {
                    currentItalic = true;
                } else if (code == 'r' && (hasColorPerm || hasFormatPerm)) {
                    currentColor = null;
                    currentBold = false;
                    currentItalic = false;
                }

                if (!content.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(content, currentColor, currentBold, currentItalic));
                }
            } else {
                result = Message.join(result, makeLinksClickableInternal(part, currentColor, currentBold, currentItalic));
            }
        }
        return HytaleGameComponentImpl.wrap(result);
    }

    @Override
    public GameComponent parsePlayerInputWithPermissions(String channelBaseColor, String playerInput, GamePlayer player) {
        if (playerInput == null || playerInput.isEmpty()) {
            return HytaleGameComponentImpl.empty();
        }

        // Parse the base color from the channel color prefix
        Color baseColor = null;
        if (channelBaseColor != null && !channelBaseColor.isEmpty()) {
            // Support hex base color
            if (channelBaseColor.startsWith("&#") && channelBaseColor.length() >= 8) {
                try {
                    baseColor = new Color(Integer.parseInt(channelBaseColor.substring(2, 8), 16));
                } catch (NumberFormatException ignored) {}
            } else {
                String[] colorParts = channelBaseColor.split("(?i)(?=&[0-9a-f])");
                for (String cp : colorParts) {
                    if (cp.startsWith("&") && cp.length() >= 2) {
                        Color c = codeToColor(cp.charAt(1));
                        if (c != null) baseColor = c;
                    }
                }
            }
        }

        boolean hasColorPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        Message result = Message.raw("");
        String[] parts = playerInput.split(COLOR_SPLIT_REGEX);
        Color currentColor = baseColor;
        boolean currentBold = false;
        boolean currentItalic = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String content = part.substring(8);
                if (hasColorPerm) {
                    try {
                        currentColor = new Color(Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ignored) {}
                }
                if (!content.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(content, currentColor, currentBold, currentItalic));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = Character.toLowerCase(part.charAt(1));
                String content = part.substring(2);

                Color color = codeToColor(code);

                if (color != null && hasColorPerm) {
                    currentColor = color;
                } else if (code == 'l' && hasFormatPerm) {
                    currentBold = true;
                } else if (code == 'o' && hasFormatPerm) {
                    currentItalic = true;
                } else if (code == 'r' && (hasColorPerm || hasFormatPerm)) {
                    currentColor = baseColor;
                    currentBold = false;
                    currentItalic = false;
                }

                if (!content.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(content, currentColor, currentBold, currentItalic));
                }
            } else {
                result = Message.join(result, makeLinksClickableInternal(part, currentColor, currentBold, currentItalic));
            }
        }

        return HytaleGameComponentImpl.wrap(result);
    }

    @Override
    public GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle) {
        String username = player.getUsername();
        String displayName = player.getDisplayName();
        String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(displayName);

        String nameToShow;

        if (isDM) {
            nameToShow = username;
        } else if (nameStyle != null) {
            nameToShow = NicknameService.getNameForStyle(player, nameStyle);
        } else {
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
            } else {
                nameToShow = username;
            }
        }

        // Build the prefix from permission system if available
        String playerPrefix = "";
        if (Verbatim.prefixService != null && Verbatim.prefixService.isPrefixSystemAvailable()) {
            playerPrefix = Verbatim.prefixService.getPlayerPrefix(player);
        }

        Message fullName = Message.raw("");

        if (!playerPrefix.isEmpty()) {
            Message prefixMsg = ((HytaleGameComponentImpl) parseColors(playerPrefix)).toHytale();
            fullName = Message.join(fullName, prefixMsg);
            if (!playerPrefix.endsWith(" ")) {
                fullName = Message.join(fullName, Message.raw(" "));
            }
        }

        Message nameMsg = ((HytaleGameComponentImpl) parseColors(colorPrefix + nameToShow)).toHytale();
        fullName = Message.join(fullName, nameMsg);

        return HytaleGameComponentImpl.wrap(fullName);
    }

    @Override
    public GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM) {
        return createPlayerNameComponent(player, colorPrefix, isDM, null);
    }

    @Override
    public GameComponent createFavoriteNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle, int gradientStartRgb, int gradientEndRgb) {
        String username = player.getUsername();
        String displayName = player.getDisplayName();
        String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(displayName);

        String nameToShow;

        if (isDM) {
            nameToShow = username;
        } else if (nameStyle != null) {
            nameToShow = NicknameService.getNameForStyle(player, nameStyle);
        } else {
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
            } else {
                nameToShow = username;
            }
        }

        // Strip any embedded color codes — the gradient replaces them
        nameToShow = FormattingCodeUtils.stripFormattingCodes(nameToShow);

        String playerPrefix = "";
        if (Verbatim.prefixService != null && Verbatim.prefixService.isPrefixSystemAvailable()) {
            playerPrefix = Verbatim.prefixService.getPlayerPrefix(player);
        }

        Message fullName = Message.raw("");

        if (!playerPrefix.isEmpty()) {
            Message prefixMsg = ((HytaleGameComponentImpl) parseColors(playerPrefix)).toHytale();
            fullName = Message.join(fullName, prefixMsg);
            if (!playerPrefix.endsWith(" ")) {
                fullName = Message.join(fullName, Message.raw(" "));
            }
        }

        // Apply gradient to name characters
        int len = nameToShow.length();
        for (int i = 0; i < len; i++) {
            float ratio = len > 1 ? (float) i / (len - 1) : 0f;
            int r = Math.round(((gradientStartRgb >> 16) & 0xFF) + ratio * (((gradientEndRgb >> 16) & 0xFF) - ((gradientStartRgb >> 16) & 0xFF)));
            int g = Math.round(((gradientStartRgb >> 8) & 0xFF) + ratio * (((gradientEndRgb >> 8) & 0xFF) - ((gradientStartRgb >> 8) & 0xFF)));
            int b = Math.round((gradientStartRgb & 0xFF) + ratio * ((gradientEndRgb & 0xFF) - (gradientStartRgb & 0xFF)));
            fullName = Message.join(fullName, Message.raw(String.valueOf(nameToShow.charAt(i))).color(new Color(r, g, b)));
        }

        return HytaleGameComponentImpl.wrap(fullName);
    }

    @Override
    public String createDiscordPlayerName(GamePlayer player, NameStyle nameStyle) {
        String username = player.getUsername();

        if (nameStyle == null) {
            return username;
        }

        switch (nameStyle) {
            case USERNAME:
                return username;

            case DISPLAY_NAME:
                String displayName = player.getDisplayName();
                String cleanDisplayName = displayName.replaceAll("\u00a7[0-9a-fk-or]", "");
                if (!username.equals(cleanDisplayName)) {
                    return cleanDisplayName + " (" + username + ")";
                } else {
                    return username;
                }

            case NICKNAME:
                String nickname = NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    String cleanNickname = FormattingCodeUtils.stripFormattingCodes(nickname);
                    return cleanNickname + " (" + username + ")";
                } else {
                    return username;
                }

            default:
                return username;
        }
    }

    @Override
    public String createDiscordPlayerName(GamePlayer player) {
        return createDiscordPlayerName(player, NameStyle.USERNAME);
    }
}
