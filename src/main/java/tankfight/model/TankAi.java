package tankfight.model;

/**
 * Decides one tick's worth of intent for a computer-driven tank.
 *
 * <p>AI produces the same {@link PlayerAction} a human's key presses do, so the model applies
 * both through exactly one code path and neither side gets abilities the other lacks.
 */
public interface TankAi {

    /**
     * @param model the live game, used only to read state (positions, blocked directions)
     * @param tank  the tank being driven; always alive and belonging to {@code model}
     * @param now   the current tick's timestamp in milliseconds
     */
    PlayerAction decide(GameModel model, Tank tank, long now);
}
