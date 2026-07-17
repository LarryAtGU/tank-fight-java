package tankfight.view;

import tankfight.model.GameModel;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class GamePanel extends JPanel implements GameView {
    private final GameModel model;
    private final GameRenderer renderer = new GameRenderer();

    public GamePanel(GameModel model) {
        this.model = model;
        setPreferredSize(new Dimension(GameModel.WIDTH, GameModel.HEIGHT));
        setBackground(new Color(30, 60, 30));
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
