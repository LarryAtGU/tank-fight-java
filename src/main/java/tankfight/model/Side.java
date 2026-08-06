package tankfight.model;

/**
 * The two teams in a round. Bullets only damage tanks of the opposing side, so
 * allies can never kill each other and neither can enemies.
 */
public enum Side {
    ALLIES,
    ENEMIES
}
