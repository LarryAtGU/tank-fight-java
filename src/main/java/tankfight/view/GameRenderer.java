package tankfight.view;

import tankfight.model.Bullet;
import tankfight.model.GameModel;
import tankfight.model.Player;
import tankfight.model.Tank;
import tankfight.model.Wall;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

class GameRenderer {
    private static final BufferedImage TANK_BLUE = loadImage("tank_blue.png");
    private static final BufferedImage TANK_RED = loadImage("tank_red.png");
    private static final BufferedImage WALL_TILE = loadImage("wall.png");
    private static final BufferedImage BULLET_IMAGE = loadImage("bullet.png");

    private static BufferedImage loadImage(String name) {
        try (InputStream in = GameRenderer.class.getResourceAsStream("/images/" + name)) {
            if (in == null) {
                throw new IOException("Missing image resource: /images/" + name);
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void render(Graphics2D g2, GameModel model) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Wall wall : model.getWalls()) drawWall(g2, wall);
        for (Bullet bullet : model.getBullets()) drawBullet(g2, bullet);
        drawTank(g2, model.getPlayer1());
        drawTank(g2, model.getPlayer2());

        drawHud(g2, model);

        if (model.isGameOver()) {
            drawGameOver(g2, model);
        }
    }

    private BufferedImage tankImageOf(Player player) {
        return player == Player.ONE ? TANK_BLUE : TANK_RED;
    }

    private void drawWall(Graphics2D g2, Wall wall) {
        Graphics2D g = (Graphics2D) g2.create();
        int tile = WALL_TILE.getWidth();
        // Anchor the texture at the world origin so adjacent wall segments tile seamlessly.
        g.setPaint(new TexturePaint(WALL_TILE, new Rectangle(0, 0, tile, tile)));
        g.fillRect(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
        g.dispose();
    }

    private void drawBullet(Graphics2D g2, Bullet bullet) {
        g2.drawImage(BULLET_IMAGE, bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight(), null);
    }

    private void drawTank(Graphics2D g2, Tank tank) {
        Graphics2D g = (Graphics2D) g2.create();
        int x = tank.getX();
        int y = tank.getY();
        int size = tank.getWidth();
        int cx = x + size / 2;
        int cy = y + size / 2;

        double angle = switch (tank.getDirection()) {
            case UP -> 0;
            case RIGHT -> Math.PI / 2;
            case DOWN -> Math.PI;
            case LEFT -> -Math.PI / 2;
        };
        g.rotate(angle, cx, cy);

        BufferedImage sprite = tankImageOf(tank.getPlayer());
        g.drawImage(sprite, x, y, size, size, null);

        g.dispose();
    }

    private void drawHud(Graphics2D g2, GameModel model) {
        drawHealthBar(g2, 20, 20, model.getPlayer1(), "P1");
        drawHealthBar(g2, GameModel.WIDTH - 220, 20, model.getPlayer2(), "P2");
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, Tank tank, String label) {
        int width = 200;
        int height = 20;
        g2.setColor(Color.WHITE);
        g2.drawString(label, x, y - 5);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, width, height);
        int healthWidth = (int) (width * (tank.getHealth() / (double) tank.getMaxHealth()));
        g2.setColor(tank.getHealth() > 30 ? Color.GREEN : Color.RED);
        g2.fillRect(x, y, healthWidth, height);
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, width, height);
    }

    private void drawGameOver(Graphics2D g2, GameModel model) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);

        Player winner = model.getWinner();
        String winnerMessage = winner == null ? "Draw!"
                : winner == Player.ONE ? "Player 1 Wins!" : "Player 2 Wins!";

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        drawCentered(g2, winnerMessage, GameModel.HEIGHT / 2 - 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
        drawCentered(g2, "Press R to Restart", GameModel.HEIGHT / 2 + 30);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        int x = (GameModel.WIDTH - metrics.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }
}
