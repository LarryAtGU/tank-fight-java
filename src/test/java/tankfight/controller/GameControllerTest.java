package tankfight.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tankfight.model.ControlType;
import tankfight.model.GameModel;
import tankfight.model.GameSetup;
import tankfight.model.Phase;
import tankfight.model.Player;
import tankfight.model.RoundConfig;
import tankfight.view.GameView;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameControllerTest {

    private static final KeyBindings PLAYER1_BINDINGS =
            new KeyBindings(KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);
    private static final KeyBindings PLAYER2_BINDINGS =
            new KeyBindings(KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
    private static final int RESTART_KEY = KeyEvent.VK_R;
    private static final int MENU_KEY = KeyEvent.VK_M;
    private static final int ABORT_KEY = KeyEvent.VK_ESCAPE;

    private GameModel model;
    private FakeInputSource input;
    private FakeGameView view;
    private GameController controller;

    @BeforeEach
    void setUp() {
        model = new GameModel(new Random(42));
        input = new FakeInputSource();
        view = new FakeGameView();
        controller = new GameController(model, view, input, PLAYER1_BINDINGS, PLAYER2_BINDINGS,
                RESTART_KEY, MENU_KEY, ABORT_KEY);
    }

    /** Presses a key, ticks once so the controller sees it, then releases it. */
    private void tap(int keyCode) {
        input.press(keyCode);
        controller.tick();
        input.release(keyCode);
    }

    private void startRoundFromMenu() {
        tap(PLAYER1_BINDINGS.fire());
    }

    // --- menu ---------------------------------------------------------------------------------

    @Test
    void tickWithNoKeysPressedRefreshesViewExactlyOnceAndDoesNotThrow() {
        assertDoesNotThrow(() -> controller.tick());
        assertEquals(1, view.refreshCount);
    }

    @Test
    void theGameOpensOnTheMenu() {
        controller.tick();

        assertEquals(Phase.MENU, model.getPhase());
    }

    @Test
    void menuNavigationIsEdgeTriggeredSoAHeldKeyOnlyMovesOnce() {
        input.press(PLAYER1_BINDINGS.down());
        controller.tick();
        controller.tick();
        controller.tick();

        assertEquals(GameSetup.ROW_PLAYER_ONE, model.getSetup().getSelectedRow(),
                "holding the key must not race through every row");
    }

    @Test
    void eitherPlayersKeysCanDriveTheMenu() {
        tap(PLAYER2_BINDINGS.down());

        assertEquals(GameSetup.ROW_PLAYER_ONE, model.getSetup().getSelectedRow());
    }

    @Test
    void theMenuCanSwitchToASinglePlayerRound() {
        tap(PLAYER1_BINDINGS.right());   // the player-count row is selected first
        startRoundFromMenu();

        assertEquals(1, model.getSetup().getPlayerCount());
        assertEquals(Phase.PLAYING, model.getPhase());
        assertNull(model.getPlayer2());
    }

    @Test
    void theMenuCanRaiseTheLevelWhichMakesTheRoundHarder() {
        tap(PLAYER1_BINDINGS.up());      // wraps up onto the level row
        tap(PLAYER1_BINDINGS.right());
        startRoundFromMenu();

        assertEquals(RoundConfig.MIN_LEVEL + 1, model.getSetup().getLevel());
        assertTrue(model.getConfig().totalEnemies() > RoundConfig.MIN_TOTAL_ENEMIES);
    }

    @Test
    void confirmingTheMenuStartsTheRound() {
        startRoundFromMenu();

        assertEquals(Phase.PLAYING, model.getPhase());
        assertEquals(2, model.getAllies().size());
    }

    // --- playing ------------------------------------------------------------------------------

    @Test
    void tickMovesPlayerOneWhenUpKeyIsPressed() {
        startRoundFromMenu();
        int startY = model.getPlayer1().getY();

        input.press(KeyEvent.VK_W);
        controller.tick();

        assertNotEquals(startY, model.getPlayer1().getY());
    }

    @Test
    void aHumanSlotIgnoresTheOtherPlayersKeys() {
        startRoundFromMenu();
        int startY = model.getPlayer2().getY();

        input.press(PLAYER1_BINDINGS.up());
        controller.tick();

        assertEquals(startY, model.getPlayer2().getY());
    }

    @Test
    void anAiSlotDrivesItselfWithNoKeysHeld() {
        model.getSetup().setControlType(Player.TWO, ControlType.AI);
        startRoundFromMenu();
        int startX = model.getPlayer2().getX();
        int startY = model.getPlayer2().getY();

        for (int i = 0; i < 30; i++) {
            controller.tick();
        }

        assertTrue(model.getPlayer2().getX() != startX || model.getPlayer2().getY() != startY,
                "a computer-driven player should move without any key being pressed");
    }

    @Test
    void bothSlotsCanBeAiAtOnce() {
        model.getSetup().setControlType(Player.ONE, ControlType.AI);
        model.getSetup().setControlType(Player.TWO, ControlType.AI);
        startRoundFromMenu();

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 120; i++) {
                controller.tick();
            }
        });
        assertEquals(Phase.PLAYING, model.getPhase());
    }

    @Test
    void aSinglePlayerRoundTicksWithoutTrippingOverTheAbsentSecondSlot() {
        model.getSetup().setPlayerCount(1);
        startRoundFromMenu();

        assertDoesNotThrow(() -> {
            input.press(PLAYER2_BINDINGS.up());
            for (int i = 0; i < 30; i++) {
                controller.tick();
            }
        });
        assertEquals(Phase.PLAYING, model.getPhase());
    }

    @Test
    void everyTickRefreshesTheViewExactlyOnce() {
        startRoundFromMenu();
        int before = view.refreshCount;

        controller.tick();
        controller.tick();

        assertEquals(before + 2, view.refreshCount);
    }

    @Test
    void theAbortKeyEndsARoundInProgressAndReturnsToTheMenu() {
        startRoundFromMenu();
        for (int i = 0; i < 10; i++) {
            controller.tick();
        }

        tap(ABORT_KEY);

        assertEquals(Phase.MENU, model.getPhase());
    }

    @Test
    void abortingARoundLeavesTheSetupIntactSoTheNextRoundCanBeStartedStraightAway() {
        model.getSetup().setPlayerCount(1);
        model.getSetup().setControlType(Player.ONE, ControlType.AI);
        model.getSetup().setLevel(RoundConfig.MIN_LEVEL + 2);
        startRoundFromMenu();
        tap(ABORT_KEY);

        startRoundFromMenu();

        assertEquals(Phase.PLAYING, model.getPhase());
        assertEquals(1, model.getSetup().getPlayerCount());
        assertEquals(ControlType.AI, model.getSetup().getControlType(Player.ONE));
        assertEquals(RoundConfig.forLevel(RoundConfig.MIN_LEVEL + 2), model.getConfig());
    }

    @Test
    void anAbortedRoundDoesNotAdvanceTheSimulationOnItsWayOut() {
        startRoundFromMenu();
        int startY = model.getPlayer1().getY();

        input.press(PLAYER1_BINDINGS.up());
        tap(ABORT_KEY);

        assertEquals(Phase.MENU, model.getPhase());
        assertEquals(startY, model.getPlayer1().getY(),
                "the abandoned round must not run one last frame");
    }

    @Test
    void theAbortKeyIsEdgeTriggeredSoHoldingItDoesNotSwallowTheNextRound() {
        startRoundFromMenu();
        input.press(ABORT_KEY);
        controller.tick();
        assertEquals(Phase.MENU, model.getPhase());

        // Esc is still physically held while the player confirms the menu again.
        startRoundFromMenu();
        controller.tick();

        assertEquals(Phase.PLAYING, model.getPhase());
    }

    // --- round over ---------------------------------------------------------------------------

    @Test
    void theRestartKeyReplaysTheRoundAfterADefeat() {
        startRoundFromMenu();
        loseTheRound();
        assertTrue(model.isGameOver());

        tap(RESTART_KEY);

        assertEquals(Phase.PLAYING, model.getPhase());
        assertNull(model.getOutcome());
        assertTrue(model.getPlayer1().isAlive());
        assertTrue(model.getPlayer2().isAlive());
    }

    @Test
    void theMenuKeyGoesBackToTheSetupScreenAfterADefeat() {
        startRoundFromMenu();
        loseTheRound();

        tap(MENU_KEY);

        assertEquals(Phase.MENU, model.getPhase());
    }

    @Test
    void theAbortKeyAlsoLeavesTheRoundOverScreen() {
        startRoundFromMenu();
        loseTheRound();

        tap(ABORT_KEY);

        assertEquals(Phase.MENU, model.getPhase());
    }

    @Test
    void aFinishedRoundStaysFinishedUntilAKeyIsPressed() {
        startRoundFromMenu();
        loseTheRound();

        for (int i = 0; i < 10; i++) {
            controller.tick();
        }

        assertEquals(Phase.ROUND_OVER, model.getPhase());
    }

    @Test
    void switchingASlotToAiTakesEffectOnTheNextRoundNotTheCurrentOne() {
        startRoundFromMenu();
        loseTheRound();
        model.getSetup().setControlType(Player.ONE, ControlType.AI);

        tap(RESTART_KEY);
        int startX = model.getPlayer1().getX();
        int startY = model.getPlayer1().getY();
        for (int i = 0; i < 30; i++) {
            controller.tick();
        }

        assertFalse(model.getPlayer1().getX() == startX && model.getPlayer1().getY() == startY,
                "restarting should re-read the setup and hand player one to the AI");
    }

    /** Destroys every ally so the round ends in defeat on the next tick. */
    private void loseTheRound() {
        model.getPlayer1().takeDamage(model.getPlayer1().getMaxHealth());
        if (model.getPlayer2() != null) {
            model.getPlayer2().takeDamage(model.getPlayer2().getMaxHealth());
        }
        controller.tick();
    }

    private static class FakeInputSource implements InputSource {
        private final Set<Integer> pressed = new HashSet<>();
        private final Set<Integer> fresh = new HashSet<>();

        void press(int keyCode) {
            if (pressed.add(keyCode)) {
                fresh.add(keyCode);
            }
        }

        void release(int keyCode) {
            pressed.remove(keyCode);
        }

        @Override
        public boolean isPressed(int keyCode) {
            return pressed.contains(keyCode);
        }

        @Override
        public boolean consumePress(int keyCode) {
            return fresh.remove(keyCode);
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
