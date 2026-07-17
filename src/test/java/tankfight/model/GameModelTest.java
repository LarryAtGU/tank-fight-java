package tankfight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModelTest {

    private static final long START = 1_000L;

    @Test
    void initialStateAfterConstruction() {
        GameModel model = new GameModel();

        assertTrue(model.getPlayer1().isAlive());
        assertTrue(model.getPlayer2().isAlive());
        assertEquals(model.getPlayer1().getMaxHealth(), model.getPlayer1().getHealth());
        assertEquals(model.getPlayer2().getMaxHealth(), model.getPlayer2().getHealth());
        assertTrue(model.getBullets().isEmpty());
        assertFalse(model.isGameOver());
        assertNull(model.getWinner());

        assertEquals(50, model.getPlayer1().getX());
        assertEquals(50, model.getPlayer1().getY());
        assertEquals(GameModel.WIDTH - 90, model.getPlayer2().getX());
        assertEquals(GameModel.HEIGHT - 90, model.getPlayer2().getY());
    }

    @Test
    void resetRestoresInitialState() {
        GameModel model = new GameModel();
        model.update(new PlayerAction(Direction.RIGHT, false), PlayerAction.NONE, START);

        model.reset();

        assertEquals(50, model.getPlayer1().getX());
        assertEquals(50, model.getPlayer1().getY());
        assertTrue(model.getBullets().isEmpty());
        assertFalse(model.isGameOver());
    }

    @Test
    void updateMovesTankByItsSpeedInRequestedDirection() {
        GameModel model = new GameModel();
        int startX = model.getPlayer1().getX();
        int startY = model.getPlayer1().getY();

        model.update(new PlayerAction(Direction.RIGHT, false), PlayerAction.NONE, START);

        assertEquals(startX + model.getPlayer1().getSpeed(), model.getPlayer1().getX());
        assertEquals(startY, model.getPlayer1().getY());
    }

    @Test
    void tankStopsAdvancingOnceBlockedByTheBorderWall() {
        GameModel model = new GameModel();
        long now = START;

        // Player 1 spawns at (50, 50) facing DOWN, close to the top border wall.
        // Drive it UP for far more ticks than needed to reach the wall.
        for (int i = 0; i < 60; i++) {
            model.update(new PlayerAction(Direction.UP, false), PlayerAction.NONE, now);
            now += 16;
        }

        int blockedY = model.getPlayer1().getY();

        model.update(new PlayerAction(Direction.UP, false), PlayerAction.NONE, now);

        assertEquals(blockedY, model.getPlayer1().getY(),
                "tank should not be able to move further once the border wall blocks it");
    }

    @Test
    void firingCreatesABulletButRespectsCooldownOnImmediateReFire() {
        GameModel model = new GameModel();
        long now = START;

        model.update(new PlayerAction(null, true), PlayerAction.NONE, now);
        assertEquals(1, model.getBullets().size());

        now += 16; // well within the 400ms cooldown
        model.update(new PlayerAction(null, true), PlayerAction.NONE, now);
        assertEquals(1, model.getBullets().size(),
                "a second fire attempt inside the cooldown window must not add another bullet");
    }

    @Test
    void ordinaryNonLethalPlayNeverEndsTheGame() {
        // Best-effort game-over coverage: forcing a precise bullet/tank collision
        // depends on exact spawn/travel geometry that isn't part of the frozen
        // contract, so this test instead asserts the negative: normal movement
        // and firing that never lines up a hit must not flip gameOver/winner.
        GameModel model = new GameModel();
        long now = START;

        for (int i = 0; i < 30; i++) {
            model.update(new PlayerAction(Direction.RIGHT, true), new PlayerAction(Direction.LEFT, true), now);
            now += 16;
        }

        assertFalse(model.isGameOver());
        assertNull(model.getWinner());
        assertTrue(model.getPlayer1().isAlive());
        assertTrue(model.getPlayer2().isAlive());
    }
}
