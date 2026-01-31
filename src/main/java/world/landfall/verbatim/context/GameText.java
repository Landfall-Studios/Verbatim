package world.landfall.verbatim.context;

import world.landfall.verbatim.Verbatim;

/**
 * Static helper methods for creating GameComponents with minimal verbosity.
 *
 * Usage with static import:
 * import static world.landfall.verbatim.context.GameText.*;
 *
 * Then use: text("Hello").withColor(GameColor.GREEN)
 * Instead of: Verbatim.gameContext.createText("Hello").withColor(GameColor.GREEN)
 */
public final class GameText {

    private GameText() {
        // Utility class
    }

    /**
     * Creates a text component from a string.
     * @param content the text content
     * @return a new GameComponent
     */
    public static GameComponent text(String content) {
        return Verbatim.gameContext.createText(content);
    }

    /**
     * Creates an empty component.
     * @return an empty GameComponent
     */
    public static GameComponent empty() {
        return Verbatim.gameContext.createEmpty();
    }
}
