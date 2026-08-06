package tankfight;

import tankfight.controller.GameController;
import tankfight.controller.KeyBindings;
import tankfight.controller.KeyboardInput;
import tankfight.model.GameModel;
import tankfight.view.GamePanel;
import tankfight.view.GameWindow;

import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModel model = new GameModel();
            GamePanel panel = new GamePanel(model);

            KeyboardInput input = new KeyboardInput();
            panel.addKeyListener(input);

            KeyBindings player1Bindings = new KeyBindings(
                    KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE);
            KeyBindings player2Bindings = new KeyBindings(
                    KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_ENTER);

            GameController controller = new GameController(
                    model, panel, input, player1Bindings, player2Bindings,
                    KeyEvent.VK_R, KeyEvent.VK_M, KeyEvent.VK_ESCAPE);

            GameWindow window = new GameWindow(panel);
            window.setVisible(true);
            panel.requestFocusInWindow();

            controller.start();
        });
    }
}
