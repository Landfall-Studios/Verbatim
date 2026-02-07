package world.landfall.verbatim.context;

/**
 * Platform-independent color enumeration for text styling.
 * Each color stores its formatting code character and RGB value.
 */
public enum GameColor {
    BLACK('0', 0x000000),
    DARK_BLUE('1', 0x0000AA),
    DARK_GREEN('2', 0x00AA00),
    DARK_AQUA('3', 0x00AAAA),
    DARK_RED('4', 0xAA0000),
    DARK_PURPLE('5', 0xAA00AA),
    GOLD('6', 0xFFAA00),
    GRAY('7', 0xAAAAAA),
    DARK_GRAY('8', 0x555555),
    BLUE('9', 0x5555FF),
    GREEN('a', 0x55FF55),
    AQUA('b', 0x55FFFF),
    RED('c', 0xFF5555),
    LIGHT_PURPLE('d', 0xFF55FF),
    YELLOW('e', 0xFFFF55),
    WHITE('f', 0xFFFFFF);

    private final char code;
    private final int rgb;

    GameColor(char code, int rgb) {
        this.code = code;
        this.rgb = rgb;
    }

    /**
     * Gets the formatting code character (e.g., '0' for BLACK, 'a' for GREEN).
     */
    public char getCode() {
        return code;
    }

    /**
     * Gets the RGB color value.
     */
    public int getRgb() {
        return rgb;
    }

    /**
     * Finds a GameColor by its formatting code character.
     * @return the matching GameColor, or WHITE as fallback
     */
    public static GameColor fromCode(char code) {
        char lower = Character.toLowerCase(code);
        for (GameColor color : values()) {
            if (color.code == lower) {
                return color;
            }
        }
        return WHITE;
    }
}
