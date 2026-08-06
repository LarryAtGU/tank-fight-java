package tankfight.controller;

import tankfight.model.GameModel;
import tankfight.model.PlayerAction;
import tankfight.model.Tank;

/**
 * Supplies one tick's {@link PlayerAction} for an ally tank. The two implementations —
 * keyboard and AI — are interchangeable, which is what lets either player slot be human or
 * computer-driven without the model knowing the difference.
 */
public interface ActionProvider {

    PlayerAction actionFor(GameModel model, Tank tank, long now);
}
