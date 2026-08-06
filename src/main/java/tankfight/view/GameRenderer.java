package tankfight.view;

import tankfight.model.Bullet;
import tankfight.model.ControlType;
import tankfight.model.GameModel;
import tankfight.model.GameSetup;
import tankfight.model.Phase;
import tankfight.model.Player;
import tankfight.model.RoundConfig;
import tankfight.model.RoundOutcome;
import tankfight.model.Side;
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
import java.util.ArrayList;
import java.util.List;

class GameRenderer {

    /** Height of the status strip drawn below the play field, so the HUD never covers spawns. */
    static final int HUD_HEIGHT = 76;
    static final int PANEL_WIDTH = GameModel.WIDTH;
    static final int PANEL_HEIGHT = GameModel.HEIGHT + HUD_HEIGHT;

    static final Color FIELD_COLOR = new Color(30, 60, 30);
    private static final Color HUD_COLOR = new Color(18, 18, 22);
    private static final Color ACCENT = new Color(255, 205, 70);
    private static final Color MUTED = new Color(150, 150, 160);
    private static final Color HEALTH_GOOD = new Color(80, 200, 90);
    private static final Color HEALTH_LOW = new Color(220, 70, 70);
    private static final Color HEALTH_EMPTY = new Color(40, 40, 46);
    /** Health drops below this fraction of maximum before a bar turns red. */
    private static final double HEALTH_LOW_FRACTION = 0.3;

    /** Geometry of the small health bar floating above each tank on the field. */
    private static final int TANK_BAR_HEIGHT = 5;
    private static final int TANK_BAR_GAP = 6;

    /** Hue of the green used for player two, derived from the blue tank sprite. */
    private static final float PLAYER_TWO_HUE = 0.33f;
    /** Pixels below this saturation (treads, outlines) keep their original colour when recolouring. */
    private static final float RECOLOR_SATURATION_FLOOR = 0.25f;

    private static final BufferedImage TANK_BLUE = loadImage("tank_blue.png");
    private static final BufferedImage TANK_RED = loadImage("tank_red.png");
    private static final BufferedImage TANK_GREEN = recolor(TANK_BLUE, PLAYER_TWO_HUE);
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

    /**
     * Recolours a sprite to {@code hue}, leaving near-grey pixels (treads, outlines, shadows)
     * untouched. Lets the three tank types be visually distinct without shipping a third PNG.
     */
    private static BufferedImage recolor(BufferedImage source, float hue) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsb);
                int rgb = hsb[1] < RECOLOR_SATURATION_FLOOR
                        ? argb
                        : Color.HSBtoRGB(hue, hsb[1], hsb[2]);
                out.setRGB(x, y, (argb & 0xFF000000) | (rgb & 0x00FFFFFF));
            }
        }
        return out;
    }

    void render(Graphics2D g2, GameModel model) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (model.getPhase() == Phase.MENU) {
            drawMenu(g2, model.getSetup());
            return;
        }

        g2.setColor(FIELD_COLOR);
        g2.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);

        for (Wall wall : model.getWalls()) drawWall(g2, wall);
        for (Bullet bullet : model.getBullets()) drawBullet(g2, bullet);

        // Every sprite first, then every bar, so a tank driving past a neighbour can't cover
        // the neighbour's health bar.
        List<Tank> onField = tanksOnField(model);
        for (Tank tank : onField) drawTank(g2, tank);
        for (Tank tank : onField) drawTankHealthBar(g2, tank);

        drawHud(g2, model);

        if (model.isGameOver()) {
            drawRoundOver(g2, model);
        }
    }

    /** Every tank that should currently be drawn: all live enemies, plus allies still standing. */
    private static List<Tank> tanksOnField(GameModel model) {
        List<Tank> tanks = new ArrayList<>(model.getEnemies());
        for (Tank ally : model.getAllies()) {
            if (ally.isAlive()) {
                tanks.add(ally);
            }
        }
        return tanks;
    }

    private BufferedImage spriteOf(Tank tank) {
        if (tank.getSide() == Side.ENEMIES) {
            return TANK_RED;
        }
        return tank.getPlayer() == Player.ONE ? TANK_BLUE : TANK_GREEN;
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
        g.drawImage(spriteOf(tank), x, y, size, size, null);
        g.dispose();
    }

    /**
     * A small bar floating above a tank showing how much health it has left. Every tank on the
     * field gets one, so an enemy's remaining hits are as readable as a teammate's. Drawn on the
     * unrotated graphics so it stays level whichever way the tank faces.
     */
    private void drawTankHealthBar(Graphics2D g2, Tank tank) {
        int width = tank.getWidth();
        int x = tank.getX();
        // Clamp so a tank hugging the top edge keeps its bar on screen.
        int y = Math.max(0, tank.getY() - TANK_BAR_GAP - TANK_BAR_HEIGHT);
        double fraction = Math.max(0, tank.getHealth() / (double) tank.getMaxHealth());

        g2.setColor(HEALTH_EMPTY);
        g2.fillRect(x, y, width, TANK_BAR_HEIGHT);
        g2.setColor(fraction > HEALTH_LOW_FRACTION ? HEALTH_GOOD : HEALTH_LOW);
        g2.fillRect(x, y, (int) Math.round(width * fraction), TANK_BAR_HEIGHT);
        g2.setColor(Color.BLACK);
        g2.drawRect(x, y, width, TANK_BAR_HEIGHT);
    }

    private void drawHud(Graphics2D g2, GameModel model) {
        int top = GameModel.HEIGHT;
        g2.setColor(HUD_COLOR);
        g2.fillRect(0, top, PANEL_WIDTH, HUD_HEIGHT);
        g2.setColor(new Color(60, 60, 70));
        g2.drawLine(0, top, PANEL_WIDTH, top);

        drawPlayerStatus(g2, 20, top + 16, model, Player.ONE);
        drawPlayerStatus(g2, 250, top + 16, model, Player.TWO);
        drawRoundStatus(g2, 500, top + 16, model);
    }

    private void drawPlayerStatus(Graphics2D g2, int x, int y, GameModel model, Player player) {
        String label = player == Player.ONE ? "P1" : "P2";
        Tank tank = model.getTank(player);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (tank == null) {
            g2.setColor(MUTED);
            g2.drawString(label + " —", x, y + 12);
            return;
        }

        g2.setColor(tank.isAlive() ? Color.WHITE : MUTED);
        g2.drawString(label + "   " + model.getScore(player) + " pts", x, y + 12);

        int barWidth = 170;
        int barHeight = 14;
        int barY = y + 22;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, barY, barWidth, barHeight);
        if (tank.isAlive()) {
            double fraction = tank.getHealth() / (double) tank.getMaxHealth();
            g2.setColor(fraction > HEALTH_LOW_FRACTION ? HEALTH_GOOD : HEALTH_LOW);
            g2.fillRect(x, barY, (int) Math.round(barWidth * fraction), barHeight);
        }
        g2.setColor(tank.isAlive() ? Color.WHITE : MUTED);
        g2.drawRect(x, barY, barWidth, barHeight);

        if (!tank.isAlive()) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(HEALTH_LOW);
            g2.drawString("DESTROYED", x + 46, barY + 11);
        }
    }

    private void drawRoundStatus(Graphics2D g2, int x, int y, GameModel model) {
        RoundConfig config = model.getConfig();
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(ACCENT);
        g2.drawString("LEVEL " + config.level(), x, y + 12);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        g2.drawString("Enemies left: " + model.getEnemiesRemaining() + " / " + config.totalEnemies(),
                x, y + 32);
        g2.drawString("Team score: " + model.getTeamScore(), x, y + 50);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(MUTED);
        drawRightAligned(g2, "On field: " + model.getEnemies().size()
                + " / " + config.maxConcurrentEnemies(), y + 32);
        drawRightAligned(g2, "Esc — quit to menu", y + 50);
    }

    /** Draws {@code text} flush against the panel's right margin. */
    private void drawRightAligned(Graphics2D g2, String text, int y) {
        int right = PANEL_WIDTH - 20;
        g2.drawString(text, right - g2.getFontMetrics().stringWidth(text), y);
    }

    private void drawMenu(Graphics2D g2, GameSetup setup) {
        g2.setColor(HUD_COLOR);
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setColor(ACCENT);
        g2.setFont(new Font("SansSerif", Font.BOLD, 52));
        drawCentered(g2, "TANK FIGHT", 150);

        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        drawCentered(g2, "Defend against the enemy horde — allies can't hurt each other", 190);

        int rowY = 280;
        int rowGap = 52;
        drawMenuRow(g2, setup, GameSetup.ROW_PLAYER_COUNT, rowY,
                "Players", String.valueOf(setup.getPlayerCount()));
        drawMenuRow(g2, setup, GameSetup.ROW_PLAYER_ONE, rowY + rowGap,
                "Player 1", labelFor(setup.getControlType(Player.ONE)));
        drawMenuRow(g2, setup, GameSetup.ROW_PLAYER_TWO, rowY + rowGap * 2,
                "Player 2", setup.getPlayerCount() == 1 ? "—" : labelFor(setup.getControlType(Player.TWO)));
        drawMenuRow(g2, setup, GameSetup.ROW_LEVEL, rowY + rowGap * 3,
                "Level", String.valueOf(setup.getLevel()));

        RoundConfig config = RoundConfig.forLevel(setup.getLevel());
        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        drawCentered(g2, config.totalEnemies() + " enemy tanks this round, up to "
                + config.maxConcurrentEnemies() + " on screen at once", rowY + rowGap * 3 + 42);

        drawCentered(g2, "W/S or ↑/↓ select     A/D or ←/→ change     Space or Enter to start", 570);
    }

    private void drawMenuRow(Graphics2D g2, GameSetup setup, int row, int y, String label, String value) {
        boolean selected = setup.getSelectedRow() == row;
        boolean disabled = setup.isRowDisabled(row);
        int labelRight = PANEL_WIDTH / 2 - 30;
        int valueLeft = PANEL_WIDTH / 2 + 30;

        if (selected) {
            g2.setColor(new Color(255, 205, 70, 30));
            g2.fillRect(labelRight - 220, y - 26, 500, 40);
            g2.setColor(ACCENT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2.drawString("▶", labelRight - 250, y);
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g2.setColor(disabled ? MUTED : Color.WHITE);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(label, labelRight - metrics.stringWidth(label), y);

        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.setColor(disabled ? MUTED : (selected ? ACCENT : Color.WHITE));
        g2.drawString(value, valueLeft, y);
    }

    private static String labelFor(ControlType type) {
        return type == ControlType.HUMAN ? "HUMAN" : "COMPUTER";
    }

    private void drawRoundOver(Graphics2D g2, GameModel model) {
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);

        boolean won = model.getOutcome() == RoundOutcome.VICTORY;
        g2.setColor(won ? new Color(110, 220, 120) : new Color(230, 90, 90));
        g2.setFont(new Font("SansSerif", Font.BOLD, 54));
        drawCentered(g2, won ? "ROUND CLEARED!" : "DEFEAT", GameModel.HEIGHT / 2 - 60);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        drawCentered(g2, "Team score: " + model.getTeamScore(), GameModel.HEIGHT / 2 - 10);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(g2, scoreBreakdown(model), GameModel.HEIGHT / 2 + 24);
        drawCentered(g2, model.getEnemiesDestroyed() + " enemy tanks destroyed",
                GameModel.HEIGHT / 2 + 50);

        g2.setColor(ACCENT);
        drawCentered(g2, "Press R to replay this level    ·    M for the menu", GameModel.HEIGHT / 2 + 100);
    }

    private static String scoreBreakdown(GameModel model) {
        String breakdown = "P1: " + model.getScore(Player.ONE);
        if (model.getTank(Player.TWO) != null) {
            breakdown += "    P2: " + model.getScore(Player.TWO);
        }
        return breakdown;
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (PANEL_WIDTH - metrics.stringWidth(text)) / 2, y);
    }
}
