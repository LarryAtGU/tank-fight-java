package tankfight.model;

/**
 * A piece of terrain: either a segment of the indestructible field border, or one destructible
 * brick tile from a {@link Maze}.
 *
 * <p>Sides never matter to a wall. Brick takes {@link Bullet} damage from allies and enemies
 * alike — either side can open a route the other then drives through — while the border is
 * immune to everything. Damage accumulates on a tile, so two hits destroy it however far apart
 * they arrive.
 */
public class Wall extends Entity implements Damageable {

    /** Brick is laid out on a fixed grid; one tile is this many pixels square. */
    public static final int BRICK_SIZE = 20;
    /** A fresh tile's hit points, in the same units as {@link Bullet#getDamage()}. */
    public static final int BRICK_HIT_POINTS = 40;
    /** Thickness of every border segment. */
    public static final int BORDER_THICKNESS = 10;

    private final boolean destructible;
    private int health;

    private Wall(int x, int y, int width, int height, boolean destructible, int health) {
        super(x, y, width, height);
        this.destructible = destructible;
        this.health = health;
    }

    /** A segment of the field border. Immune to gunfire and never removed from the field. */
    public static Wall border(int x, int y, int width, int height) {
        return new Wall(x, y, width, height, false, 0);
    }

    /** One full-health brick tile with its top-left corner at {@code (x, y)}. */
    public static Wall brick(int x, int y) {
        return new Wall(x, y, BRICK_SIZE, BRICK_SIZE, true, BRICK_HIT_POINTS);
    }

    /** True for brick, false for the border. Only brick can be damaged or destroyed. */
    public boolean isDestructible() {
        return destructible;
    }

    /** Hit points left on this tile; always zero for the border, which is never damaged. */
    @Override
    public int getHealth() {
        return health;
    }

    /** {@link #BRICK_HIT_POINTS} for brick; zero for the border, which has no health to track. */
    @Override
    public int getMaxHealth() {
        return destructible ? BRICK_HIT_POINTS : 0;
    }

    /** Whether this wall is still standing. The border always is. */
    @Override
    public boolean isAlive() {
        return !destructible || health > 0;
    }

    /** Wears down a brick tile. A no-op on the border. */
    @Override
    public void takeDamage(int amount) {
        if (destructible) {
            health = Math.max(0, health - amount);
        }
    }
}
