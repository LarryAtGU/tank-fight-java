package tankfight.model;

/** How a round ended. */
public enum RoundOutcome {
    /** Every enemy tank in the round was destroyed. */
    VICTORY,
    /** Every ally tank was destroyed before the round was cleared. */
    DEFEAT
}
