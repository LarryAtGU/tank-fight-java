package tankfight.controller;

import tankfight.model.Direction;
import tankfight.model.GameModel;
import tankfight.model.PlayerAction;
import tankfight.view.GameView;

import javax.swing.Timer;

public class GameController {

    private static final int FPS = 60;

    private final GameModel model;
    private final GameView view;
    private final InputSource input;
    private final KeyBindings player1Bindings;
    private final KeyBindings player2Bindings;
    private final int restartKey;
    private final Timer timer;

    public GameController(GameModel model, GameView view, InputSource input,
                           KeyBindings player1Bindings, KeyBindings player2Bindings, int restartKey) {
        this.model = model;
        this.view = view;
        this.input = input;
        this.player1Bindings = player1Bindings;
        this.player2Bindings = player2Bindings;
        this.restartKey = restartKey;
        this.timer = new Timer(1000 / FPS, e -> tick());
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    void tick() {
        long now = System.currentTimeMillis();
        if (model.isGameOver()) {
            if (input.isPressed(restartKey)) {
                model.reset();
            }
        } else {
            PlayerAction action1 = actionFor(player1Bindings);
            PlayerAction action2 = actionFor(player2Bindings);
            model.update(action1, action2, now);
        }
        view.refresh();
    }

    private PlayerAction actionFor(KeyBindings bindings) {
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
        boolean fire = input.isPressed(bindings.fire());
        return new PlayerAction(direction, fire);
    }
}
