package tankfight.model;

/** Which screen the game is on. */
public enum Phase {
    /** Setup screen: choosing player count, human/AI per player, and level. */
    MENU,
    /** A round is in progress. */
    PLAYING,
    /** The round finished; see {@link GameModel#getOutcome()}. */
    ROUND_OVER
}
