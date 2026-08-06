package tankfight.model;

import java.util.Random;

/**
 * Drives a player slot that was set to {@link ControlType#AI}.
 *
 * <p>Unlike the enemy AI this one plays to win: it closes on the nearest enemy and fires once
 * roughly lined up on an axis. It never needs to check its line of fire, because ally bullets
 * pass straight through allies. With no enemy on screen it falls back to wandering.
 */
public class AllyTankAi implements TankAi {

    /** How close to an axis an enemy must be, in pixels, before it's worth shooting at. */
    private static final int ALIGN_TOLERANCE = Tank.SIZE / 2;

    private final RandomTankAi wander;

    public AllyTankAi(Random random) {
        this.wander = new RandomTankAi(random);
    }

    @Override
    public PlayerAction decide(GameModel model, Tank tank, long now) {
        Tank target = nearestEnemy(model, tank);
        if (target == null) {
            return new PlayerAction(wander.decide(model, tank, now).moveDirection(), false);
        }

        int dx = target.getX() - tank.getX();
        int dy = target.getY() - tank.getY();

        if (Math.abs(dx) <= ALIGN_TOLERANCE) {
            return new PlayerAction(dy < 0 ? Direction.UP : Direction.DOWN, true);
        }
        if (Math.abs(dy) <= ALIGN_TOLERANCE) {
            return new PlayerAction(dx < 0 ? Direction.LEFT : Direction.RIGHT, true);
        }

        // Not lined up yet: close the bigger gap first, and take the other axis when walled off.
        Direction horizontal = dx < 0 ? Direction.LEFT : Direction.RIGHT;
        Direction vertical = dy < 0 ? Direction.UP : Direction.DOWN;
        Direction primary = Math.abs(dx) >= Math.abs(dy) ? horizontal : vertical;
        Direction secondary = primary == horizontal ? vertical : horizontal;

        if (model.canMove(tank, primary)) {
            return new PlayerAction(primary, false);
        }
        if (model.canMove(tank, secondary)) {
            return new PlayerAction(secondary, false);
        }
        return new PlayerAction(wander.pickOpenDirection(model, tank, tank.getDirection()), false);
    }

    private Tank nearestEnemy(GameModel model, Tank tank) {
        Tank nearest = null;
        long best = Long.MAX_VALUE;
        for (Tank enemy : model.getEnemies()) {
            if (!enemy.isAlive() || enemy.getSide() == tank.getSide()) {
                continue;
            }
            long dx = enemy.getX() - tank.getX();
            long dy = enemy.getY() - tank.getY();
            long distance = dx * dx + dy * dy;
            if (distance < best) {
                best = distance;
                nearest = enemy;
            }
        }
        return nearest;
    }
}
