package tankfight.view;

import tankfight.model.Bullet;
import tankfight.model.GameModel;
import tankfight.model.Player;
import tankfight.model.Tank;
import tankfight.model.Wall;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

class GameRenderer {
    private static final Color PLAYER_ONE_COLOR = new Color(70, 130, 220);
    private static final Color PLAYER_TWO_COLOR = new Color(220, 70, 70);

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

    private Color colorOf(Player player) {
        return player == Player.ONE ? PLAYER_ONE_COLOR : PLAYER_TWO_COLOR;
    }

    private void drawWall(Graphics2D g2, Wall wall) {
        g2.setColor(new Color(120, 120, 120));
        g2.fillRect(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
    }

    private void drawBullet(Graphics2D g2, Bullet bullet) {
        g2.setColor(Color.YELLOW);
        g2.fillOval(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
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

        Color color = colorOf(tank.getPlayer());
        g.setColor(color.darker());
        g.fillRect(cx - 4, y - 8, 8, size / 2 + 8);

        g.setColor(color);
        g.fillRoundRect(x, y, size, size, 8, 8);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, size, size, 8, 8);

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
