package tankfight.model;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GameModel {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private final List<Wall> walls = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private Tank player1;
    private Tank player2;
    private boolean gameOver;
    private Player winner;

    public GameModel() {
        initWalls();
        reset();
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

    public void reset() {
        bullets.clear();
        player1 = new Tank(Player.ONE, 50, 50, Direction.DOWN);
        player2 = new Tank(Player.TWO, WIDTH - 90, HEIGHT - 90, Direction.UP);
        gameOver = false;
        winner = null;
    }

    public void update(PlayerAction player1Action, PlayerAction player2Action, long now) {
        if (gameOver) {
            return;
        }

        updateBullets();
        applyAction(player1, player2, player1Action, now);
        applyAction(player2, player1, player2Action, now);
        checkGameOver();
    }

    private void applyAction(Tank tank, Tank other, PlayerAction action, long now) {
        if (action.moveDirection() != null) {
            tank.setDirection(action.moveDirection());
            tryMove(tank, other, action.moveDirection());
        }
        if (action.fire() && tank.canFire(now)) {
            bullets.add(tank.fire(now));
        }
    }

    private void tryMove(Tank tank, Tank other, Direction dir) {
        int nx = tank.getX() + dir.dx() * tank.getSpeed();
        int ny = tank.getY() + dir.dy() * tank.getSpeed();
        Rectangle newBounds = tank.getBoundsAt(nx, ny);

        for (Wall wall : walls) {
            if (newBounds.intersects(wall.getBounds())) {
                return;
            }
        }

        if (newBounds.intersects(other.getBounds())) {
            return;
        }

        tank.setPosition(nx, ny);
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.advance();
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

            Tank target = bullet.getOwner() == Player.ONE ? player2 : player1;
            if (bounds.intersects(target.getBounds())) {
                if (target.isAlive()) {
                    target.takeDamage(bullet.getDamage());
                }
                it.remove();
            }
        }
    }

    private void checkGameOver() {
        boolean player1Alive = player1.isAlive();
        boolean player2Alive = player2.isAlive();

        if (!player1Alive || !player2Alive) {
            gameOver = true;
            if (player1Alive) {
                winner = Player.ONE;
            } else if (player2Alive) {
                winner = Player.TWO;
            } else {
                winner = null;
            }
        }
    }

    public Tank getPlayer1() {
        return player1;
    }

    public Tank getPlayer2() {
        return player2;
    }

    public List<Wall> getWalls() {
        return Collections.unmodifiableList(walls);
    }

    public List<Bullet> getBullets() {
        return Collections.unmodifiableList(bullets);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Player getWinner() {
        return winner;
    }
}
