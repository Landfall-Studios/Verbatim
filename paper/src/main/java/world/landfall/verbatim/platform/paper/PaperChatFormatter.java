package world.landfall.verbatim.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * Paper implementation of ChatFormatter using Adventure Component API.
 */
public class PaperChatFormatter implements ChatFormatter {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static TextColor codeToColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> TextColor.color(0x000000);       // BLACK
            case '1' -> TextColor.color(0x0000AA);       // DARK_BLUE
            case '2' -> TextColor.color(0x00AA00);       // DARK_GREEN
            case '3' -> TextColor.color(0x00AAAA);       // DARK_AQUA
            case '4' -> TextColor.color(0xAA0000);       // DARK_RED
            case '5' -> TextColor.color(0xAA00AA);       // DARK_PURPLE
            case '6' -> TextColor.color(0xFFAA00);       // GOLD
            case '7' -> TextColor.color(0xAAAAAA);       // GRAY
            case '8' -> TextColor.color(0x555555);       // DARK_GRAY
            case '9' -> TextColor.color(0x5555FF);       // BLUE
            case 'a' -> TextColor.color(0x55FF55);       // GREEN
            case 'b' -> TextColor.color(0x55FFFF);       // AQUA
            case 'c' -> TextColor.color(0xFF5555);       // RED
            case 'd' -> TextColor.color(0xFF55FF);       // LIGHT_PURPLE
            case 'e' -> TextColor.color(0xFFFF55);       // YELLOW
            case 'f' -> TextColor.color(0xFFFFFF);       // WHITE
            default -> null;
        };
    }

    private static Component applyStyle(Component comp, TextColor color, boolean bold, boolean italic,
                                         boolean underlined, boolean strikethrough, boolean obfuscated) {
        if (color != null) comp = comp.color(color);
        if (bold) comp = comp.decoration(TextDecoration.BOLD, true);
        if (italic) comp = comp.decoration(TextDecoration.ITALIC, true);
        if (underlined) comp = comp.decoration(TextDecoration.UNDERLINED, true);
        if (strikethrough) comp = comp.decoration(TextDecoration.STRIKETHROUGH, true);
        if (obfuscated) comp = comp.decoration(TextDecoration.OBFUSCATED, true);
        return comp;
    }

    /**
     * Makes links in text clickable using Adventure's click events.
     */
    Component makeLinksClickableInternal(String text, TextColor baseColor, boolean bold, boolean italic,
                                         boolean underlined, boolean strikethrough, boolean obfuscated) {
        if (text == null || text.isEmpty()) {
            return Component.text("");
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) {
            return applyStyle(Component.text(text), baseColor, bold, italic, underlined, strikethrough, obfuscated);
        }

        matcher.reset();
        Component result = Component.empty();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String beforeUrl = text.substring(lastEnd, matcher.start());
                result = result.append(applyStyle(Component.text(beforeUrl), baseColor, bold, italic, underlined, strikethrough, obfuscated));
            }

            String url = matcher.group();
            String clickUrl = url.toLowerCase().startsWith("www.") ? "https://" + url : url;

            Component urlComponent = Component.text(url)
                .color(TextColor.color(0x5555FF))
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(clickUrl))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("Click to open: " + clickUrl).color(TextColor.color(0xAAAAAA))));
            result = result.append(urlComponent);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            result = result.append(applyStyle(Component.text(remaining), baseColor, bold, italic, underlined, strikethrough, obfuscated));
        }

        return result;
    }

    Component makeLinksClickableInternal(String text, TextColor baseColor, boolean bold, boolean italic) {
        return makeLinksClickableInternal(text, baseColor, bold, italic, false, false, false);
    }

    Component makeLinksClickableInternal(String text, TextColor baseColor) {
        return makeLinksClickableInternal(text, baseColor, false, false, false, false, false);
    }

    @Override
    public GameComponent makeLinksClickable(String text, GameComponent baseStyleComponent) {
        TextColor baseColor = null;
        if (baseStyleComponent instanceof PaperGameComponentImpl impl) {
            baseColor = impl.toAdventure().color();
        }
        return PaperGameComponentImpl.wrap(makeLinksClickableInternal(text, baseColor));
    }

    private static final String COLOR_SPLIT_REGEX = "(?i)(?=&#[0-9a-f]{6})|(?=&[0-9a-fk-or])";

    @Override
    public GameComponent parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return PaperGameComponentImpl.empty();
        }

        Component result = Component.empty();
        String[] parts = text.split(COLOR_SPLIT_REGEX);
        TextColor currentColor = null;
        boolean currentBold = false;
        boolean currentItalic = false;
        boolean currentUnderlined = false;
        boolean currentStrikethrough = false;
        boolean currentObfuscated = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String textContent = part.substring(8);
                try {
                    currentColor = TextColor.color(Integer.parseInt(hex, 16));
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                } catch (NumberFormatException ignored) {}
                if (!textContent.isEmpty()) {
                    result = result.append(makeLinksClickableInternal(textContent, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = Character.toLowerCase(part.charAt(1));
                String textContent = part.substring(2);

                TextColor color = codeToColor(code);
                if (color != null) {
                    currentColor = color;
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                } else if (code == 'l') {
                    currentBold = true;
                } else if (code == 'o') {
                    currentItalic = true;
                } else if (code == 'n') {
                    currentUnderlined = true;
                } else if (code == 'm') {
                    currentStrikethrough = true;
                } else if (code == 'k') {
                    currentObfuscated = true;
                } else if (code == 'r') {
                    currentColor = null;
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                }

                if (!textContent.isEmpty()) {
                    result = result.append(makeLinksClickableInternal(textContent, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
                }
            } else {
                result = result.append(makeLinksClickableInternal(part, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
            }
        }
        return PaperGameComponentImpl.wrap(result);
    }

    @Override
    public GameComponent parseColorsWithPermissions(String text, GamePlayer player) {
        if (text == null || text.isEmpty()) {
            return PaperGameComponentImpl.empty();
        }

        boolean hasColorPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        Component result = Component.empty();
        String[] parts = text.split(COLOR_SPLIT_REGEX);
        TextColor currentColor = null;
        boolean currentBold = false;
        boolean currentItalic = false;
        boolean currentUnderlined = false;
        boolean currentStrikethrough = false;
        boolean currentObfuscated = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String content = part.substring(8);
                if (hasColorPerm) {
                    try {
                        currentColor = TextColor.color(Integer.parseInt(hex, 16));
                        currentBold = false;
                        currentItalic = false;
                        currentUnderlined = false;
                        currentStrikethrough = false;
                        currentObfuscated = false;
                    } catch (NumberFormatException ignored) {}
                }
                if (!content.isEmpty()) {
                    result = result.append(makeLinksClickableInternal(content, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = Character.toLowerCase(part.charAt(1));
                String content = part.substring(2);

                TextColor color = codeToColor(code);

                if (color != null && hasColorPerm) {
                    currentColor = color;
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                } else if (code == 'l' && hasFormatPerm) {
                    currentBold = true;
                } else if (code == 'o' && hasFormatPerm) {
                    currentItalic = true;
                } else if (code == 'n' && hasFormatPerm) {
                    currentUnderlined = true;
                } else if (code == 'm' && hasFormatPerm) {
                    currentStrikethrough = true;
                } else if (code == 'k' && hasFormatPerm) {
                    currentObfuscated = true;
                } else if (code == 'r' && (hasColorPerm || hasFormatPerm)) {
                    currentColor = null;
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                }

                if (!content.isEmpty()) {
                    result = result.append(makeLinksClickableInternal(content, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
                }
            } else {
                result = result.append(makeLinksClickableInternal(part, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
            }
        }
        return PaperGameComponentImpl.wrap(result);
    }

    @Override
    public GameComponent parsePlayerInputWithPermissions(String channelBaseColor, String playerInput, GamePlayer player) {
        if (playerInput == null || playerInput.isEmpty()) {
            return PaperGameComponentImpl.empty();
        }

        TextColor baseColor = null;
        if (channelBaseColor != null && !channelBaseColor.isEmpty()) {
            if (channelBaseColor.startsWith("&#") && channelBaseColor.length() >= 8) {
                try {
                    baseColor = TextColor.color(Integer.parseInt(channelBaseColor.substring(2, 8), 16));
                } catch (NumberFormatException ignored) {}
            } else {
                String[] colorParts = channelBaseColor.split("(?i)(?=&[0-9a-f])");
                for (String cp : colorParts) {
                    if (cp.startsWith("&") && cp.length() >= 2) {
                        TextColor c = codeToColor(cp.charAt(1));
                        if (c != null) baseColor = c;
                    }
                }
            }
        }

        boolean hasColorPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        Component result = Component.empty();
        String[] parts = playerInput.split(COLOR_SPLIT_REGEX);
        TextColor currentColor = baseColor;
        boolean currentBold = false;
        boolean currentItalic = false;
        boolean currentUnderlined = false;
        boolean currentStrikethrough = false;
        boolean currentObfuscated = false;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String content = part.substring(8);
                if (hasColorPerm) {
                    try {
                        currentColor = TextColor.color(Integer.parseInt(hex, 16));
                        currentBold = false;
                        currentItalic = false;
                        currentUnderlined = false;
                        currentStrikethrough = false;
                        currentObfuscated = false;
                    } catch (NumberFormatException ignored) {}
                }
                if (!content.isEmpty()) {
                    result = result.append(makeLinksClickableInternal(content, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = Character.toLowerCase(part.charAt(1));
                String content = part.substring(2);

                TextColor color = codeToColor(code);

                if (color != null && hasColorPerm) {
                    currentColor = color;
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                } else if (code == 'l' && hasFormatPerm) {
                    currentBold = true;
                } else if (code == 'o' && hasFormatPerm) {
                    currentItalic = true;
                } else if (code == 'n' && hasFormatPerm) {
                    currentUnderlined = true;
                } else if (code == 'm' && hasFormatPerm) {
                    currentStrikethrough = true;
                } else if (code == 'k' && hasFormatPerm) {
                    currentObfuscated = true;
                } else if (code == 'r' && (hasColorPerm || hasFormatPerm)) {
                    currentColor = baseColor;
                    currentBold = false;
                    currentItalic = false;
                    currentUnderlined = false;
                    currentStrikethrough = false;
                    currentObfuscated = false;
                }

                if (!content.isEmpty()) {
                    result = result.append(makeLinksClickableInternal(content, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
                }
            } else {
                result = result.append(makeLinksClickableInternal(part, currentColor, currentBold, currentItalic, currentUnderlined, currentStrikethrough, currentObfuscated));
            }
        }

        return PaperGameComponentImpl.wrap(result);
    }

    @Override
    public GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle) {
        String username = player.getUsername();
        String displayName = player.getDisplayName();
        String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(displayName);

        net.kyori.adventure.text.event.ClickEvent clickEvent =
            net.kyori.adventure.text.event.ClickEvent.suggestCommand("/msg " + username + " ");
        net.kyori.adventure.text.event.HoverEvent<?> hoverEvent = null;

        String nameToShow;

        if (isDM) {
            nameToShow = username;
        } else if (nameStyle != null) {
            nameToShow = NicknameService.getNameForStyle(player, nameStyle);

            if (nameStyle == NameStyle.NICKNAME) {
                String nickname = NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    hoverEvent = net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("Username: " + username).color(TextColor.color(0xAAAAAA)));
                }
            } else if (nameStyle == NameStyle.DISPLAY_NAME && !username.equals(strippedDisplayName)) {
                hoverEvent = net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("Username: " + username).color(TextColor.color(0xAAAAAA)));
            }
        } else {
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
                hoverEvent = net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("Username: " + username).color(TextColor.color(0xAAAAAA)));
            } else {
                nameToShow = username;
            }
        }

        String playerPrefix = "";
        String prefixTooltipText = null;
        if (Verbatim.prefixService != null && Verbatim.prefixService.isPrefixSystemAvailable()) {
            playerPrefix = Verbatim.prefixService.getPlayerPrefix(player);
            if (!playerPrefix.isEmpty()) {
                prefixTooltipText = Verbatim.prefixService.getPrefixTooltip(player);
            }
        }

        Component fullName = Component.empty();

        if (!playerPrefix.isEmpty()) {
            Component prefixComp = ((PaperGameComponentImpl) parseColors(playerPrefix)).toAdventure();
            if (prefixTooltipText != null && !prefixTooltipText.isEmpty()) {
                Component tooltipComp = ((PaperGameComponentImpl) parseColors(prefixTooltipText)).toAdventure();
                prefixComp = prefixComp.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(tooltipComp));
            }
            fullName = fullName.append(prefixComp);
            if (!playerPrefix.endsWith(" ")) {
                fullName = fullName.append(Component.text(" "));
            }
        }

        Component nameComp = ((PaperGameComponentImpl) parseColors(colorPrefix + nameToShow)).toAdventure();
        nameComp = nameComp.clickEvent(clickEvent);
        if (hoverEvent != null) {
            nameComp = nameComp.hoverEvent(hoverEvent);
        }

        fullName = fullName.append(nameComp);

        return PaperGameComponentImpl.wrap(fullName);
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

        net.kyori.adventure.text.event.ClickEvent clickEvent =
            net.kyori.adventure.text.event.ClickEvent.suggestCommand("/msg " + username + " ");
        net.kyori.adventure.text.event.HoverEvent<?> hoverEvent = null;

        String nameToShow;

        if (isDM) {
            nameToShow = username;
        } else if (nameStyle != null) {
            nameToShow = NicknameService.getNameForStyle(player, nameStyle);

            if (nameStyle == NameStyle.NICKNAME) {
                String nickname = NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    hoverEvent = net.kyori.adventure.text.event.HoverEvent.showText(
                        Component.text("Username: " + username).color(TextColor.color(0xAAAAAA)));
                }
            } else if (nameStyle == NameStyle.DISPLAY_NAME && !username.equals(strippedDisplayName)) {
                hoverEvent = net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("Username: " + username).color(TextColor.color(0xAAAAAA)));
            }
        } else {
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
                hoverEvent = net.kyori.adventure.text.event.HoverEvent.showText(
                    Component.text("Username: " + username).color(TextColor.color(0xAAAAAA)));
            } else {
                nameToShow = username;
            }
        }

        nameToShow = FormattingCodeUtils.stripFormattingCodes(nameToShow);

        String playerPrefix = "";
        String prefixTooltipText = null;
        if (Verbatim.prefixService != null && Verbatim.prefixService.isPrefixSystemAvailable()) {
            playerPrefix = Verbatim.prefixService.getPlayerPrefix(player);
            if (!playerPrefix.isEmpty()) {
                prefixTooltipText = Verbatim.prefixService.getPrefixTooltip(player);
            }
        }

        Component fullName = Component.empty();

        if (!playerPrefix.isEmpty()) {
            Component prefixComp = ((PaperGameComponentImpl) parseColors(playerPrefix)).toAdventure();
            if (prefixTooltipText != null && !prefixTooltipText.isEmpty()) {
                Component tooltipComp = ((PaperGameComponentImpl) parseColors(prefixTooltipText)).toAdventure();
                prefixComp = prefixComp.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(tooltipComp));
            }
            fullName = fullName.append(prefixComp);
            if (!playerPrefix.endsWith(" ")) {
                fullName = fullName.append(Component.text(" "));
            }
        }

        // Apply gradient to name characters with click/hover events
        int len = nameToShow.length();
        for (int i = 0; i < len; i++) {
            float ratio = len > 1 ? (float) i / (len - 1) : 0f;
            int r = Math.round(((gradientStartRgb >> 16) & 0xFF) + ratio * (((gradientEndRgb >> 16) & 0xFF) - ((gradientStartRgb >> 16) & 0xFF)));
            int g = Math.round(((gradientStartRgb >> 8) & 0xFF) + ratio * (((gradientEndRgb >> 8) & 0xFF) - ((gradientStartRgb >> 8) & 0xFF)));
            int b = Math.round((gradientStartRgb & 0xFF) + ratio * ((gradientEndRgb & 0xFF) - (gradientStartRgb & 0xFF)));
            Component charComp = Component.text(String.valueOf(nameToShow.charAt(i)))
                .color(TextColor.color(r, g, b))
                .clickEvent(clickEvent);
            if (hoverEvent != null) {
                charComp = charComp.hoverEvent(hoverEvent);
            }
            fullName = fullName.append(charComp);
        }

        return PaperGameComponentImpl.wrap(fullName);
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
