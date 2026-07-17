package tankfight.model;

public record PlayerAction(Direction moveDirection, boolean fire) {
    public static final PlayerAction NONE = new PlayerAction(null, false);
}
