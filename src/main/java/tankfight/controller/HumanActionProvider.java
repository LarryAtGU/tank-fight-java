package tankfight.controller;

import tankfight.model.Direction;
import tankfight.model.GameModel;
import tankfight.model.PlayerAction;
import tankfight.model.Tank;

/** Turns a person's held keys into a {@link PlayerAction}. */
public class HumanActionProvider implements ActionProvider {

    private final InputSource input;
    private final KeyBindings bindings;

    public HumanActionProvider(InputSource input, KeyBindings bindings) {
        this.input = input;
        this.bindings = bindings;
    }

    @Override
    public PlayerAction actionFor(GameModel model, Tank tank, long now) {
        Direction direction = null;
        if (input.isPressed(bindings.up())) {
            direction = Direction.UP;
        } else if (input.isPressed(bindings.down())) {
            direction = Direction.DOWN;
        } else if (input.isPressed(bindings.left())) {
            direction = Direction.LEFT;
        } else if (input.isPressed(bindings.right())) {
            direction = Direction.RIGHT;
        }
        return new PlayerAction(direction, input.isPressed(bindings.fire()));
    }
}
