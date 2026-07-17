package tankfight.model;

import java.awt.Rectangle;

public abstract class Entity {
    protected int x;
    protected int y;
    protected final int width;
    protected final int height;

    protected Entity(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public Rectangle getBoundsAt(int nx, int ny) {
        return new Rectangle(nx, ny, width, height);
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
