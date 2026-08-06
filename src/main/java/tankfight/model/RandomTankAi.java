package tankfight.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Drives the enemy side: wander the map and fire on a coin flip.
 *
 * <p>Deliberately stateless — the tank's own {@link Tank#getDirection()} is the "keep going"
 * memory, so a tank holds its heading until it either gets blocked or rolls a turn. That keeps
 * the AI cheap to run for every enemy, every tick, and trivial to test with a seeded
 * {@link Random}.
 */
public class RandomTankAi implements TankAi {

    private static final double TURN_CHANCE = 0.02;
    private static final double FIRE_CHANCE = 0.025;

    private final Random random;

    public RandomTankAi(Random random) {
        this.random = random;
    }

    @Override
    public PlayerAction decide(GameModel model, Tank tank, long now) {
        Direction direction = tank.getDirection();
        if (!model.canMove(tank, direction) || random.nextDouble() < TURN_CHANCE) {
            direction = pickOpenDirection(model, tank, direction);
        }
        return new PlayerAction(direction, random.nextDouble() < FIRE_CHANCE);
    }

    /** A random direction the tank could actually move in, falling back to {@code current} if boxed in. */
    Direction pickOpenDirection(GameModel model, Tank tank, Direction current) {
        List<Direction> open = new ArrayList<>(Direction.values().length);
        for (Direction candidate : Direction.values()) {
            if (model.canMove(tank, candidate)) {
                open.add(candidate);
            }
        }
        return open.isEmpty() ? current : open.get(random.nextInt(open.size()));
    }
}
