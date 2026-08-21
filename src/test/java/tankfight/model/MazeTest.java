package tankfight.model;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MazeTest {

    /** Tile counts for levels 1..8, straight out of the specification. */
    private static final int[] TILE_COUNTS = {22, 36, 66, 68, 93, 103, 60, 113};

    private static final String[] NAMES = {
            "Open Field", "Crossroads", "Lanes", "Chambers", "Comb", "Spiral", "Grid", "Labyrinth"};

    private static Set<Point> tilesOf(Maze maze) {
        Set<Point> tiles = new HashSet<>();
        for (Wall brick : maze.buildBricks()) {
            tiles.add(new Point(brick.getX(), brick.getY()));
        }
        return tiles;
    }

    @Test
    void everyLevelExpandsToTheTileCountTheSpecificationStates() {
        for (int level = RoundConfig.MIN_LEVEL; level <= RoundConfig.MAX_LEVEL; level++) {
            List<Wall> bricks = Maze.forLevel(level).buildBricks();

            assertEquals(TILE_COUNTS[level - 1], bricks.size(), "tile count for level " + level);
            assertEquals(bricks.size(), tilesOf(Maze.forLevel(level)).size(),
                    "level " + level + " should not lay two tiles on the same square");
        }
    }

    @Test
    void everyLevelHasItsOwnNamedLayout() {
        for (int level = RoundConfig.MIN_LEVEL; level <= RoundConfig.MAX_LEVEL; level++) {
            assertEquals(NAMES[level - 1], Maze.forLevel(level).getMazeName());
        }
    }

    @Test
    void levelOneExpandsToExactlyTheSpecifiedTiles() {
        Set<Point> expected = new HashSet<>();
        for (int y = 140; y <= 280; y += 20) {
            expected.add(new Point(200, y));      // left pillar
        }
        for (int x = 340; x <= 440; x += 20) {
            expected.add(new Point(x, 280));      // centre block
        }
        for (int y = 300; y <= 440; y += 20) {
            expected.add(new Point(580, y));      // right pillar
        }

        assertEquals(expected, tilesOf(Maze.OPEN_FIELD));
    }

    @Test
    void tilesComeBackOrderedByYThenX() {
        List<Wall> bricks = Maze.LABYRINTH.buildBricks();

        for (int i = 1; i < bricks.size(); i++) {
            Wall previous = bricks.get(i - 1);
            Wall current = bricks.get(i);
            boolean ordered = previous.getY() < current.getY()
                    || (previous.getY() == current.getY() && previous.getX() < current.getX());
            assertTrue(ordered, "tile " + i + " is out of order");
        }
    }

    @Test
    void everyTileSitsOnTheBrickGrid() {
        for (int level = RoundConfig.MIN_LEVEL; level <= RoundConfig.MAX_LEVEL; level++) {
            for (Wall brick : Maze.forLevel(level).buildBricks()) {
                assertEquals(0, brick.getX() % Wall.BRICK_SIZE, "off-grid x on level " + level);
                assertEquals(0, brick.getY() % Wall.BRICK_SIZE, "off-grid y on level " + level);
                assertEquals(Wall.BRICK_SIZE, brick.getWidth());
                assertEquals(Wall.BRICK_SIZE, brick.getHeight());
            }
        }
    }

    @Test
    void noTileBlocksAnEnemyEntryPointOrAnAllyStart() {
        for (int level = RoundConfig.MIN_LEVEL; level <= RoundConfig.MAX_LEVEL; level++) {
            GameModel model = new GameModel();
            model.getSetup().setLevel(level);
            model.startRound();

            for (Tank ally : model.getAllies()) {
                for (Wall wall : model.getWalls()) {
                    assertTrue(!ally.getBounds().intersects(wall.getBounds()),
                            "an ally starts inside a wall on level " + level);
                }
            }
            for (int spawnX : GameModel.SPAWN_XS) {
                java.awt.Rectangle spot =
                        new java.awt.Rectangle(spawnX, GameModel.SPAWN_Y, Tank.SIZE, Tank.SIZE);
                for (Wall wall : model.getWalls()) {
                    assertTrue(!spot.intersects(wall.getBounds()),
                            "entry point " + spawnX + " is walled in on level " + level);
                }
            }
        }
    }

    @Test
    void levelsOutOfRangeClampRatherThanFail() {
        assertSame(Maze.OPEN_FIELD, Maze.forLevel(RoundConfig.MIN_LEVEL - 5));
        assertSame(Maze.LABYRINTH, Maze.forLevel(RoundConfig.MAX_LEVEL + 5));
    }

    @Test
    void eachCallHandsBackFreshFullHealthTiles() {
        List<Wall> first = Maze.OPEN_FIELD.buildBricks();
        first.get(0).takeDamage(Wall.BRICK_HIT_POINTS);

        List<Wall> second = Maze.OPEN_FIELD.buildBricks();

        assertNotSame(first.get(0), second.get(0));
        assertEquals(Wall.BRICK_HIT_POINTS, second.get(0).getHealth(),
                "a rebuilt maze must not remember the last round's damage");
    }
}
