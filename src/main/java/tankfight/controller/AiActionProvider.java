package tankfight.controller;

import tankfight.model.GameModel;
import tankfight.model.PlayerAction;
import tankfight.model.Tank;
import tankfight.model.TankAi;

/** Fills a player slot with a {@link TankAi} instead of a keyboard. */
public class AiActionProvider implements ActionProvider {

    private final TankAi ai;

    public AiActionProvider(TankAi ai) {
        this.ai = ai;
    }

    @Override
    public PlayerAction actionFor(GameModel model, Tank tank, long now) {
        return ai.decide(model, tank, now);
    }
}
