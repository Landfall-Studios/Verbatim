package world.landfall.verbatim.platform.neoforge;

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
// NeoForgeGameComponentImpl is in this package
import world.landfall.verbatim.context.GamePlayer;
import world.landfall.verbatim.util.FormattingCodeUtils;
import world.landfall.verbatim.util.NicknameService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NeoForge implementation of ChatFormatter.
 * Contains all Minecraft-specific formatting logic previously in ChatFormattingUtils.
 */
public class NeoForgeChatFormatter implements ChatFormatter {

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
        // Extract the style from the base component
        Style baseStyle = Style.EMPTY;
        if (baseStyleComponent instanceof NeoForgeGameComponentImpl impl) {
            baseStyle = impl.toMinecraftMutable().getStyle();
        }
        return NeoForgeGameComponentImpl.wrap(makeLinksClickableInternal(text, baseStyle));
    }

    @Override
    public GameComponent parseColors(String text) {
        if (text == null || text.isEmpty()) {
            return NeoForgeGameComponentImpl.empty();
        }

        MutableComponent mainComponent = Component.literal("");
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");

        Style currentStyle = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
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
        return NeoForgeGameComponentImpl.wrap(mainComponent);
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

        if (Verbatim.prefixService != null && Verbatim.prefixService.isLuckPermsAvailable()) {
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
            MutableComponent prefixComponent = ((NeoForgeGameComponentImpl) parseColors(luckPermsPrefix)).toMinecraftMutable();

            if (prefixTooltipText != null && !prefixTooltipText.isEmpty()) {
                Component tooltipComponent = ((NeoForgeGameComponentImpl) parseColors(prefixTooltipText)).toMinecraft();
                prefixComponent = prefixComponent.withStyle(style ->
                    style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltipComponent))
                );
            }

            fullNameComponent.append(prefixComponent);

            if (!luckPermsPrefix.endsWith(" ")) {
                fullNameComponent.append(Component.literal(" "));
            }
        }

        MutableComponent nameComponent = ((NeoForgeGameComponentImpl) parseColors(colorPrefix + nameToShow)).toMinecraftMutable();

        final HoverEvent finalHoverEvent = hoverEvent;
        nameComponent = nameComponent.withStyle(style -> {
            Style updatedStyle = style.withClickEvent(clickEvent);
            if (finalHoverEvent != null) {
                updatedStyle = updatedStyle.withHoverEvent(finalHoverEvent);
            }
            return updatedStyle;
        });

        fullNameComponent.append(nameComponent);

        return NeoForgeGameComponentImpl.wrap(fullNameComponent);
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

    @Override
    public GameComponent parseColorsWithPermissions(String text, GamePlayer player) {
        if (text == null || text.isEmpty()) {
            return NeoForgeGameComponentImpl.empty();
        }

        boolean hasColorPerm  = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        MutableComponent main = Component.literal("");
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");
        Style current = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
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
        return NeoForgeGameComponentImpl.wrap(main);
    }

    @Override
    public GameComponent parsePlayerInputWithPermissions(String channelBaseColor, String playerInput, GamePlayer player) {
        if (playerInput == null || playerInput.isEmpty()) {
            return NeoForgeGameComponentImpl.empty();
        }

        Component baseColorComponent = ((NeoForgeGameComponentImpl) parseColors(channelBaseColor)).toMinecraft();
        Style baseStyle = baseColorComponent.getStyle();
        MutableComponent result = Component.empty();

        boolean hasColorPerm  = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_COLOR, 2);
        boolean hasFormatPerm = Verbatim.permissionService.hasPermission(player, NicknameService.PERM_CHAT_FORMAT, 2);

        String[] parts = playerInput.split("(?i)(?=&[0-9a-fk-or])");
        Style current = baseStyle;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("&") && part.length() >= 2) {
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

        return NeoForgeGameComponentImpl.wrap(result);
    }
}
