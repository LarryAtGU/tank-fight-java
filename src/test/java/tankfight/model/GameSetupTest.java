package tankfight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSetupTest {

    @Test
    void defaultsToTwoHumanPlayersOnLevelOne() {
        GameSetup setup = new GameSetup();

        assertEquals(2, setup.getPlayerCount());
        assertEquals(ControlType.HUMAN, setup.getControlType(Player.ONE));
        assertEquals(ControlType.HUMAN, setup.getControlType(Player.TWO));
        assertEquals(RoundConfig.MIN_LEVEL, setup.getLevel());
        assertEquals(GameSetup.ROW_PLAYER_COUNT, setup.getSelectedRow());
    }

    @Test
    void adjustingThePlayerCountRowTogglesBetweenOneAndTwo() {
        GameSetup setup = new GameSetup();

        setup.adjust(1);
        assertEquals(1, setup.getPlayerCount());

        setup.adjust(1);
        assertEquals(2, setup.getPlayerCount());
    }

    @Test
    void eitherPlayerSlotCanBeSwitchedToAi() {
        GameSetup setup = new GameSetup();

        setup.moveCursor(1);
        setup.adjust(1);
        assertEquals(ControlType.AI, setup.getControlType(Player.ONE));

        setup.moveCursor(1);
        setup.adjust(1);
        assertEquals(ControlType.AI, setup.getControlType(Player.TWO));
    }

    @Test
    void thePlayerTwoRowIsSkippedAndDisabledInAOnePlayerSetup() {
        GameSetup setup = new GameSetup();
        setup.setPlayerCount(1);

        assertTrue(setup.isRowDisabled(GameSetup.ROW_PLAYER_TWO));
        assertFalse(setup.isRowDisabled(GameSetup.ROW_PLAYER_ONE));

        setup.moveCursor(1);
        assertEquals(GameSetup.ROW_PLAYER_ONE, setup.getSelectedRow());

        setup.moveCursor(1);
        assertEquals(GameSetup.ROW_LEVEL, setup.getSelectedRow(),
                "the cursor must jump over the unusable player two row");
    }

    @Test
    void droppingToOnePlayerMovesTheCursorOffThePlayerTwoRow() {
        GameSetup setup = new GameSetup();
        setup.moveCursor(1);
        setup.moveCursor(1);
        assertEquals(GameSetup.ROW_PLAYER_TWO, setup.getSelectedRow());

        setup.setPlayerCount(1);

        assertNotEquals(GameSetup.ROW_PLAYER_TWO, setup.getSelectedRow());
    }

    @Test
    void theCursorWrapsAround() {
        GameSetup setup = new GameSetup();

        setup.moveCursor(-1);

        assertEquals(GameSetup.ROW_LEVEL, setup.getSelectedRow());
    }

    @Test
    void levelIsClampedToTheSupportedRange() {
        GameSetup setup = new GameSetup();

        setup.setLevel(-3);
        assertEquals(RoundConfig.MIN_LEVEL, setup.getLevel());

        setup.setLevel(1000);
        assertEquals(RoundConfig.MAX_LEVEL, setup.getLevel());
    }

    @Test
    void rejectsPlayerCountsTheGameCannotStage() {
        GameSetup setup = new GameSetup();

        assertThrows(IllegalArgumentException.class, () -> setup.setPlayerCount(0));
        assertThrows(IllegalArgumentException.class, () -> setup.setPlayerCount(3));
    }
}
