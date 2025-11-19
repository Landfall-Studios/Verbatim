package world.landfall.verbatim.context;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Platform-independent interface for text components.
 * Wraps Minecraft's Component/MutableComponent system.
 */
public interface GameComponent {

    /**
     * Appends another component to this one.
     * @param component the component to append
     * @return this component for chaining
     */
    GameComponent append(GameComponent component);

    /**
     * Appends a string literal to this component.
     * @param text the text to append
     * @return this component for chaining
     */
    GameComponent append(String text);

    /**
     * Applies a color to this component.
     * @param color the color to apply
     * @return this component for chaining
     */
    GameComponent withColor(GameColor color);

    /**
     * Sets whether this component is bold.
     * @param bold true for bold
     * @return this component for chaining
     */
    GameComponent withBold(boolean bold);

    /**
     * Sets whether this component is italic.
     * @param italic true for italic
     * @return this component for chaining
     */
    GameComponent withItalic(boolean italic);

    /**
     * Sets whether this component is underlined.
     * @param underlined true for underlined
     * @return this component for chaining
     */
    GameComponent withUnderlined(boolean underlined);

    /**
     * Sets whether this component has strikethrough.
     * @param strikethrough true for strikethrough
     * @return this component for chaining
     */
    GameComponent withStrikethrough(boolean strikethrough);

    /**
     * Sets whether this component is obfuscated.
     * @param obfuscated true for obfuscated
     * @return this component for chaining
     */
    GameComponent withObfuscated(boolean obfuscated);

    /**
     * Adds a click event that suggests a command.
     * @param command the command to suggest
     * @return this component for chaining
     */
    GameComponent withClickSuggestCommand(String command);

    /**
     * Adds a click event that runs a command.
     * @param command the command to run
     * @return this component for chaining
     */
    GameComponent withClickRunCommand(String command);

    /**
     * Adds a click event that opens a URL.
     * @param url the URL to open
     * @return this component for chaining
     */
    GameComponent withClickOpenUrl(String url);

    /**
     * Adds a click event that copies text to clipboard.
     * @param text the text to copy
     * @return this component for chaining
     */
    GameComponent withClickCopyToClipboard(String text);

    /**
     * Adds a hover event that shows text.
     * @param text the text to show on hover
     * @return this component for chaining
     */
    GameComponent withHoverText(GameComponent text);

    /**
     * Adds a hover event that shows text.
     * @param text the text to show on hover
     * @return this component for chaining
     */
    GameComponent withHoverText(String text);

    /**
     * Gets the plain string content of this component.
     * @return the string content
     */
    String getString();

    /**
     * Creates a copy of this component.
     * @return a copy
     */
    GameComponent copy();

    /**
     * Gets the underlying Minecraft Component.
     * This is the bridge to the Minecraft-specific implementation.
     * @return the Minecraft Component
     */
    Component toMinecraft();

    /**
     * Gets the underlying Minecraft MutableComponent.
     * @return the Minecraft MutableComponent
     */
    MutableComponent toMinecraftMutable();
}
