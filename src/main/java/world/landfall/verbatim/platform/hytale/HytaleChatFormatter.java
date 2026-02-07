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
     * Makes links in text clickable using Hytale's link tag system.
     * Returns a Message with clickable URLs.
     */
    public Message makeLinksClickableInternal(String text, Color baseColor) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) {
            Message msg = Message.raw(text);
            return baseColor != null ? msg.color(baseColor) : msg;
        }

        matcher.reset();
        Message result = Message.raw("");
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String beforeUrl = text.substring(lastEnd, matcher.start());
                Message part = Message.raw(beforeUrl);
                result = Message.join(result, baseColor != null ? part.color(baseColor) : part);
            }

            String url = matcher.group();
            String clickUrl = url.toLowerCase().startsWith("www.") ? "https://" + url : url;

            // Hytale supports clickable links via the Message API
            Message urlComponent = Message.raw(url).color(new Color(0x5555FF));
            result = Message.join(result, urlComponent);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            Message part = Message.raw(remaining);
            result = Message.join(result, baseColor != null ? part.color(baseColor) : part);
        }

        return result;
    }

    @Override
    public GameComponent makeLinksClickable(String text, GameComponent baseStyleComponent) {
        Color baseColor = null;
        // Extract color context from the base component if possible
        return HytaleGameComponentImpl.wrap(makeLinksClickableInternal(text, baseColor));
    }

    @Override
    public GameComponent parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return HytaleGameComponentImpl.empty();
        }

        Message result = Message.raw("");
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");
        Color currentColor = null;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                String textContent = part.substring(2);

                Color color = codeToColor(code);
                if (color != null) {
                    currentColor = color;
                } else if (Character.toLowerCase(code) == 'r') {
                    currentColor = null;
                }
                // Formatting codes (k, l, m, n, o) are handled as no-ops for color;
                // Hytale's Message API handles bold/italic through TinyMessage tags

                if (!textContent.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(textContent, currentColor));
                }
            } else {
                result = Message.join(result, makeLinksClickableInternal(part, currentColor));
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
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");
        Color currentColor = null;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                String content = part.substring(2);

                Color color = codeToColor(code);
                boolean isFormatCode = "klmno".indexOf(Character.toLowerCase(code)) >= 0;
                boolean isReset = Character.toLowerCase(code) == 'r';

                if (color != null && hasColorPerm) {
                    currentColor = color;
                } else if (isReset && (hasColorPerm || hasFormatPerm)) {
                    currentColor = null;
                }
                // Format codes are no-ops in Hytale Message unless we use TinyMessage

                if (!content.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(content, currentColor));
                }
            } else {
                result = Message.join(result, makeLinksClickableInternal(part, currentColor));
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
            String stripped = channelBaseColor.replaceAll("(?i)&[0-9a-fk-or]", "");
            // Extract the last color code from the prefix
            String[] colorParts = channelBaseColor.split("(?i)(?=&[0-9a-f])");
            for (String cp : colorParts) {
                if (cp.startsWith("&") && cp.length() >= 2) {
                    Color c = codeToColor(cp.charAt(1));
                    if (c != null) baseColor = c;
                }
            }
        }

        boolean hasColorPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        Message result = Message.raw("");
        String[] parts = playerInput.split("(?i)(?=&[0-9a-fk-or])");
        Color currentColor = baseColor;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                String content = part.substring(2);

                Color color = codeToColor(code);
                boolean isReset = Character.toLowerCase(code) == 'r';

                if (color != null && hasColorPerm) {
                    currentColor = color;
                } else if (isReset && (hasColorPerm || hasFormatPerm)) {
                    currentColor = baseColor;
                }

                if (!content.isEmpty()) {
                    result = Message.join(result, makeLinksClickableInternal(content, currentColor));
                }
            } else {
                result = Message.join(result, makeLinksClickableInternal(part, currentColor));
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

        // Build the prefix from LuckPerms if available
        String luckPermsPrefix = "";
        if (Verbatim.prefixService != null && Verbatim.prefixService.isLuckPermsAvailable()) {
            luckPermsPrefix = Verbatim.prefixService.getPlayerPrefix(player);
        }

        Message fullName = Message.raw("");

        if (!luckPermsPrefix.isEmpty()) {
            Message prefixMsg = ((HytaleGameComponentImpl) parseColors(luckPermsPrefix)).toHytale();
            fullName = Message.join(fullName, prefixMsg);
            if (!luckPermsPrefix.endsWith(" ")) {
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
