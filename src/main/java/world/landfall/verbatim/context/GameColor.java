package world.landfall.verbatim.context;

import net.minecraft.ChatFormatting;

/**
 * Platform-independent color enumeration for text styling.
 */
public enum GameColor {
    BLACK(ChatFormatting.BLACK),
    DARK_BLUE(ChatFormatting.DARK_BLUE),
    DARK_GREEN(ChatFormatting.DARK_GREEN),
    DARK_AQUA(ChatFormatting.DARK_AQUA),
    DARK_RED(ChatFormatting.DARK_RED),
    DARK_PURPLE(ChatFormatting.DARK_PURPLE),
    GOLD(ChatFormatting.GOLD),
    GRAY(ChatFormatting.GRAY),
    DARK_GRAY(ChatFormatting.DARK_GRAY),
    BLUE(ChatFormatting.BLUE),
    GREEN(ChatFormatting.GREEN),
    AQUA(ChatFormatting.AQUA),
    RED(ChatFormatting.RED),
    LIGHT_PURPLE(ChatFormatting.LIGHT_PURPLE),
    YELLOW(ChatFormatting.YELLOW),
    WHITE(ChatFormatting.WHITE);

    private final ChatFormatting minecraftFormatting;

    GameColor(ChatFormatting formatting) {
        this.minecraftFormatting = formatting;
    }

    /**
     * Gets the Minecraft ChatFormatting equivalent.
     * This is the only place that references the Minecraft-specific type.
     */
    public ChatFormatting toMinecraft() {
        return minecraftFormatting;
    }

    /**
     * Converts a Minecraft ChatFormatting to GameColor.
     */
    public static GameColor fromMinecraft(ChatFormatting formatting) {
        for (GameColor color : values()) {
            if (color.minecraftFormatting == formatting) {
                return color;
            }
        }
        return WHITE; // Default fallback
    }
}
