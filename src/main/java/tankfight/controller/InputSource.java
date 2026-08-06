package tankfight.controller;

public interface InputSource {

    /** True for as long as the key is held — what driving a tank needs. */
    boolean isPressed(int keyCode);

    /**
     * True exactly once per physical press of the key, and consumed by the call — what menu
     * navigation needs, so holding a key doesn't race through every option at 60 FPS.
     */
    boolean consumePress(int keyCode);
}
