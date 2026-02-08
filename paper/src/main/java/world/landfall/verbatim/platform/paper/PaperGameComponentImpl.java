package world.landfall.verbatim.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;

/**
 * Paper implementation of GameComponent wrapping Adventure's Component.
 *
 * Paper uses Adventure natively, which supports every GameComponent feature:
 * hover events, click events (suggest/run/url/clipboard), all text decorations.
 * This is a mutable wrapper around an immutable Component.
 */
public class PaperGameComponentImpl implements GameComponent {

    private Component wrapped;

    public PaperGameComponentImpl(Component component) {
        this.wrapped = component;
    }

    public PaperGameComponentImpl(String text) {
        this.wrapped = Component.text(text);
    }

    public static GameComponent empty() {
        return new PaperGameComponentImpl(Component.empty());
    }

    public static GameComponent literal(String text) {
        return new PaperGameComponentImpl(Component.text(text));
    }

    public static GameComponent wrap(Component component) {
        return new PaperGameComponentImpl(component);
    }

    /**
     * Gets the underlying Adventure Component.
     * Only call from platform layer code.
     */
    public Component toAdventure() {
        return wrapped;
    }

    @Override
    public GameComponent append(GameComponent component) {
        if (component instanceof PaperGameComponentImpl impl) {
            wrapped = wrapped.append(impl.toAdventure());
        } else {
            wrapped = wrapped.append(Component.text(component.getString()));
        }
        return this;
    }

    @Override
    public GameComponent append(String text) {
        wrapped = wrapped.append(Component.text(text));
        return this;
    }

    @Override
    public GameComponent withColor(GameColor color) {
        wrapped = wrapped.color(TextColor.color(color.getRgb()));
        return this;
    }

    @Override
    public GameComponent withRgbColor(int rgb) {
        wrapped = wrapped.color(TextColor.color(rgb));
        return this;
    }

    @Override
    public GameComponent withBold(boolean bold) {
        wrapped = wrapped.decoration(TextDecoration.BOLD, bold);
        return this;
    }

    @Override
    public GameComponent withItalic(boolean italic) {
        wrapped = wrapped.decoration(TextDecoration.ITALIC, italic);
        return this;
    }

    @Override
    public GameComponent withUnderlined(boolean underlined) {
        wrapped = wrapped.decoration(TextDecoration.UNDERLINED, underlined);
        return this;
    }

    @Override
    public GameComponent withStrikethrough(boolean strikethrough) {
        wrapped = wrapped.decoration(TextDecoration.STRIKETHROUGH, strikethrough);
        return this;
    }

    @Override
    public GameComponent withObfuscated(boolean obfuscated) {
        wrapped = wrapped.decoration(TextDecoration.OBFUSCATED, obfuscated);
        return this;
    }

    @Override
    public GameComponent withClickSuggestCommand(String command) {
        wrapped = wrapped.clickEvent(ClickEvent.suggestCommand(command));
        return this;
    }

    @Override
    public GameComponent withClickRunCommand(String command) {
        wrapped = wrapped.clickEvent(ClickEvent.runCommand(command));
        return this;
    }

    @Override
    public GameComponent withClickOpenUrl(String url) {
        wrapped = wrapped.clickEvent(ClickEvent.openUrl(url));
        return this;
    }

    @Override
    public GameComponent withClickCopyToClipboard(String text) {
        wrapped = wrapped.clickEvent(ClickEvent.copyToClipboard(text));
        return this;
    }

    @Override
    public GameComponent withHoverText(GameComponent text) {
        Component hoverComponent;
        if (text instanceof PaperGameComponentImpl impl) {
            hoverComponent = impl.toAdventure();
        } else {
            hoverComponent = Component.text(text.getString());
        }
        wrapped = wrapped.hoverEvent(HoverEvent.showText(hoverComponent));
        return this;
    }

    @Override
    public GameComponent withHoverText(String text) {
        wrapped = wrapped.hoverEvent(HoverEvent.showText(Component.text(text)));
        return this;
    }

    @Override
    public String getString() {
        return PlainTextComponentSerializer.plainText().serialize(wrapped);
    }

    @Override
    public GameComponent copy() {
        // Adventure Components are immutable, so same reference in a new wrapper is safe
        return new PaperGameComponentImpl(wrapped);
    }
}
