package world.landfall.verbatim.platform.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
 * Forge 1.20.1 implementation of ChatFormatter.
 * Contains all Minecraft-specific formatting logic.
 */
public class ForgeChatFormatter implements ChatFormatter {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+|www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    );

    public MutableComponent makeLinksClickableInternal(String text, Style baseStyle) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        Matcher matcher = URL_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String beforeUrl = text.substring(lastEnd, matcher.start());
                result.append(Component.literal(beforeUrl).setStyle(baseStyle));
            }

            String url = matcher.group();
            String clickUrl = url;

            if (url.toLowerCase().startsWith("www.")) {
                clickUrl = "https://" + url;
            }

            MutableComponent urlComponent = Component.literal(url);
            urlComponent.setStyle(baseStyle
                .withColor(ChatFormatting.BLUE)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickUrl))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to open: " + clickUrl).withStyle(ChatFormatting.GRAY)))
            );
            result.append(urlComponent);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            result.append(Component.literal(remaining).setStyle(baseStyle));
        }

        if (lastEnd == 0) {
            return Component.literal(text).setStyle(baseStyle);
        }

        return result;
    }

    @Override
    public GameComponent makeLinksClickable(String text, GameComponent baseStyleComponent) {
        Style baseStyle = Style.EMPTY;
        if (baseStyleComponent instanceof ForgeGameComponentImpl impl) {
            baseStyle = impl.toMinecraftMutable().getStyle();
        }
        return ForgeGameComponentImpl.wrap(makeLinksClickableInternal(text, baseStyle));
    }

    private static final String COLOR_SPLIT_REGEX = "(?i)(?=&#[0-9a-f]{6})|(?=&[0-9a-fk-or])";

    @Override
    public GameComponent parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return ForgeGameComponentImpl.empty();
        }

        MutableComponent mainComponent = Component.literal("");
        String[] parts = text.split(COLOR_SPLIT_REGEX);
        Style currentStyle = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String textContent = part.substring(8);
                try {
                    currentStyle = Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(Integer.parseInt(hex, 16)));
                } catch (NumberFormatException ignored) {}
                if (!textContent.isEmpty()) {
                    mainComponent.append(makeLinksClickableInternal(textContent, currentStyle));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                String textContent = part.substring(2);

                if (formatting != null) {
                    if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else {
                        currentStyle = applyStyle(currentStyle, formatting);
                    }
                }

                if (!textContent.isEmpty()) {
                    mainComponent.append(makeLinksClickableInternal(textContent, currentStyle));
                }
            } else {
                mainComponent.append(makeLinksClickableInternal(part, currentStyle));
            }
        }
        return ForgeGameComponentImpl.wrap(mainComponent);
    }

    private static Style applyStyle(Style baseStyle, ChatFormatting format) {
        if (format == ChatFormatting.BOLD) return baseStyle.withBold(true);
        if (format == ChatFormatting.ITALIC) return baseStyle.withItalic(true);
        if (format == ChatFormatting.UNDERLINE) return baseStyle.withUnderlined(true);
        if (format == ChatFormatting.STRIKETHROUGH) return baseStyle.withStrikethrough(true);
        if (format == ChatFormatting.OBFUSCATED) return baseStyle.withObfuscated(true);
        return baseStyle;
    }

    @Override
    public GameComponent createPlayerNameComponent(GamePlayer player, String colorPrefix, boolean isDM, NameStyle nameStyle) {
        String username = player.getUsername();
        String displayName = player.getDisplayName();
        String strippedDisplayName = FormattingCodeUtils.stripFormattingCodes(displayName);

        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + username + " ");
        HoverEvent hoverEvent = null;

        String nameToShow;

        if (isDM) {
            nameToShow = username;
        } else if (nameStyle != null) {
            nameToShow = NicknameService.getNameForStyle(player, nameStyle);

            if (nameStyle == NameStyle.NICKNAME) {
                String nickname = NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
                }
            } else if (nameStyle == NameStyle.DISPLAY_NAME && !username.equals(strippedDisplayName)) {
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
            }
        } else {
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
            } else {
                nameToShow = username;
            }
        }

        String luckPermsPrefix = "";
        final String prefixTooltipText;

        if (Verbatim.prefixService != null && Verbatim.prefixService.isPrefixSystemAvailable()) {
            luckPermsPrefix = Verbatim.prefixService.getPlayerPrefix(player);

            if (!luckPermsPrefix.isEmpty()) {
                prefixTooltipText = Verbatim.prefixService.getPrefixTooltip(player);
            } else {
                prefixTooltipText = null;
            }
        } else {
            prefixTooltipText = null;
        }

        MutableComponent fullNameComponent = Component.empty();

        if (!luckPermsPrefix.isEmpty()) {
            MutableComponent prefixComponent = ((ForgeGameComponentImpl) parseColors(luckPermsPrefix)).toMinecraftMutable();

            if (prefixTooltipText != null && !prefixTooltipText.isEmpty()) {
                Component tooltipComponent = ((ForgeGameComponentImpl) parseColors(prefixTooltipText)).toMinecraft();
                prefixComponent = prefixComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipComponent))
                );
            }

            fullNameComponent.append(prefixComponent);

            if (!luckPermsPrefix.endsWith(" ")) {
                fullNameComponent.append(Component.literal(" "));
            }
        }

        MutableComponent nameComponent = ((ForgeGameComponentImpl) parseColors(colorPrefix + nameToShow)).toMinecraftMutable();

        final HoverEvent finalHoverEvent = hoverEvent;
        nameComponent = nameComponent.withStyle(style -> {
            Style updatedStyle = style.withClickEvent(clickEvent);
            if (finalHoverEvent != null) {
                updatedStyle = updatedStyle.withHoverEvent(finalHoverEvent);
            }
            return updatedStyle;
        });

        fullNameComponent.append(nameComponent);

        return ForgeGameComponentImpl.wrap(fullNameComponent);
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

        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + username + " ");
        HoverEvent hoverEvent = null;

        String nameToShow;

        if (isDM) {
            nameToShow = username;
        } else if (nameStyle != null) {
            nameToShow = NicknameService.getNameForStyle(player, nameStyle);

            if (nameStyle == NameStyle.NICKNAME) {
                String nickname = NicknameService.getNickname(player);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
                }
            } else if (nameStyle == NameStyle.DISPLAY_NAME && !username.equals(strippedDisplayName)) {
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
            }
        } else {
            if (!username.equals(strippedDisplayName)) {
                nameToShow = strippedDisplayName;
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Username: " + username).withStyle(ChatFormatting.GRAY));
            } else {
                nameToShow = username;
            }
        }

        nameToShow = FormattingCodeUtils.stripFormattingCodes(nameToShow);

        String luckPermsPrefix = "";
        final String prefixTooltipText;

        if (Verbatim.prefixService != null && Verbatim.prefixService.isPrefixSystemAvailable()) {
            luckPermsPrefix = Verbatim.prefixService.getPlayerPrefix(player);
            if (!luckPermsPrefix.isEmpty()) {
                prefixTooltipText = Verbatim.prefixService.getPrefixTooltip(player);
            } else {
                prefixTooltipText = null;
            }
        } else {
            prefixTooltipText = null;
        }

        MutableComponent fullNameComponent = Component.empty();

        if (!luckPermsPrefix.isEmpty()) {
            MutableComponent prefixComponent = ((ForgeGameComponentImpl) parseColors(luckPermsPrefix)).toMinecraftMutable();
            if (prefixTooltipText != null && !prefixTooltipText.isEmpty()) {
                Component tooltipComponent = ((ForgeGameComponentImpl) parseColors(prefixTooltipText)).toMinecraft();
                prefixComponent = prefixComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipComponent))
                );
            }
            fullNameComponent.append(prefixComponent);
            if (!luckPermsPrefix.endsWith(" ")) {
                fullNameComponent.append(Component.literal(" "));
            }
        }

        int len = nameToShow.length();
        final HoverEvent finalHoverEvent = hoverEvent;
        for (int i = 0; i < len; i++) {
            float ratio = len > 1 ? (float) i / (len - 1) : 0f;
            int r = Math.round(((gradientStartRgb >> 16) & 0xFF) + ratio * (((gradientEndRgb >> 16) & 0xFF) - ((gradientStartRgb >> 16) & 0xFF)));
            int g = Math.round(((gradientStartRgb >> 8) & 0xFF) + ratio * (((gradientEndRgb >> 8) & 0xFF) - ((gradientStartRgb >> 8) & 0xFF)));
            int b = Math.round((gradientStartRgb & 0xFF) + ratio * ((gradientEndRgb & 0xFF) - (gradientStartRgb & 0xFF)));

            MutableComponent charComponent = Component.literal(String.valueOf(nameToShow.charAt(i)));
            Style charStyle = Style.EMPTY
                .withColor(net.minecraft.network.chat.TextColor.fromRgb((r << 16) | (g << 8) | b))
                .withClickEvent(clickEvent);
            if (finalHoverEvent != null) {
                charStyle = charStyle.withHoverEvent(finalHoverEvent);
            }
            charComponent.setStyle(charStyle);
            fullNameComponent.append(charComponent);
        }

        return ForgeGameComponentImpl.wrap(fullNameComponent);
    }

    @Override
    public String createDiscordPlayerName(GamePlayer player, NameStyle nameStyle) {
        String username = player.getUsername();

        if (nameStyle == null) return username;

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

    @Override
    public GameComponent parseColorsWithPermissions(String text, GamePlayer player) {
        if (text == null || text.isEmpty()) {
            return ForgeGameComponentImpl.empty();
        }

        boolean hasColorPerm  = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        MutableComponent main = Component.literal("");
        String[] parts = text.split(COLOR_SPLIT_REGEX);
        Style current = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String content = part.substring(8);
                if (hasColorPerm) {
                    try {
                        current = Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(Integer.parseInt(hex, 16)));
                    } catch (NumberFormatException ignored) {}
                }
                if (!content.isEmpty()) {
                    main.append(makeLinksClickableInternal(content, current));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                String content = part.substring(2);

                if (fmt != null) {
                    boolean allow = false;
                    if (fmt.isColor()) {
                        allow = hasColorPerm;
                    } else if (fmt == ChatFormatting.RESET) {
                        allow = hasColorPerm || hasFormatPerm;
                    } else {
                        allow = hasFormatPerm;
                    }

                    if (allow) {
                        if (fmt.isColor()) {
                            current = Style.EMPTY.withColor(fmt);
                        } else if (fmt == ChatFormatting.RESET) {
                            current = Style.EMPTY;
                        } else {
                            current = applyStyle(current, fmt);
                        }
                    }
                }

                if (!content.isEmpty()) {
                    main.append(makeLinksClickableInternal(content, current));
                }
            } else {
                main.append(makeLinksClickableInternal(part, current));
            }
        }
        return ForgeGameComponentImpl.wrap(main);
    }

    @Override
    public GameComponent parsePlayerInputWithPermissions(String channelBaseColor, String playerInput, GamePlayer player) {
        if (playerInput == null || playerInput.isEmpty()) {
            return ForgeGameComponentImpl.empty();
        }

        Component baseColorComponent = ((ForgeGameComponentImpl) parseColors(channelBaseColor)).toMinecraft();
        Style baseStyle = baseColorComponent.getStyle();
        MutableComponent result = Component.empty();

        boolean hasColorPerm  = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        String[] parts = playerInput.split(COLOR_SPLIT_REGEX);
        Style current = baseStyle;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = part.substring(2, 8);
                String content = part.substring(8);
                if (hasColorPerm) {
                    try {
                        current = Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(Integer.parseInt(hex, 16)));
                    } catch (NumberFormatException ignored) {}
                }
                if (!content.isEmpty()) {
                    result.append(makeLinksClickableInternal(content, current));
                }
            } else if (part.startsWith("&") && part.length() >= 2) {
                char code = part.charAt(1);
                ChatFormatting fmt = ChatFormatting.getByCode(code);
                String content = part.substring(2);

                if (fmt != null) {
                    boolean allow = false;
                    if (fmt.isColor()) {
                        allow = hasColorPerm;
                    } else if (fmt == ChatFormatting.RESET) {
                        allow = hasColorPerm || hasFormatPerm;
                    } else {
                        allow = hasFormatPerm;
                    }

                    if (allow) {
                        if (fmt.isColor()) {
                            current = Style.EMPTY.withColor(fmt);
                        } else if (fmt == ChatFormatting.RESET) {
                            current = baseStyle;
                        } else {
                            current = applyStyle(current, fmt);
                        }
                    }
                }

                if (!content.isEmpty()) {
                    result.append(makeLinksClickableInternal(content, current));
                }
            } else {
                result.append(makeLinksClickableInternal(part, current));
            }
        }

        return ForgeGameComponentImpl.wrap(result);
    }
}
