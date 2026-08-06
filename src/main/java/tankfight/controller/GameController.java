package tankfight.controller;

import tankfight.model.AllyTankAi;
import tankfight.model.ControlType;
import tankfight.model.GameModel;
import tankfight.model.GameSetup;
import tankfight.model.Player;
import tankfight.model.PlayerAction;
import tankfight.model.Tank;
import tankfight.view.GameView;

import javax.swing.Timer;
import java.util.List;
import java.util.Random;

public class GameController {

    private static final int FPS = 60;

    private final GameModel model;
    private final GameView view;
    private final InputSource input;
    private final KeyBindings player1Bindings;
    private final KeyBindings player2Bindings;
    private final int restartKey;
    private final int menuKey;
    private final int abortKey;
    private final Timer timer;
    private final ActionProvider aiProvider;

    private ActionProvider player1Provider;
    private ActionProvider player2Provider;

    /**
     * @param restartKey replays the level once a round has ended
     * @param menuKey    returns to the setup screen once a round has ended
     * @param abortKey   abandons a round that is still in progress and returns to the setup
     *                   screen; also works on the round-over screen
     */
    public GameController(GameModel model, GameView view, InputSource input,
                          KeyBindings player1Bindings, KeyBindings player2Bindings,
                          int restartKey, int menuKey, int abortKey) {
        this.model = model;
        this.view = view;
        this.input = input;
        this.player1Bindings = player1Bindings;
        this.player2Bindings = player2Bindings;
        this.restartKey = restartKey;
        this.menuKey = menuKey;
        this.abortKey = abortKey;
        this.aiProvider = new AiActionProvider(new AllyTankAi(new Random()));
        this.timer = new Timer(1000 / FPS, e -> tick());
        bindProviders();
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    void tick() {
        long now = System.currentTimeMillis();
        switch (model.getPhase()) {
            case MENU -> handleMenu();
            case PLAYING -> handlePlaying(now);
            case ROUND_OVER -> handleRoundOver();
        }
        view.refresh();
    }

    private void handlePlaying(long now) {
        // Checked before the tick runs, so the abandoned round doesn't advance one last frame.
        if (input.consumePress(abortKey)) {
            model.returnToMenu();
            return;
        }
        model.update(
                actionFor(player1Provider, model.getPlayer1(), now),
                actionFor(player2Provider, model.getPlayer2(), now),
                now);
    }

    /**
     * Menu keys are edge-triggered, and both players' bindings drive it, so whoever is holding
     * the keyboard can set the round up.
     */
    private void handleMenu() {
        GameSetup setup = model.getSetup();
        for (KeyBindings bindings : List.of(player1Bindings, player2Bindings)) {
            if (input.consumePress(bindings.up())) {
                setup.moveCursor(-1);
            }
            if (input.consumePress(bindings.down())) {
                setup.moveCursor(1);
            }
            if (input.consumePress(bindings.left())) {
                setup.adjust(-1);
            }
            if (input.consumePress(bindings.right())) {
                setup.adjust(1);
            }
            if (input.consumePress(bindings.fire())) {
                startRound();
                return;
            }
        }
    }

    private void handleRoundOver() {
        if (input.consumePress(restartKey)) {
            startRound();
        } else if (input.consumePress(menuKey) || input.consumePress(abortKey)) {
            model.returnToMenu();
        }
    }

    /** Starts a round and binds each player slot to a keyboard or the AI, per the setup. */
    private void startRound() {
        model.startRound();
        bindProviders();
    }

    private void bindProviders() {
        GameSetup setup = model.getSetup();
        player1Provider = providerFor(setup.getControlType(Player.ONE), player1Bindings);
        player2Provider = providerFor(setup.getControlType(Player.TWO), player2Bindings);
    }

    private ActionProvider providerFor(ControlType type, KeyBindings bindings) {
        return type == ControlType.AI ? aiProvider : new HumanActionProvider(input, bindings);
    }

    private PlayerAction actionFor(ActionProvider provider, Tank tank, long now) {
        if (provider == null || tank == null || !tank.isAlive()) {
            return PlayerAction.NONE;
        }
        return provider.actionFor(model, tank, now);
    }
}
