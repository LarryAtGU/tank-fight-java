package tankfight.entities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Wall {
    private final Rectangle bounds;

    public Wall(int x, int y, int width, int height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void draw(Graphics2D g) {
        g.setColor(new Color(120, 120, 120));
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
