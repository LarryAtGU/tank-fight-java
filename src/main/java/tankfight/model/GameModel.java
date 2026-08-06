package tankfight.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The whole simulation: one or two allied player tanks against a round's worth of AI enemies.
 *
 * <p>Enemies trickle in from three entry points along the top edge, capped both by a per-round
 * total and by how many may be alive at once (see {@link RoundConfig}). Allies score by
 * destroying them. Friendly fire is impossible in either direction — a bullet only damages tanks
 * whose {@link Side} differs from its own — so the only way to lose is to let the enemy side
 * destroy every ally.
 */
public class GameModel {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    /** Points awarded to the firing player for each enemy tank destroyed. */
    public static final int POINTS_PER_KILL = 100;

    /** X coordinates of the three enemy entry points: top-left, top-middle, top-right. */
    public static final int[] SPAWN_XS = {20, WIDTH / 2 - Tank.SIZE / 2, WIDTH - 20 - Tank.SIZE};
    /** Y coordinate shared by all three enemy entry points. */
    public static final int SPAWN_Y = 20;

    private static final int ALLY_Y = HEIGHT - 20 - Tank.SIZE;
    private static final int ALLY_ONE_X = WIDTH / 2 - 120;
    private static final int ALLY_TWO_X = WIDTH / 2 + 80;
    private static final int SOLO_ALLY_X = WIDTH / 2 - Tank.SIZE / 2;

    private final List<Wall> walls = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Tank> allies = new ArrayList<>();
    private final List<Tank> enemies = new ArrayList<>();
    private final Map<Player, Integer> scores = new EnumMap<>(Player.class);

    private final GameSetup setup = new GameSetup();
    private final Random random;
    private final TankAi enemyAi;

    private RoundConfig config = RoundConfig.forLevel(RoundConfig.MIN_LEVEL);
    private Phase phase = Phase.MENU;
    private RoundOutcome outcome;
    private int enemiesSpawned;
    private int enemiesDestroyed;
    private long roundStartTime;
    private long lastSpawnTime;

    public GameModel() {
        this(new Random());
    }

    /** Seeded constructor: makes enemy spawning and enemy AI reproducible for tests. */
    public GameModel(Random random) {
        this(random, new RandomTankAi(random));
    }

    /** Lets a caller swap the behaviour driving every enemy tank. */
    public GameModel(Random random, TankAi enemyAi) {
        this.random = random;
        this.enemyAi = enemyAi;
        initWalls();
    }

    private void initWalls() {
        walls.add(new Wall(0, 0, WIDTH, 10));
        walls.add(new Wall(0, HEIGHT - 10, WIDTH, 10));
        walls.add(new Wall(0, 0, 10, HEIGHT));
        walls.add(new Wall(WIDTH - 10, 0, 10, HEIGHT));

        walls.add(new Wall(WIDTH / 2 - 60, HEIGHT / 2 - 10, 120, 20));
        walls.add(new Wall(200, 150, 20, 150));
        walls.add(new Wall(WIDTH - 220, HEIGHT - 300, 20, 150));
    }

    /**
     * Builds a fresh round from the current {@link #getSetup()} and switches to
     * {@link Phase#PLAYING}. This is also what {@link #reset()} does, so "replay" and "start"
     * are the same operation.
     */
    public void startRound() {
        startRound(RoundConfig.forLevel(setup.getLevel()));
    }

    /**
     * Starts a round with a hand-supplied difficulty instead of the one implied by the setup's
     * level, for callers that want to shape a round directly.
     */
    public void startRound(RoundConfig config) {
        this.config = config;

        bullets.clear();
        allies.clear();
        enemies.clear();
        scores.clear();
        enemiesSpawned = 0;
        enemiesDestroyed = 0;
        roundStartTime = 0;
        lastSpawnTime = 0;
        outcome = null;

        if (setup.getPlayerCount() == 1) {
            allies.add(Tank.ally(Player.ONE, SOLO_ALLY_X, ALLY_Y, Direction.UP));
            scores.put(Player.ONE, 0);
        } else {
            allies.add(Tank.ally(Player.ONE, ALLY_ONE_X, ALLY_Y, Direction.UP));
            allies.add(Tank.ally(Player.TWO, ALLY_TWO_X, ALLY_Y, Direction.UP));
            scores.put(Player.ONE, 0);
            scores.put(Player.TWO, 0);
        }

        phase = Phase.PLAYING;
    }

    /** Restarts the round with the same setup. */
    public void reset() {
        startRound();
    }

    /** Abandons the current round and returns to the setup menu. */
    public void returnToMenu() {
        phase = Phase.MENU;
    }

    /**
     * Advances the simulation one tick. The two actions drive the ally tanks; enemies are driven
     * by the model's own AI. Actions for an absent or destroyed ally are ignored, so a caller may
     * always pass two.
     */
    public void update(PlayerAction player1Action, PlayerAction player2Action, long now) {
        if (phase != Phase.PLAYING) {
            return;
        }
        if (roundStartTime == 0) {
            // The clock only starts on the first tick, so allies get one spawn interval of grace
            // before the first enemy arrives.
            roundStartTime = now;
            lastSpawnTime = now;
        }

        updateBullets();
        applyAction(getTank(Player.ONE), player1Action, now);
        applyAction(getTank(Player.TWO), player2Action, now);
        for (Tank enemy : List.copyOf(enemies)) {
            if (enemy.isAlive()) {
                applyAction(enemy, enemyAi.decide(this, enemy, now), now);
            }
        }
        removeDestroyedEnemies();
        spawnEnemies(now);
        checkRoundOver();
    }

    private void applyAction(Tank tank, PlayerAction action, long now) {
        if (tank == null || action == null || !tank.isAlive()) {
            return;
        }
        if (action.moveDirection() != null) {
            tank.setDirection(action.moveDirection());
            tryMove(tank, action.moveDirection());
        }
        if (action.fire() && tank.canFire(now)) {
            bullets.add(tank.fire(now));
        }
    }

    private void tryMove(Tank tank, Direction direction) {
        if (canMove(tank, direction)) {
            tank.setPosition(tank.getX() + direction.dx() * tank.getSpeed(),
                    tank.getY() + direction.dy() * tank.getSpeed());
        }
    }

    /**
     * Whether {@code tank} could take one step in {@code direction} without leaving the field or
     * running into a wall or another living tank. Public so {@link TankAi} implementations can
     * steer without duplicating collision rules.
     */
    public boolean canMove(Tank tank, Direction direction) {
        int nx = tank.getX() + direction.dx() * tank.getSpeed();
        int ny = tank.getY() + direction.dy() * tank.getSpeed();
        return isFree(tank.getBoundsAt(nx, ny), tank);
    }

    /** True when {@code bounds} is inside the field and clear of walls and living tanks. */
    private boolean isFree(Rectangle bounds, Tank ignored) {
        if (bounds.x < 0 || bounds.y < 0
                || bounds.x + bounds.width > WIDTH || bounds.y + bounds.height > HEIGHT) {
            return false;
        }
        for (Wall wall : walls) {
            if (bounds.intersects(wall.getBounds())) {
                return false;
            }
        }
        return !hitsLivingTank(bounds, ignored, allies) && !hitsLivingTank(bounds, ignored, enemies);
    }

    private static boolean hitsLivingTank(Rectangle bounds, Tank ignored, List<Tank> tanks) {
        for (Tank tank : tanks) {
            if (tank != ignored && tank.isAlive() && bounds.intersects(tank.getBounds())) {
                return true;
            }
        }
        return false;
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.advance();
            Rectangle bounds = bullet.getBounds();

            if (bounds.x < 0 || bounds.y < 0 || bounds.x > WIDTH || bounds.y > HEIGHT) {
                it.remove();
                continue;
            }

            if (hitsWall(bounds)) {
                it.remove();
                continue;
            }

            Tank target = opposingTankAt(bounds, bullet.getSide());
            if (target != null) {
                target.takeDamage(bullet.getDamage());
                if (!target.isAlive() && target.getSide() == Side.ENEMIES && bullet.getOwner() != null) {
                    scores.merge(bullet.getOwner(), POINTS_PER_KILL, Integer::sum);
                }
                it.remove();
            }
        }
    }

    private boolean hitsWall(Rectangle bounds) {
        for (Wall wall : walls) {
            if (bounds.intersects(wall.getBounds())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The first living tank at {@code bounds} belonging to the side opposite {@code shooter}.
     * Same-side tanks are skipped entirely, so friendly bullets pass through teammates rather
     * than being absorbed by them.
     */
    private Tank opposingTankAt(Rectangle bounds, Side shooter) {
        for (Tank tank : allies) {
            if (tank.getSide() != shooter && tank.isAlive() && bounds.intersects(tank.getBounds())) {
                return tank;
            }
        }
        for (Tank tank : enemies) {
            if (tank.getSide() != shooter && tank.isAlive() && bounds.intersects(tank.getBounds())) {
                return tank;
            }
        }
        return null;
    }

    private void spawnEnemies(long now) {
        if (enemiesSpawned >= config.totalEnemies() || enemies.size() >= config.maxConcurrentEnemies()) {
            return;
        }
        if (now - lastSpawnTime < config.spawnIntervalMs()) {
            return;
        }

        for (int index : shuffledSpawnOrder()) {
            Rectangle spot = new Rectangle(SPAWN_XS[index], SPAWN_Y, Tank.SIZE, Tank.SIZE);
            if (isFree(spot, null)) {
                enemies.add(Tank.enemy(SPAWN_XS[index], SPAWN_Y, Direction.DOWN));
                enemiesSpawned++;
                lastSpawnTime = now;
                return;
            }
        }
        // Every entry point is blocked; leave lastSpawnTime alone and retry next tick.
    }

    private List<Integer> shuffledSpawnOrder() {
        List<Integer> order = new ArrayList<>(SPAWN_XS.length);
        for (int i = 0; i < SPAWN_XS.length; i++) {
            order.add(i);
        }
        Collections.shuffle(order, random);
        return order;
    }

    /**
     * Destroyed enemies leave the field; destroyed allies stay in the list with zero health so
     * the HUD can keep showing their slot and score for the rest of the round.
     */
    private void removeDestroyedEnemies() {
        Iterator<Tank> it = enemies.iterator();
        while (it.hasNext()) {
            if (!it.next().isAlive()) {
                it.remove();
                enemiesDestroyed++;
            }
        }
    }

    private void checkRoundOver() {
        boolean anyAllyAlive = false;
        for (Tank ally : allies) {
            if (ally.isAlive()) {
                anyAllyAlive = true;
                break;
            }
        }
        if (!anyAllyAlive) {
            phase = Phase.ROUND_OVER;
            outcome = RoundOutcome.DEFEAT;
        } else if (enemiesDestroyed >= config.totalEnemies()) {
            phase = Phase.ROUND_OVER;
            outcome = RoundOutcome.VICTORY;
        }
    }

    public GameSetup getSetup() {
        return setup;
    }

    public Phase getPhase() {
        return phase;
    }

    public RoundConfig getConfig() {
        return config;
    }

    /** The tank in {@code player}'s slot, or {@code null} if that slot isn't in this round. */
    public Tank getTank(Player player) {
        for (Tank ally : allies) {
            if (ally.getPlayer() == player) {
                return ally;
            }
        }
        return null;
    }

    public Tank getPlayer1() {
        return getTank(Player.ONE);
    }

    /** The second player's tank, or {@code null} in a one-player round. */
    public Tank getPlayer2() {
        return getTank(Player.TWO);
    }

    /** Both ally tanks in slot order; destroyed allies remain in the list. */
    public List<Tank> getAllies() {
        return Collections.unmodifiableList(allies);
    }

    /** The enemy tanks currently on the field; destroyed ones are removed. */
    public List<Tank> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public List<Wall> getWalls() {
        return Collections.unmodifiableList(walls);
    }

    public List<Bullet> getBullets() {
        return Collections.unmodifiableList(bullets);
    }

    public int getScore(Player player) {
        return scores.getOrDefault(player, 0);
    }

    public int getTeamScore() {
        int total = 0;
        for (int score : scores.values()) {
            total += score;
        }
        return total;
    }

    public int getEnemiesDestroyed() {
        return enemiesDestroyed;
    }

    /** Enemy tanks in this round not yet destroyed, whether already spawned or still to come. */
    public int getEnemiesRemaining() {
        return config.totalEnemies() - enemiesDestroyed;
    }

    public boolean isGameOver() {
        return phase == Phase.ROUND_OVER;
    }

    /** How the round ended, or {@code null} while it is still being played. */
    public RoundOutcome getOutcome() {
        return outcome;
    }
}
