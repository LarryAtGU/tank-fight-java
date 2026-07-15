package tankfight;

import tankfight.game.GameWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
