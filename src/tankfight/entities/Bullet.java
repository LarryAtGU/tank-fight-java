package tankfight.entities;

import tankfight.util.Direction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Bullet {
    public static final int SIZE = 8;
    private static final int SPEED = 7;
    private static final int DAMAGE = 20;

    private int x;
    private int y;
    private final Direction direction;
    private final Tank owner;

    public Bullet(int x, int y, Direction direction, Tank owner) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.owner = owner;
    }

    public void update() {
        x += direction.dx() * SPEED;
        y += direction.dy() * SPEED;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, SIZE, SIZE);
    }

    public Tank getOwner() {
        return owner;
    }

    public int getDamage() {
        return DAMAGE;
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fillOval(x, y, SIZE, SIZE);
    }
}
