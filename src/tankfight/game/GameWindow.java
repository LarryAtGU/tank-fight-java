package tankfight.game;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GameWindow extends JFrame {

    public GameWindow() {
        super("Tank Fight");
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(gamePanel::requestFocusInWindow);
    }
}
