package world.landfall.verbatim.context;

/**
 * Platform-independent interface for text components.
 * No Minecraft-specific types in this interface.
 */
public interface GameComponent {

    /**
     * Appends another component to this one.
     */
    GameComponent append(GameComponent component);

    /**
     * Appends a string literal to this component.
     */
    GameComponent append(String text);

    /**
     * Applies a color to this component.
     */
    GameComponent withColor(GameColor color);

    /**
     * Sets whether this component is bold.
     */
    GameComponent withBold(boolean bold);

    /**
     * Sets whether this component is italic.
     */
    GameComponent withItalic(boolean italic);

    /**
     * Sets whether this component is underlined.
     */
    GameComponent withUnderlined(boolean underlined);

    /**
     * Sets whether this component has strikethrough.
     */
    GameComponent withStrikethrough(boolean strikethrough);

    /**
     * Sets whether this component is obfuscated.
     */
    GameComponent withObfuscated(boolean obfuscated);

    /**
     * Adds a click event that suggests a command.
     */
    GameComponent withClickSuggestCommand(String command);

    /**
     * Adds a click event that runs a command.
     */
    GameComponent withClickRunCommand(String command);

    /**
     * Adds a click event that opens a URL.
     */
    GameComponent withClickOpenUrl(String url);

    /**
     * Adds a click event that copies text to clipboard.
     */
    GameComponent withClickCopyToClipboard(String text);

    /**
     * Adds a hover event that shows text.
     */
    GameComponent withHoverText(GameComponent text);

    /**
     * Adds a hover event that shows text.
     */
    GameComponent withHoverText(String text);

    /**
     * Gets the plain string content of this component.
     */
    String getString();

    /**
     * Creates a copy of this component.
     */
    GameComponent copy();
}
