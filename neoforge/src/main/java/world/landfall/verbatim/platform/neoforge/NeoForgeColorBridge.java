package world.landfall.verbatim.platform.neoforge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import world.landfall.verbatim.context.GameColor;

/**
 * Conversion utility between GameColor and Minecraft ChatFormatting/TextColor.
 */
public final class NeoForgeColorBridge {

    private NeoForgeColorBridge() {}

    private static final ChatFormatting[] MINECRAFT_COLORS = {
        ChatFormatting.BLACK,       // 0
        ChatFormatting.DARK_BLUE,   // 1
        ChatFormatting.DARK_GREEN,  // 2
        ChatFormatting.DARK_AQUA,   // 3
        ChatFormatting.DARK_RED,    // 4
        ChatFormatting.DARK_PURPLE, // 5
        ChatFormatting.GOLD,        // 6
        ChatFormatting.GRAY,        // 7
        ChatFormatting.DARK_GRAY,   // 8
        ChatFormatting.BLUE,        // 9
        ChatFormatting.GREEN,       // a
        ChatFormatting.AQUA,        // b
        ChatFormatting.RED,         // c
        ChatFormatting.LIGHT_PURPLE,// d
        ChatFormatting.YELLOW,      // e
        ChatFormatting.WHITE        // f
    };

    /**
     * Converts a GameColor to its Minecraft ChatFormatting equivalent.
     */
    public static ChatFormatting toMinecraft(GameColor color) {
        return MINECRAFT_COLORS[color.ordinal()];
    }

    /**
     * Converts a GameColor to a Minecraft TextColor.
     */
    public static TextColor toTextColor(GameColor color) {
        return TextColor.fromRgb(color.getRgb());
    }

    /**
     * Converts a Minecraft ChatFormatting to a GameColor.
     * @return the matching GameColor, or WHITE as fallback
     */
    public static GameColor fromMinecraft(ChatFormatting formatting) {
        if (formatting == null || !formatting.isColor()) {
            return GameColor.WHITE;
        }
        for (int i = 0; i < MINECRAFT_COLORS.length; i++) {
            if (MINECRAFT_COLORS[i] == formatting) {
                return GameColor.values()[i];
            }
        }
        return GameColor.WHITE;
    }
}
