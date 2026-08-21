package tankfight.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The brick layout of one level. A level number picks both its difficulty ({@link RoundConfig})
 * and its terrain, so level is the only knob: it decides how many enemies arrive, how fast, and
 * what ground the fight happens on.
 *
 * <p>Each layout is stored as axis-aligned rectangles, which is how the specification states
 * them, and {@link #buildBricks()} expands those onto the fixed {@link Wall#BRICK_SIZE} grid.
 * Every tile is destructible: a round chews holes in its maze as it is played, and
 * {@link #buildBricks()} hands back fresh tiles each time so restarting a level rebuilds it
 * intact.
 */
public enum Maze {

    /** A centre block and two pillars — almost nothing in the way. */
    OPEN_FIELD("Open Field", new int[][]{
            {340, 280, 120, 20},
            {200, 140, 20, 160},
            {580, 300, 20, 160},
    }),

    /** A broken cross. Four lanes feed the middle, and the middle is where everyone ends up. */
    CROSSROADS("Crossroads", new int[][]{
            {380, 160, 20, 120},
            {380, 340, 20, 120},
            {180, 280, 140, 20},
            {480, 280, 140, 20},
            {120, 120, 20, 100},
            {660, 380, 20, 100},
    }),

    /** North-south corridors with staggered gaps: long sightlines down a lane, blind elsewhere. */
    LANES("Lanes", new int[][]{
            {160, 80, 20, 220},
            {160, 380, 20, 140},
            {320, 140, 20, 300},
            {480, 80, 20, 220},
            {480, 380, 20, 140},
            {640, 140, 20, 300},
    }),

    /** Two rooms with doorways. The fight happens at the gaps unless someone makes a new one. */
    CHAMBERS("Chambers", new int[][]{
            {140, 200, 220, 20},
            {440, 200, 220, 20},
            {140, 380, 220, 20},
            {440, 380, 220, 20},
            {140, 220, 20, 160},
            {640, 220, 20, 160},
            {380, 80, 20, 80},
            {380, 440, 20, 80},
    }),

    /** Interlocking teeth from the top and bottom edges. Around is slow; through is two bullets. */
    COMB("Comb", new int[][]{
            {120, 100, 20, 200},
            {240, 100, 20, 200},
            {360, 100, 20, 200},
            {480, 100, 20, 200},
            {600, 100, 20, 200},
            {180, 380, 20, 180},
            {300, 380, 20, 140},
            {420, 380, 20, 180},
            {540, 380, 20, 180},
            {660, 380, 20, 180},
    }),

    /** One route wrapping inward. The fastest way back out is through a wall. */
    SPIRAL("Spiral", new int[][]{
            {140, 120, 500, 20},
            {620, 140, 20, 320},
            {200, 440, 420, 20},
            {200, 240, 20, 200},
            {220, 240, 300, 20},
            {500, 260, 20, 100},
            {300, 360, 220, 20},
    }),

    /** A regular field of short blocks: open enough to move, broken enough to lose a tank in. */
    GRID("Grid", new int[][]{
            {120, 120, 80, 20},
            {280, 120, 80, 20},
            {440, 120, 80, 20},
            {600, 120, 80, 20},
            {120, 260, 80, 20},
            {280, 260, 80, 20},
            {440, 260, 80, 20},
            {600, 260, 80, 20},
            {120, 400, 80, 20},
            {280, 400, 80, 20},
            {440, 400, 80, 20},
            {600, 400, 80, 20},
            {200, 180, 20, 60},
            {520, 180, 20, 60},
            {200, 320, 20, 60},
            {520, 320, 20, 60},
    }),

    /** Dense and winding. At this level shooting a path is usually faster than finding one. */
    LABYRINTH("Labyrinth", new int[][]{
            {100, 100, 20, 180},
            {100, 360, 20, 160},
            {220, 100, 20, 60},
            {220, 220, 20, 300},
            {120, 200, 100, 20},
            {240, 160, 140, 20},
            {340, 260, 20, 200},
            {360, 260, 180, 20},
            {540, 100, 20, 180},
            {540, 340, 20, 180},
            {660, 160, 20, 300},
            {560, 440, 100, 20},
            {400, 440, 20, 80},
            {240, 440, 100, 20},
    });

    private final String mazeName;
    private final int[][] rectangles;

    Maze(String mazeName, int[][] rectangles) {
        this.mazeName = mazeName;
        this.rectangles = rectangles;
    }

    /** The maze for {@code level}, clamped into {@link RoundConfig#MIN_LEVEL}..{@code MAX_LEVEL}. */
    public static Maze forLevel(int level) {
        int clamped = Math.max(RoundConfig.MIN_LEVEL, Math.min(RoundConfig.MAX_LEVEL, level));
        return values()[clamped - RoundConfig.MIN_LEVEL];
    }

    /** The layout's display name, e.g. "Labyrinth". */
    public String getMazeName() {
        return mazeName;
    }

    /**
     * Fresh, full-health brick tiles for this layout, ordered by y and then x. Rectangles that
     * touch or overlap contribute a tile once.
     */
    public List<Wall> buildBricks() {
        Set<Point> tiles = new LinkedHashSet<>();
        for (int[] rectangle : rectangles) {
            int x = rectangle[0];
            int y = rectangle[1];
            int width = rectangle[2];
            int height = rectangle[3];
            for (int ty = y; ty < y + height; ty += Wall.BRICK_SIZE) {
                for (int tx = x; tx < x + width; tx += Wall.BRICK_SIZE) {
                    tiles.add(new Point(tx, ty));
                }
            }
        }

        List<Point> ordered = new ArrayList<>(tiles);
        ordered.sort(Comparator.comparingInt((Point p) -> p.y).thenComparingInt(p -> p.x));

        List<Wall> bricks = new ArrayList<>(ordered.size());
        for (Point tile : ordered) {
            bricks.add(Wall.brick(tile.x, tile.y));
        }
        return bricks;
    }
}
