package world.landfall.verbatim.platform.hytale;

import com.hypixel.hytale.server.core.Message;
import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;

/**
 * Hytale implementation of GameComponent that wraps Hytale's Message class.
 *
 * Hytale's Message API:
 * - Message.raw("text") for literal text
 * - Message.empty() for empty message
 * - Message.join(msg1, msg2, ...) for concatenation
 * - .color(java.awt.Color) for coloring
 * - .bold(boolean), .italic(boolean), .monospace(boolean) for styling
 * - .link(String) for clickable URLs
 * - .insert(Message) / .insertAll(Message...) for appending children
 * - .getRawText() for plain text extraction
 */
public class HytaleGameComponentImpl implements GameComponent {

    private Message wrapped;

    public HytaleGameComponentImpl(Message message) {
        this.wrapped = message;
    }

    public HytaleGameComponentImpl(String text) {
        this.wrapped = Message.raw(text);
    }

    public static GameComponent empty() {
        return new HytaleGameComponentImpl(Message.empty());
    }

    public static GameComponent literal(String text) {
        return new HytaleGameComponentImpl(Message.raw(text));
    }

    public static GameComponent wrap(Message message) {
        return new HytaleGameComponentImpl(message);
    }

    /**
     * Gets the underlying Hytale Message.
     * Only call from platform layer code.
     */
    public Message toHytale() {
        return wrapped;
    }

    @Override
    public GameComponent append(GameComponent component) {
        if (component instanceof HytaleGameComponentImpl impl) {
            wrapped = Message.join(wrapped, impl.toHytale());
        } else {
            wrapped = Message.join(wrapped, Message.raw(component.getString()));
        }
        return this;
    }

    @Override
    public GameComponent append(String text) {
        wrapped = Message.join(wrapped, Message.raw(text));
        return this;
    }

    @Override
    public GameComponent withColor(GameColor color) {
        wrapped = wrapped.color(HytaleColorBridge.toHytaleColor(color));
        return this;
    }

    @Override
    public GameComponent withBold(boolean bold) {
        wrapped = wrapped.bold(bold);
        return this;
    }

    @Override
    public GameComponent withItalic(boolean italic) {
        wrapped = wrapped.italic(italic);
        return this;
    }

    @Override
    public GameComponent withUnderlined(boolean underlined) {
        // Hytale's Message API does not expose an underline method.
        return this;
    }

    @Override
    public GameComponent withStrikethrough(boolean strikethrough) {
        // Hytale's Message API does not expose a strikethrough method.
        return this;
    }

    @Override
    public GameComponent withObfuscated(boolean obfuscated) {
        // Hytale's Message API does not expose an obfuscated method.
        return this;
    }

    @Override
    public GameComponent withClickSuggestCommand(String command) {
        // Hytale's Message API does not expose suggest command click events.
        return this;
    }

    @Override
    public GameComponent withClickRunCommand(String command) {
        // Hytale's Message API does not expose run command click events.
        return this;
    }

    @Override
    public GameComponent withClickOpenUrl(String url) {
        wrapped = wrapped.link(url);
        return this;
    }

    @Override
    public GameComponent withClickCopyToClipboard(String text) {
        // Hytale's Message API does not expose copy-to-clipboard click events.
        return this;
    }

    @Override
    public GameComponent withHoverText(GameComponent text) {
        // Hytale's Message API does not expose hover events directly.
        return this;
    }

    @Override
    public GameComponent withHoverText(String text) {
        // Hytale's Message API does not expose hover events directly.
        return this;
    }

    @Override
    public String getString() {
        String raw = wrapped.getRawText();
        if (raw != null) return raw;
        // getRawText() returns null for compound/joined messages;
        // recursively extract text from children
        return extractText(wrapped);
    }

    private static String extractText(Message msg) {
        StringBuilder sb = new StringBuilder();
        String raw = msg.getRawText();
        if (raw != null) {
            sb.append(raw);
        }
        java.util.List<Message> children = msg.getChildren();
        if (children != null) {
            for (Message child : children) {
                String childText = extractText(child);
                if (childText != null) {
                    sb.append(childText);
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @Override
    public GameComponent copy() {
        // Message is immutable in Hytale, so the same reference is safe
        return new HytaleGameComponentImpl(wrapped);
    }
}
