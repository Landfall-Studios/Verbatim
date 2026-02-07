package world.landfall.verbatim.test;

import world.landfall.verbatim.context.GameColor;
import world.landfall.verbatim.context.GameComponent;

/**
 * Mock GameComponent for unit testing.
 * Tracks the text content and styling for assertions.
 */
public class MockGameComponent implements GameComponent {
    private StringBuilder content = new StringBuilder();
    private GameColor color;
    private int rgbColor = -1;
    private boolean bold;
    private boolean italic;
    private boolean underlined;

    public MockGameComponent() {}

    public MockGameComponent(String text) {
        this.content.append(text);
    }

    @Override
    public GameComponent append(GameComponent component) {
        if (component instanceof MockGameComponent mock) {
            content.append(mock.content);
        } else {
            content.append(component.getString());
        }
        return this;
    }

    @Override
    public GameComponent append(String text) {
        content.append(text);
        return this;
    }

    @Override
    public GameComponent withColor(GameColor color) {
        this.color = color;
        return this;
    }

    @Override
    public GameComponent withRgbColor(int rgb) {
        this.rgbColor = rgb;
        return this;
    }

    @Override
    public GameComponent withBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    @Override
    public GameComponent withItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    @Override
    public GameComponent withUnderlined(boolean underlined) {
        this.underlined = underlined;
        return this;
    }

    @Override
    public GameComponent withStrikethrough(boolean strikethrough) {
        return this;
    }

    @Override
    public GameComponent withObfuscated(boolean obfuscated) {
        return this;
    }

    @Override
    public GameComponent withClickSuggestCommand(String command) {
        return this;
    }

    @Override
    public GameComponent withClickRunCommand(String command) {
        return this;
    }

    @Override
    public GameComponent withClickOpenUrl(String url) {
        return this;
    }

    @Override
    public GameComponent withClickCopyToClipboard(String text) {
        return this;
    }

    @Override
    public GameComponent withHoverText(GameComponent text) {
        return this;
    }

    @Override
    public GameComponent withHoverText(String text) {
        return this;
    }

    @Override
    public String getString() {
        return content.toString();
    }

    @Override
    public GameComponent copy() {
        MockGameComponent copy = new MockGameComponent(content.toString());
        copy.color = this.color;
        copy.rgbColor = this.rgbColor;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underlined = this.underlined;
        return copy;
    }

    @Override
    public String toString() {
        return content.toString();
    }

    // Test helpers
    public GameColor getColor() { return color; }
    public int getRgbColor() { return rgbColor; }
    public boolean isBold() { return bold; }
    public boolean isItalic() { return italic; }
    public boolean isUnderlined() { return underlined; }
}
