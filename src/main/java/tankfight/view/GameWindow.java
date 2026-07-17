package tankfight.view;

import javax.swing.JFrame;

public class GameWindow extends JFrame {

    public GameWindow(GamePanel gamePanel) {
        super("Tank Fight");
        add(gamePanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }
}
