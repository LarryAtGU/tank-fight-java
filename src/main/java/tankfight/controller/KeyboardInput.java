package tankfight.controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class KeyboardInput implements InputSource, KeyListener {

    private final Set<Integer> pressedKeys = new HashSet<>();

    @Override
    public boolean isPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // no-op
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }
}
