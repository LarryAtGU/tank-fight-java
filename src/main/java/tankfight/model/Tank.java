package tankfight.model;

public class Tank extends Entity implements Movable, Damageable {
    public static final int SIZE = 40;
    private static final int SPEED = 3;
    private static final int MAX_HEALTH = 100;
    private static final long FIRE_COOLDOWN_MS = 400;

    private final Player player;
    private Direction direction;
    private int health = MAX_HEALTH;
    private long lastFireTime = 0;

    public Tank(Player player, int x, int y, Direction direction) {
        super(x, y, SIZE, SIZE);
        this.player = player;
        this.direction = direction;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public int getSpeed() {
        return SPEED;
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    @Override
    public boolean isAlive() {
        return health > 0;
    }

    @Override
    public void takeDamage(int amount) {
        health = Math.max(0, health - amount);
    }

    public boolean canFire(long now) {
        return now - lastFireTime >= FIRE_COOLDOWN_MS;
    }

    public Bullet fire(long now) {
        lastFireTime = now;
        int bx = x + SIZE / 2 - Bullet.SIZE / 2 + direction.dx() * (SIZE / 2);
        int by = y + SIZE / 2 - Bullet.SIZE / 2 + direction.dy() * (SIZE / 2);
        return new Bullet(player, bx, by, direction);
    }
}
