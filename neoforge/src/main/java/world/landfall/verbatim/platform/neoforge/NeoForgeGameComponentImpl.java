package world.landfall.verbatim.platform.neoforge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;

/**
 * NeoForge implementation of GameComponent that wraps Minecraft's MutableComponent.
 */
public class NeoForgeGameComponentImpl implements GameComponent {

    private MutableComponent wrapped;

    public NeoForgeGameComponentImpl(MutableComponent component) {
        this.wrapped = component;
    }

    public NeoForgeGameComponentImpl(String text) {
        this.wrapped = Component.literal(text);
    }

    public static GameComponent empty() {
        return new NeoForgeGameComponentImpl(Component.empty());
    }

    public static GameComponent literal(String text) {
        return new NeoForgeGameComponentImpl(Component.literal(text));
    }

    public static GameComponent wrap(Component component) {
        if (component instanceof MutableComponent mutable) {
            return new NeoForgeGameComponentImpl(mutable);
        }
        return new NeoForgeGameComponentImpl(component.copy());
    }

    /**
     * Gets the underlying Minecraft Component.
     * Only call from platform layer code.
     */
    public Component toMinecraft() {
        return wrapped;
    }

    /**
     * Gets the underlying Minecraft MutableComponent.
     * Only call from platform layer code.
     */
    public MutableComponent toMinecraftMutable() {
        return wrapped;
    }

    @Override
    public GameComponent append(GameComponent component) {
        if (component instanceof NeoForgeGameComponentImpl impl) {
            wrapped.append(impl.toMinecraft());
        } else {
            wrapped.append(Component.literal(component.getString()));
        }
        return this;
    }

    @Override
    public GameComponent append(String text) {
        wrapped.append(Component.literal(text));
        return this;
    }

    @Override
    public GameComponent withColor(GameColor color) {
        wrapped = wrapped.withStyle(NeoForgeColorBridge.toMinecraft(color));
        return this;
    }

    @Override
    public GameComponent withRgbColor(int rgb) {
        wrapped = wrapped.withStyle(Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb)));
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
        Component hoverComponent;
        if (text instanceof NeoForgeGameComponentImpl impl) {
            hoverComponent = impl.toMinecraft();
        } else {
            hoverComponent = Component.literal(text.getString());
        }
        wrapped = wrapped.withStyle(wrapped.getStyle().withHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent)));
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
        return new NeoForgeGameComponentImpl(wrapped.copy());
    }
}
