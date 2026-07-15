package tankfight.entities;

import tankfight.util.Direction;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Tank {
    public static final int SIZE = 40;
    private static final int SPEED = 3;
    private static final int MAX_HEALTH = 100;
    private static final long FIRE_COOLDOWN_MS = 400;

    private int x;
    private int y;
    private Direction direction;
    private final Color color;
    private int health = MAX_HEALTH;
    private long lastFireTime = 0;

    public Tank(int x, int y, Direction direction, Color color) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.color = color;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, SIZE, SIZE);
    }

    public Rectangle getBoundsAt(int nx, int ny) {
        return new Rectangle(nx, ny, SIZE, SIZE);
    }

    public int getSpeed() {
        return SPEED;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - amount);
    }

    public boolean canFire(long now) {
        return now - lastFireTime >= FIRE_COOLDOWN_MS;
    }

    public Bullet fire(long now) {
        lastFireTime = now;
        int centerX = x + SIZE / 2 - Bullet.SIZE / 2;
        int centerY = y + SIZE / 2 - Bullet.SIZE / 2;
        int bx = centerX + direction.dx() * (SIZE / 2);
        int by = centerY + direction.dy() * (SIZE / 2);
        return new Bullet(bx, by, direction, this);
    }

    public void draw(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int cx = x + SIZE / 2;
        int cy = y + SIZE / 2;
        double angle = switch (direction) {
            case UP -> 0;
            case RIGHT -> Math.PI / 2;
            case DOWN -> Math.PI;
            case LEFT -> -Math.PI / 2;
        };
        g2.rotate(angle, cx, cy);

        g2.setColor(color.darker());
        g2.fillRect(cx - 4, y - 8, 8, SIZE / 2 + 8);

        g2.setColor(color);
        g2.fillRoundRect(x, y, SIZE, SIZE, 8, 8);
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x, y, SIZE, SIZE, 8, 8);

        g2.dispose();
    }
}
