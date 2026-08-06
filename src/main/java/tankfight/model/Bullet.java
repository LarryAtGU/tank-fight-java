package tankfight.model;

public class Bullet extends Entity implements Movable {
    public static final int SIZE = 8;
    private static final int SPEED = 7;
    private static final int DAMAGE = 20;

    private final Direction direction;
    private final Side side;
    private final Player owner;

    public Bullet(Side side, Player owner, int x, int y, Direction direction) {
        super(x, y, SIZE, SIZE);
        this.side = side;
        this.owner = owner;
        this.direction = direction;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public int getSpeed() {
        return SPEED;
    }

    /** The team that fired this bullet; it can only damage tanks of the other side. */
    public Side getSide() {
        return side;
    }

    /** The player credited with kills by this bullet, or {@code null} for enemy fire. */
    public Player getOwner() {
        return owner;
    }

    public int getDamage() {
        return DAMAGE;
    }

    void advance() {
        setPosition(x + direction.dx() * SPEED, y + direction.dy() * SPEED);
    }
}
