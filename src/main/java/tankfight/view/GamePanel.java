package tankfight.view;

import tankfight.model.GameModel;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GamePanel extends JPanel implements GameView {
    private final GameModel model;
    private final GameRenderer renderer = new GameRenderer();

    public GamePanel(GameModel model) {
        this.model = model;
        // Taller than the play field: the renderer puts the HUD in a strip underneath it.
        setPreferredSize(new Dimension(GameRenderer.PANEL_WIDTH, GameRenderer.PANEL_HEIGHT));
        setBackground(GameRenderer.FIELD_COLOR);
        setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render((Graphics2D) g, model);
    }

    @Override
    public void refresh() {
        repaint();
    }
}
