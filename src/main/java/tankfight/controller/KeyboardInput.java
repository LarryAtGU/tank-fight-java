package tankfight.controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class KeyboardInput implements InputSource, KeyListener {

    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> freshPresses = new HashSet<>();

    @Override
    public boolean isPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    @Override
    public boolean consumePress(int keyCode) {
        return freshPresses.remove(keyCode);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // no-op
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // add() is false when the key was already down, which filters out OS key-repeat and
        // keeps freshPresses to one entry per real press.
        if (pressedKeys.add(e.getKeyCode())) {
            freshPresses.add(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }
}
