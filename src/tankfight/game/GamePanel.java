package tankfight.game;

import tankfight.entities.Bullet;
import tankfight.entities.Tank;
import tankfight.entities.Wall;
import tankfight.input.KeyHandler;
import tankfight.util.Direction;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    private static final int FPS = 60;

    private final KeyHandler keyHandler = new KeyHandler();
    private final List<Wall> walls = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private Tank player1;
    private Tank player2;
    private boolean gameOver = false;
    private String winnerMessage = "";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 60, 30));
        setFocusable(true);
        addKeyListener(keyHandler);

        initWalls();
        resetGame();

        Timer timer = new Timer(1000 / FPS, e -> gameLoop());
        timer.start();
    }

    private void initWalls() {
        walls.add(new Wall(0, 0, WIDTH, 10));
        walls.add(new Wall(0, HEIGHT - 10, WIDTH, 10));
        walls.add(new Wall(0, 0, 10, HEIGHT));
        walls.add(new Wall(WIDTH - 10, 0, 10, HEIGHT));

        walls.add(new Wall(WIDTH / 2 - 60, HEIGHT / 2 - 10, 120, 20));
        walls.add(new Wall(200, 150, 20, 150));
        walls.add(new Wall(WIDTH - 220, HEIGHT - 300, 20, 150));
    }

    private void resetGame() {
        bullets.clear();
        player1 = new Tank(50, 50, Direction.DOWN, new Color(70, 130, 220));
        player2 = new Tank(WIDTH - 90, HEIGHT - 90, Direction.UP, new Color(220, 70, 70));
        gameOver = false;
        winnerMessage = "";
    }

    private void gameLoop() {
        if (!gameOver) {
            handleInput();
            updateBullets();
            checkGameOver();
        } else if (keyHandler.isPressed(KeyEvent.VK_R)) {
            resetGame();
        }
        repaint();
    }

    private void handleInput() {
        handleTankInput(player1, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);
        handleTankInput(player2, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);
    }

    private void handleTankInput(Tank tank, int up, int down, int left, int right, int fireKey) {
        Direction moveDir = null;
        if (keyHandler.isPressed(up)) moveDir = Direction.UP;
        else if (keyHandler.isPressed(down)) moveDir = Direction.DOWN;
        else if (keyHandler.isPressed(left)) moveDir = Direction.LEFT;
        else if (keyHandler.isPressed(right)) moveDir = Direction.RIGHT;

        if (moveDir != null) {
            tank.setDirection(moveDir);
            moveTank(tank, moveDir);
        }

        if (keyHandler.isPressed(fireKey) && tank.canFire(System.currentTimeMillis())) {
            bullets.add(tank.fire(System.currentTimeMillis()));
        }
    }

    private void moveTank(Tank tank, Direction dir) {
        int nx = tank.getX() + dir.dx() * tank.getSpeed();
        int ny = tank.getY() + dir.dy() * tank.getSpeed();
        Rectangle newBounds = tank.getBoundsAt(nx, ny);

        for (Wall wall : walls) {
            if (newBounds.intersects(wall.getBounds())) return;
        }

        Tank other = tank == player1 ? player2 : player1;
        if (newBounds.intersects(other.getBounds())) return;

        tank.setPosition(nx, ny);
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.update();
            Rectangle bounds = bullet.getBounds();

            if (bounds.x < 0 || bounds.y < 0 || bounds.x > WIDTH || bounds.y > HEIGHT) {
                it.remove();
                continue;
            }

            boolean hitWall = false;
            for (Wall wall : walls) {
                if (bounds.intersects(wall.getBounds())) {
                    hitWall = true;
                    break;
                }
            }
            if (hitWall) {
                it.remove();
                continue;
            }

            Tank target = bullet.getOwner() == player1 ? player2 : player1;
            if (target.isAlive() && bounds.intersects(target.getBounds())) {
                target.takeDamage(bullet.getDamage());
                it.remove();
            }
        }
    }

    private void checkGameOver() {
        if (!player1.isAlive() || !player2.isAlive()) {
            gameOver = true;
            winnerMessage = player1.isAlive() ? "Player 1 Wins!" : "Player 2 Wins!";
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Wall wall : walls) wall.draw(g2);
        for (Bullet bullet : bullets) bullet.draw(g2);
        player1.draw(g2);
        player2.draw(g2);

        drawHud(g2);

        if (gameOver) {
            drawGameOver(g2);
        }
    }

    private void drawHud(Graphics2D g2) {
        drawHealthBar(g2, 20, 20, player1, "P1");
        drawHealthBar(g2, WIDTH - 220, 20, player2, "P2");
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

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        drawCentered(g2, winnerMessage, HEIGHT / 2 - 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
        drawCentered(g2, "Press R to Restart", HEIGHT / 2 + 30);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        int x = (WIDTH - metrics.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }
}
