package world.landfall.verbatim.context;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Implementation of GameComponent that wraps Minecraft's MutableComponent.
 */
public class GameComponentImpl implements GameComponent {

    private MutableComponent wrapped;

    public GameComponentImpl(MutableComponent component) {
        this.wrapped = component;
    }

    public GameComponentImpl(String text) {
        this.wrapped = Component.literal(text);
    }

    public static GameComponent empty() {
        return new GameComponentImpl(Component.empty());
    }

    public static GameComponent literal(String text) {
        return new GameComponentImpl(Component.literal(text));
    }

    public static GameComponent wrap(Component component) {
        if (component instanceof MutableComponent mutable) {
            return new GameComponentImpl(mutable);
        }
        return new GameComponentImpl(component.copy());
    }

    @Override
    public GameComponent append(GameComponent component) {
        wrapped.append(component.toMinecraft());
        return this;
    }

    @Override
    public GameComponent append(String text) {
        wrapped.append(Component.literal(text));
        return this;
    }

    @Override
    public GameComponent withColor(GameColor color) {
        wrapped = wrapped.withStyle(color.toMinecraft());
        return this;
    }

    @Override
    public GameComponent withBold(boolean bold) {
        wrapped = wrapped.withStyle(Style.EMPTY.withBold(bold));
        return this;
    }

    @Override
    public GameComponent withItalic(boolean italic) {
        wrapped = wrapped.withStyle(Style.EMPTY.withItalic(italic));
        return this;
    }

    @Override
    public GameComponent withUnderlined(boolean underlined) {
        wrapped = wrapped.withStyle(Style.EMPTY.withUnderlined(underlined));
        return this;
    }

    @Override
    public GameComponent withStrikethrough(boolean strikethrough) {
        wrapped = wrapped.withStyle(Style.EMPTY.withStrikethrough(strikethrough));
        return this;
    }

    @Override
    public GameComponent withObfuscated(boolean obfuscated) {
        wrapped = wrapped.withStyle(Style.EMPTY.withObfuscated(obfuscated));
        return this;
    }

    @Override
    public GameComponent withClickSuggestCommand(String command) {
        wrapped = wrapped.withStyle(wrapped.getStyle().withClickEvent(
            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        return this;
    }

    @Override
    public GameComponent withClickRunCommand(String command) {
        wrapped = wrapped.withStyle(wrapped.getStyle().withClickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
        return this;
    }

    @Override
    public GameComponent withClickOpenUrl(String url) {
        wrapped = wrapped.withStyle(wrapped.getStyle().withClickEvent(
            new ClickEvent(ClickEvent.Action.OPEN_URL, url)));
        return this;
    }

    @Override
    public GameComponent withClickCopyToClipboard(String text) {
        wrapped = wrapped.withStyle(wrapped.getStyle().withClickEvent(
            new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
        return this;
    }

    @Override
    public GameComponent withHoverText(GameComponent text) {
        wrapped = wrapped.withStyle(wrapped.getStyle().withHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, text.toMinecraft())));
        return this;
    }

    @Override
    public GameComponent withHoverText(String text) {
        wrapped = wrapped.withStyle(wrapped.getStyle().withHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(text))));
        return this;
    }

    @Override
    public String getString() {
        return wrapped.getString();
    }

    @Override
    public GameComponent copy() {
        return new GameComponentImpl(wrapped.copy());
    }

    @Override
    public Component toMinecraft() {
        return wrapped;
    }

    @Override
    public MutableComponent toMinecraftMutable() {
        return wrapped;
    }
}
