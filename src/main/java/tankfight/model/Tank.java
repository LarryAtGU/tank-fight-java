package tankfight.model;

public class Tank extends Entity implements Movable, Damageable {
    public static final int SIZE = 40;

    public static final int ALLY_HEALTH = 100;
    public static final int ALLY_SPEED = 3;
    public static final long ALLY_FIRE_COOLDOWN_MS = 400;

    public static final int ENEMY_HEALTH = 40;
    public static final int ENEMY_SPEED = 2;
    public static final long ENEMY_FIRE_COOLDOWN_MS = 900;

    private final Side side;
    private final Player player;
    private final int maxHealth;
    private final int speed;
    private final long fireCooldownMs;

    private Direction direction;
    private int health;
    private long lastFireTime = 0;

    public Tank(Side side, Player player, int x, int y, Direction direction,
                int maxHealth, int speed, long fireCooldownMs) {
        super(x, y, SIZE, SIZE);
        this.side = side;
        this.player = player;
        this.direction = direction;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.fireCooldownMs = fireCooldownMs;
    }

    /** A player-controlled tank. {@code player} identifies which of the two ally slots it fills. */
    public static Tank ally(Player player, int x, int y, Direction direction) {
        return new Tank(Side.ALLIES, player, x, y, direction,
                ALLY_HEALTH, ALLY_SPEED, ALLY_FIRE_COOLDOWN_MS);
    }

    /** A tank on the enemy side. Enemies have no {@link Player} slot. */
    public static Tank enemy(int x, int y, Direction direction) {
        return new Tank(Side.ENEMIES, null, x, y, direction,
                ENEMY_HEALTH, ENEMY_SPEED, ENEMY_FIRE_COOLDOWN_MS);
    }

    public Side getSide() {
        return side;
    }

    /** The ally slot this tank fills, or {@code null} for enemy tanks. */
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
        return speed;
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
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
        return now - lastFireTime >= fireCooldownMs;
    }

    public Bullet fire(long now) {
        lastFireTime = now;
        int bx = x + SIZE / 2 - Bullet.SIZE / 2 + direction.dx() * (SIZE / 2);
        int by = y + SIZE / 2 - Bullet.SIZE / 2 + direction.dy() * (SIZE / 2);
        return new Bullet(side, player, bx, by, direction);
    }
}
