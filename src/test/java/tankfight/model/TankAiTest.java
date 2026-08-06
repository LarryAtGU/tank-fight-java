package tankfight.model;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TankAiTest {

    private static final long START = 1_000L;
    private static final RoundConfig NO_SPAWNS = new RoundConfig(1, 5, 3, Long.MAX_VALUE);
    private static final TankAi INERT = (model, tank, now) -> PlayerAction.NONE;

    private static GameModel emptyRound() {
        GameModel model = new GameModel(new Random(1), INERT);
        model.startRound(NO_SPAWNS);
        return model;
    }

    /** A round holding exactly one motionless enemy, parked where the test wants it. */
    private static GameModel roundWithEnemyAt(int x, int y) {
        GameModel model = new GameModel(new Random(1), INERT);
        model.startRound(new RoundConfig(1, 1, 1, 0));
        long now = START;
        while (model.getEnemies().isEmpty()) {
            model.update(PlayerAction.NONE, PlayerAction.NONE, now);
            now += 16;
        }
        model.getEnemies().get(0).setPosition(x, y);
        return model;
    }

    // --- RandomTankAi -------------------------------------------------------------------------

    @Test
    void randomAiAlwaysPicksADirectionItCouldActuallyDriveIn() {
        GameModel model = roundWithEnemyAt(400, 300);
        Tank enemy = model.getEnemies().get(0);
        RandomTankAi ai = new RandomTankAi(new Random(99));

        for (int i = 0; i < 500; i++) {
            PlayerAction action = ai.decide(model, enemy, START);
            assertNotNull(action.moveDirection(), "the enemy AI always commits to a heading");
            assertTrue(model.canMove(enemy, action.moveDirection())
                            || action.moveDirection() == enemy.getDirection(),
                    "picked a blocked direction it wasn't already committed to");
        }
    }

    @Test
    void randomAiTurnsWhenItsCurrentHeadingIsBlocked() {
        GameModel model = roundWithEnemyAt(GameModel.SPAWN_XS[1], 12);
        Tank enemy = model.getEnemies().get(0);
        enemy.setDirection(Direction.UP);   // pinned against the top border wall
        RandomTankAi ai = new RandomTankAi(new Random(4));

        PlayerAction action = ai.decide(model, enemy, START);

        assertTrue(model.canMove(enemy, action.moveDirection()),
                "a blocked enemy must pick a heading that is actually open");
    }

    @Test
    void randomAiDoesBothWanderAndShootOverTime() {
        GameModel model = roundWithEnemyAt(400, 300);
        Tank enemy = model.getEnemies().get(0);
        RandomTankAi ai = new RandomTankAi(new Random(2024));

        int shots = 0;
        for (int i = 0; i < 1000; i++) {
            if (ai.decide(model, enemy, START).fire()) {
                shots++;
            }
        }

        assertTrue(shots > 0, "enemies are supposed to shoot at random");
        assertTrue(shots < 1000, "enemies are not supposed to hold the trigger down");
    }

    @Test
    void randomAiIsReproducibleForAGivenSeed() {
        GameModel model = roundWithEnemyAt(400, 300);
        Tank enemy = model.getEnemies().get(0);

        PlayerAction first = new RandomTankAi(new Random(7)).decide(model, enemy, START);
        PlayerAction second = new RandomTankAi(new Random(7)).decide(model, enemy, START);

        assertEquals(first, second);
    }

    // --- AllyTankAi ---------------------------------------------------------------------------

    @Test
    void allyAiFiresWhenAnEnemyIsLinedUpAhead() {
        GameModel model = roundWithEnemyAt(400, 200);
        Tank p1 = model.getPlayer1();
        p1.setPosition(400, 500);

        PlayerAction action = new AllyTankAi(new Random(1)).decide(model, p1, START);

        assertEquals(Direction.UP, action.moveDirection(), "it should turn onto the target");
        assertTrue(action.fire());
    }

    @Test
    void allyAiClosesTheDistanceWithoutWastingShotsWhenNotLinedUp() {
        GameModel model = roundWithEnemyAt(100, 100);
        Tank p1 = model.getPlayer1();
        p1.setPosition(500, 450);

        PlayerAction action = new AllyTankAi(new Random(1)).decide(model, p1, START);

        assertFalse(action.fire(), "no point shooting at an enemy that isn't in front of it");
        assertEquals(Direction.LEFT, action.moveDirection(),
                "the horizontal gap is the larger one, so close that first");
    }

    @Test
    void allyAiKeepsMovingWhenThereIsNothingToShootAt() {
        GameModel model = emptyRound();
        Tank p1 = model.getPlayer1();

        PlayerAction action = new AllyTankAi(new Random(1)).decide(model, p1, START);

        assertNotNull(action.moveDirection());
        assertFalse(action.fire());
    }

    @Test
    void allyAiNeverTargetsItsTeammate() {
        GameModel model = emptyRound();
        Tank p1 = model.getPlayer1();
        Tank p2 = model.getPlayer2();
        p1.setPosition(400, 500);
        p2.setPosition(400, 300);   // perfectly lined up, but on the same side

        for (int i = 0; i < 50; i++) {
            assertFalse(new AllyTankAi(new Random(i)).decide(model, p1, START).fire(),
                    "an AI player must never open fire on its ally");
        }
    }
}
