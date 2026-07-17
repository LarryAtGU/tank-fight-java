package tankfight.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tankfight.model.GameModel;
import tankfight.view.GameView;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GameControllerTest {

    private static final KeyBindings PLAYER1_BINDINGS =
            new KeyBindings(KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);
    private static final KeyBindings PLAYER2_BINDINGS =
            new KeyBindings(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
    private static final int RESTART_KEY = KeyEvent.VK_R;

    private GameModel model;
    private FakeInputSource input;
    private FakeGameView view;
    private GameController controller;

    @BeforeEach
    void setUp() {
        model = new GameModel();
        input = new FakeInputSource();
        view = new FakeGameView();
        controller = new GameController(model, view, input, PLAYER1_BINDINGS, PLAYER2_BINDINGS, RESTART_KEY);
    }

    @Test
    void tickWithNoKeysPressedRefreshesViewExactlyOnceAndDoesNotThrow() {
        assertDoesNotThrow(() -> controller.tick());
        assertEquals(1, view.refreshCount);
    }

    @Test
    void tickMovesPlayerOneWhenUpKeyIsPressed() {
        int startY = model.getPlayer1().getY();

        input.press(KeyEvent.VK_W);
        controller.tick();

        assertNotEquals(startY, model.getPlayer1().getY());
    }

    // A restart-key / game-over test was omitted: the frozen GameModel contract exposed to this
    // workstream (update/reset/isGameOver) gives no way to force a tank's health to zero without
    // depending on model internals (Tank/Bullet/combat specifics) owned by a different parallel
    // workstream. Driving a real game-over via update() calls would require timing/positioning
    // assumptions this fork isn't the source of truth for, so the test would be flaky by
    // construction. Leaving this to a model-layer or integration-level test instead.

    private static class FakeInputSource implements InputSource {
        private final Set<Integer> pressed = new HashSet<>();

        void press(int keyCode) {
            pressed.add(keyCode);
        }

        void release(int keyCode) {
            pressed.remove(keyCode);
        }

        @Override
        public boolean isPressed(int keyCode) {
            return pressed.contains(keyCode);
        }
    }

    private static class FakeGameView implements GameView {
        int refreshCount = 0;

        @Override
        public void refresh() {
            refreshCount++;
        }
    }
}
