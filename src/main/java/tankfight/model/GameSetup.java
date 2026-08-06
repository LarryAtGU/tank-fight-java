package tankfight.model;

/**
 * The mutable state behind the setup menu: how many player slots are in play, whether each is
 * driven by a human or the computer, which level to run, and which menu row the cursor is on.
 *
 * <p>This lives in the model rather than the view so the menu can be driven and asserted on
 * headlessly; the renderer only reads it and the controller only nudges it through
 * {@link #moveCursor(int)} / {@link #adjust(int)}.
 */
public class GameSetup {

    public static final int ROW_PLAYER_COUNT = 0;
    public static final int ROW_PLAYER_ONE = 1;
    public static final int ROW_PLAYER_TWO = 2;
    public static final int ROW_LEVEL = 3;
    public static final int ROW_COUNT = 4;

    private int playerCount = 2;
    private ControlType player1Type = ControlType.HUMAN;
    private ControlType player2Type = ControlType.HUMAN;
    private int level = RoundConfig.MIN_LEVEL;
    private int selectedRow = ROW_PLAYER_COUNT;

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        if (playerCount < 1 || playerCount > 2) {
            throw new IllegalArgumentException("playerCount must be 1 or 2, was " + playerCount);
        }
        this.playerCount = playerCount;
        if (playerCount == 1 && selectedRow == ROW_PLAYER_TWO) {
            selectedRow = ROW_LEVEL;
        }
    }

    public ControlType getControlType(Player player) {
        return player == Player.ONE ? player1Type : player2Type;
    }

    public void setControlType(Player player, ControlType type) {
        if (player == Player.ONE) {
            player1Type = type;
        } else {
            player2Type = type;
        }
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(RoundConfig.MIN_LEVEL, Math.min(RoundConfig.MAX_LEVEL, level));
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    /** True when {@code row} is shown but not usable — the P2 row in a one-player setup. */
    public boolean isRowDisabled(int row) {
        return row == ROW_PLAYER_TWO && playerCount == 1;
    }

    /** Moves the cursor by {@code delta} rows, wrapping and skipping disabled rows. */
    public void moveCursor(int delta) {
        if (delta == 0) {
            return;
        }
        int step = delta > 0 ? 1 : -1;
        for (int i = 0; i < ROW_COUNT; i++) {
            selectedRow = Math.floorMod(selectedRow + step, ROW_COUNT);
            if (!isRowDisabled(selectedRow)) {
                return;
            }
        }
    }

    /** Changes the value on the selected row. {@code delta} is -1 (left) or +1 (right). */
    public void adjust(int delta) {
        if (delta == 0) {
            return;
        }
        switch (selectedRow) {
            case ROW_PLAYER_COUNT -> setPlayerCount(playerCount == 1 ? 2 : 1);
            case ROW_PLAYER_ONE -> player1Type = toggle(player1Type);
            case ROW_PLAYER_TWO -> {
                if (playerCount == 2) {
                    player2Type = toggle(player2Type);
                }
            }
            case ROW_LEVEL -> setLevel(level + (delta > 0 ? 1 : -1));
            default -> throw new IllegalStateException("Unknown menu row: " + selectedRow);
        }
    }

    private static ControlType toggle(ControlType type) {
        return type == ControlType.HUMAN ? ControlType.AI : ControlType.HUMAN;
    }
}
