package tankfight.model;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModelTest {

    private static final long START = 1_000L;
    private static final long TICK_MS = 16;

    /** A round that never spawns anything, so the ally rules can be tested in isolation. */
    private static final RoundConfig NO_SPAWNS = new RoundConfig(1, 5, 3, Long.MAX_VALUE);

    /** Enemies that hold still and never shoot, so they stay where a test puts them. */
    private static final TankAi INERT = (model, tank, now) -> PlayerAction.NONE;
    /** Enemies that drive straight down, which keeps the entry points clearing for new spawns. */
    private static final TankAi MARCH_DOWN = (model, tank, now) -> new PlayerAction(Direction.DOWN, false);
    /** Enemies that stand still and fire non-stop. */
    private static final TankAi TRIGGER_HAPPY = (model, tank, now) -> new PlayerAction(null, true);

    private static GameModel quietModel() {
        return new GameModel(new Random(42), INERT);
    }

    /** A started two-player round with no enemies at all. */
    private static GameModel startedModel() {
        GameModel model = quietModel();
        model.startRound(NO_SPAWNS);
        return model;
    }

    /** A started round that immediately fields exactly {@code total} motionless enemies. */
    private static GameModel modelWithEnemies(int total, TankAi ai) {
        GameModel model = new GameModel(new Random(42), ai);
        model.startRound(new RoundConfig(1, total, total, 0));
        return model;
    }

    /** Runs {@code ticks} updates with both players idle, returning the timestamp reached. */
    private static long idle(GameModel model, int ticks, long now) {
        for (int i = 0; i < ticks; i++) {
            model.update(PlayerAction.NONE, PlayerAction.NONE, now);
            now += TICK_MS;
        }
        return now;
    }

    /** Ticks until the field holds {@code count} enemies, then parks them at the given spots. */
    private static void placeEnemies(GameModel model, int[]... positions) {
        long now = START;
        while (model.getEnemies().size() < positions.length) {
            model.update(PlayerAction.NONE, PlayerAction.NONE, now);
            now += TICK_MS;
            if (now > START + 100_000) {
                throw new IllegalStateException("model never spawned " + positions.length + " enemies");
            }
        }
        for (int i = 0; i < positions.length; i++) {
            model.getEnemies().get(i).setPosition(positions[i][0], positions[i][1]);
        }
    }

    /** Fires repeatedly at whatever is ahead until {@code shots} shells have been sent. */
    private static long shoot(GameModel model, Direction facing, int shots) {
        long now = START;
        for (int i = 0; i < shots; i++) {
            model.update(new PlayerAction(facing, true), PlayerAction.NONE, now);
            now = idle(model, 60, now + TICK_MS) + Tank.ALLY_FIRE_COOLDOWN_MS;
        }
        return now;
    }

    // --- setup and round lifecycle ------------------------------------------------------------

    @Test
    void aNewModelSitsOnTheMenuAndIgnoresUpdates() {
        GameModel model = quietModel();

        assertEquals(Phase.MENU, model.getPhase());
        assertTrue(model.getAllies().isEmpty());

        model.update(new PlayerAction(Direction.UP, true), PlayerAction.NONE, START);

        assertTrue(model.getBullets().isEmpty(), "the menu must not run the simulation");
        assertEquals(Phase.MENU, model.getPhase());
    }

    @Test
    void aTwoPlayerRoundFieldsBothAlliesSideBySideAtTheBottom() {
        GameModel model = startedModel();

        assertEquals(Phase.PLAYING, model.getPhase());
        assertEquals(2, model.getAllies().size());
        assertNotNull(model.getPlayer1());
        assertNotNull(model.getPlayer2());
        assertEquals(Side.ALLIES, model.getPlayer1().getSide());
        assertEquals(model.getPlayer1().getY(), model.getPlayer2().getY());
        assertTrue(model.getPlayer1().getY() > GameModel.HEIGHT / 2,
                "allies defend from the bottom, opposite the enemy entry points");
        assertNull(model.getOutcome());
        assertEquals(0, model.getTeamScore());
    }

    @Test
    void aOnePlayerRoundFieldsOnlyPlayerOne() {
        GameModel model = quietModel();
        model.getSetup().setPlayerCount(1);
        model.startRound(NO_SPAWNS);

        assertEquals(1, model.getAllies().size());
        assertNotNull(model.getPlayer1());
        assertNull(model.getPlayer2(), "there is no second tank in a one-player round");

        // An action for the absent slot must simply be ignored.
        model.update(PlayerAction.NONE, new PlayerAction(Direction.UP, true), START);
        assertTrue(model.getBullets().isEmpty());
    }

    @Test
    void startingARoundClearsTheScoresAndBulletsOfThePreviousOne() {
        GameModel model = startedModel();
        model.update(new PlayerAction(null, true), PlayerAction.NONE, START);
        assertFalse(model.getBullets().isEmpty());

        model.reset();

        assertTrue(model.getBullets().isEmpty());
        assertEquals(0, model.getTeamScore());
        assertEquals(0, model.getEnemiesDestroyed());
        assertEquals(Phase.PLAYING, model.getPhase());
    }

    @Test
    void theLevelChosenInTheMenuDecidesTheRoundsDifficulty() {
        GameModel model = quietModel();
        model.getSetup().setLevel(RoundConfig.MAX_LEVEL);

        model.startRound();

        assertEquals(RoundConfig.forLevel(RoundConfig.MAX_LEVEL), model.getConfig());
        assertEquals(RoundConfig.MAX_TOTAL_ENEMIES, model.getEnemiesRemaining());
    }

    // --- movement -----------------------------------------------------------------------------

    @Test
    void updateMovesATankByItsSpeedInTheRequestedDirection() {
        GameModel model = startedModel();
        Tank p1 = model.getPlayer1();
        int startX = p1.getX();

        model.update(new PlayerAction(Direction.LEFT, false), PlayerAction.NONE, START);

        assertEquals(startX - p1.getSpeed(), p1.getX());
    }

    @Test
    void tankStopsAdvancingOnceBlockedByTheBorderWall() {
        GameModel model = startedModel();
        Tank p1 = model.getPlayer1();
        long now = START;

        for (int i = 0; i < 60; i++) {
            model.update(new PlayerAction(Direction.DOWN, false), PlayerAction.NONE, now);
            now += TICK_MS;
        }
        int blockedY = p1.getY();

        model.update(new PlayerAction(Direction.DOWN, false), PlayerAction.NONE, now);

        assertEquals(blockedY, p1.getY(),
                "tank should not be able to move further once the border wall blocks it");
    }

    @Test
    void tanksCannotDriveThroughEachOther() {
        GameModel model = startedModel();
        Tank p1 = model.getPlayer1();
        Tank p2 = model.getPlayer2();
        // Leave a gap just wide enough for one step but not two.
        p2.setPosition(p1.getX() + Tank.SIZE + 5, p1.getY());
        int startX = p1.getX();

        model.update(new PlayerAction(Direction.RIGHT, false), PlayerAction.NONE, START);
        assertEquals(startX + p1.getSpeed(), p1.getX(), "the first step should still fit");

        model.update(new PlayerAction(Direction.RIGHT, false), PlayerAction.NONE, START + TICK_MS);

        assertEquals(startX + p1.getSpeed(), p1.getX(),
                "player one should be stopped by player two's hull");
    }

    // --- firing -------------------------------------------------------------------------------

    @Test
    void firingCreatesABulletButRespectsCooldownOnImmediateReFire() {
        GameModel model = startedModel();

        model.update(new PlayerAction(null, true), PlayerAction.NONE, START);
        assertEquals(1, model.getBullets().size());

        model.update(new PlayerAction(null, true), PlayerAction.NONE, START + TICK_MS);
        assertEquals(1, model.getBullets().size(),
                "a second fire attempt inside the cooldown window must not add another bullet");
    }

    // --- friendly fire ------------------------------------------------------------------------

    @Test
    void alliesCannotDamageEachOther() {
        GameModel model = startedModel();
        Tank p1 = model.getPlayer1();
        Tank p2 = model.getPlayer2();
        // Line player two up squarely in player one's line of fire, over clear ground.
        p1.setPosition(200, 500);
        p2.setPosition(400, 500);
        p1.setDirection(Direction.RIGHT);
        int fullHealth = p2.getHealth();

        model.update(new PlayerAction(null, true), PlayerAction.NONE, START);
        idle(model, 60, START + TICK_MS);

        assertEquals(fullHealth, p2.getHealth(), "an ally bullet must never damage an ally");
        assertTrue(p2.isAlive());
    }

    @Test
    void anAllyBulletPassesThroughAnAllyRatherThanBeingAbsorbed() {
        GameModel model = modelWithEnemies(1, INERT);
        Tank p1 = model.getPlayer1();
        Tank p2 = model.getPlayer2();
        placeEnemies(model, new int[]{600, 500});
        Tank enemy = model.getEnemies().get(0);
        p1.setPosition(200, 500);
        p2.setPosition(400, 500);   // directly between player one and the enemy
        p1.setDirection(Direction.RIGHT);
        int enemyHealth = enemy.getHealth();

        model.update(new PlayerAction(null, true), PlayerAction.NONE, START);
        idle(model, 80, START + TICK_MS);

        assertTrue(enemy.getHealth() < enemyHealth,
                "a teammate standing in the way must not shield the enemy behind them");
    }

    @Test
    void enemiesCannotDamageEachOther() {
        GameModel model = modelWithEnemies(2, TRIGGER_HAPPY);
        placeEnemies(model, new int[]{200, 400}, new int[]{400, 400});
        Tank shooter = model.getEnemies().get(0);
        Tank victim = model.getEnemies().get(1);
        shooter.setDirection(Direction.RIGHT);
        int fullHealth = victim.getHealth();

        idle(model, 60, START);

        assertEquals(fullHealth, victim.getHealth(), "an enemy bullet must never damage an enemy");
        assertTrue(victim.isAlive());
    }

    // --- scoring and round outcome ------------------------------------------------------------

    @Test
    void destroyingAnEnemyCreditsTheFiringPlayerAndClearsItFromTheField() {
        GameModel model = modelWithEnemies(3, INERT);
        placeEnemies(model, new int[]{400, 400}, new int[]{100, 100}, new int[]{700, 100});
        Tank enemy = model.getEnemies().get(0);
        model.getPlayer1().setPosition(400, 500);
        model.getPlayer1().setDirection(Direction.UP);

        shoot(model, null, 4);

        assertFalse(enemy.isAlive());
        assertFalse(model.getEnemies().contains(enemy), "destroyed enemies leave the field");
        assertEquals(1, model.getEnemiesDestroyed());
        assertEquals(GameModel.POINTS_PER_KILL, model.getScore(Player.ONE));
        assertEquals(0, model.getScore(Player.TWO), "only the player who fired is credited");
        assertEquals(GameModel.POINTS_PER_KILL, model.getTeamScore());
    }

    @Test
    void theRoundIsWonOnceEveryEnemyInItHasBeenDestroyed() {
        GameModel model = modelWithEnemies(1, INERT);
        placeEnemies(model, new int[]{400, 400});
        Tank enemy = model.getEnemies().get(0);
        model.getPlayer1().setPosition(400, 500);
        model.getPlayer1().setDirection(Direction.UP);

        shoot(model, null, 4);

        assertFalse(enemy.isAlive());
        assertTrue(model.isGameOver());
        assertEquals(RoundOutcome.VICTORY, model.getOutcome());
        assertEquals(0, model.getEnemiesRemaining());
    }

    @Test
    void theRoundIsLostOnceEveryAllyIsDestroyed() {
        GameModel model = startedModel();
        model.getPlayer1().takeDamage(Tank.ALLY_HEALTH);
        model.getPlayer2().takeDamage(Tank.ALLY_HEALTH);

        model.update(PlayerAction.NONE, PlayerAction.NONE, START);

        assertTrue(model.isGameOver());
        assertEquals(RoundOutcome.DEFEAT, model.getOutcome());
    }

    @Test
    void oneSurvivingAllyKeepsTheRoundGoing() {
        GameModel model = startedModel();
        model.getPlayer1().takeDamage(Tank.ALLY_HEALTH);

        model.update(PlayerAction.NONE, new PlayerAction(Direction.UP, false), START);

        assertFalse(model.isGameOver());
        assertEquals(Phase.PLAYING, model.getPhase());
        assertSame(model.getPlayer1(), model.getAllies().get(0),
                "a destroyed ally stays in the list so the HUD can keep showing its slot");
    }

    @Test
    void aDestroyedAllyStopsRespondingToItsControls() {
        GameModel model = startedModel();
        Tank p1 = model.getPlayer1();
        p1.takeDamage(Tank.ALLY_HEALTH);
        int restingX = p1.getX();

        model.update(new PlayerAction(Direction.LEFT, true), PlayerAction.NONE, START);

        assertEquals(restingX, p1.getX());
        assertTrue(model.getBullets().isEmpty());
    }

    @Test
    void aFinishedRoundIgnoresFurtherUpdates() {
        GameModel model = startedModel();
        model.getPlayer1().takeDamage(Tank.ALLY_HEALTH);
        model.getPlayer2().takeDamage(Tank.ALLY_HEALTH);
        model.update(PlayerAction.NONE, PlayerAction.NONE, START);
        assertTrue(model.isGameOver());

        model.update(new PlayerAction(Direction.UP, true), new PlayerAction(Direction.UP, true),
                START + TICK_MS);

        assertTrue(model.getBullets().isEmpty());
    }

    // --- enemy spawning -----------------------------------------------------------------------

    @Test
    void enemiesArriveFromTheThreeTopEntryPointsOnly() {
        GameModel model = modelWithEnemies(3, INERT);

        idle(model, 40, START);

        assertFalse(model.getEnemies().isEmpty());
        for (Tank enemy : model.getEnemies()) {
            assertEquals(Side.ENEMIES, enemy.getSide());
            assertEquals(GameModel.SPAWN_Y, enemy.getY(), "enemies enter along the top edge");
            boolean atAnEntryPoint = false;
            for (int spawnX : GameModel.SPAWN_XS) {
                atAnEntryPoint |= enemy.getX() == spawnX;
            }
            assertTrue(atAnEntryPoint, "unexpected entry x: " + enemy.getX());
        }
    }

    @Test
    void allThreeEntryPointsGetUsedOverARound() {
        GameModel model = modelWithEnemies(3, INERT);

        idle(model, 40, START);

        assertEquals(3, model.getEnemies().size());
        assertEquals(3, model.getEnemies().stream().map(Tank::getX).distinct().count(),
                "with three enemies and three free entry points, each point should be used once");
    }

    @Test
    void neverMoreThanTheConfiguredNumberOfEnemiesAreOnScreenAtOnce() {
        int concurrent = 3;
        GameModel model = new GameModel(new Random(11), MARCH_DOWN);
        model.startRound(new RoundConfig(1, 40, concurrent, 0));

        long now = START;
        for (int i = 0; i < 400; i++) {
            model.update(PlayerAction.NONE, PlayerAction.NONE, now);
            assertTrue(model.getEnemies().size() <= concurrent,
                    "on-screen enemies exceeded the cap: " + model.getEnemies().size());
            now += TICK_MS;
        }
        assertEquals(concurrent, model.getEnemies().size(), "the field should be kept topped up");
    }

    @Test
    void aRoundNeverSpawnsMoreEnemiesThanItsTotal() {
        int total = 5;
        GameModel model = new GameModel(new Random(5), MARCH_DOWN);
        model.startRound(new RoundConfig(1, total, 8, 0));

        idle(model, 600, START);

        // Nothing shoots the enemies, so every tank the round ever spawned is still on the field.
        assertEquals(total, model.getEnemies().size());
    }

    @Test
    void spawningWaitsOutTheConfiguredInterval() {
        long interval = 500;
        GameModel model = new GameModel(new Random(13), INERT);
        model.startRound(new RoundConfig(1, 10, 8, interval));

        model.update(PlayerAction.NONE, PlayerAction.NONE, START);
        assertEquals(0, model.getEnemies().size(), "allies get a grace period before the first enemy");

        model.update(PlayerAction.NONE, PlayerAction.NONE, START + interval);
        assertEquals(1, model.getEnemies().size());

        model.update(PlayerAction.NONE, PlayerAction.NONE, START + interval + 1);
        assertEquals(1, model.getEnemies().size(), "the next enemy must wait a full interval");

        model.update(PlayerAction.NONE, PlayerAction.NONE, START + interval * 2);
        assertEquals(2, model.getEnemies().size());
    }
}
