package tankfight.model;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallTest {

    @Test
    void borderBoundsMatchTheFactoryArguments() {
        Wall wall = Wall.border(10, 20, 120, 20);

        assertEquals(new Rectangle(10, 20, 120, 20), wall.getBounds());
    }

    @Test
    void aBrickTileIsOneGridSquareAtFullHealth() {
        Wall brick = Wall.brick(200, 140);

        assertEquals(new Rectangle(200, 140, Wall.BRICK_SIZE, Wall.BRICK_SIZE), brick.getBounds());
        assertEquals(Wall.BRICK_HIT_POINTS, brick.getHealth());
        assertEquals(Wall.BRICK_HIT_POINTS, brick.getMaxHealth());
        assertTrue(brick.isDestructible());
        assertTrue(brick.isAlive());
    }

    @Test
    void brickDamageAccumulatesAcrossSeparateHits() {
        Wall brick = Wall.brick(0, 0);

        brick.takeDamage(20);
        assertEquals(20, brick.getHealth(), "one bullet should not finish a tile");
        assertTrue(brick.isAlive());

        brick.takeDamage(20);
        assertEquals(0, brick.getHealth());
        assertFalse(brick.isAlive(), "two bullets should destroy a tile");
    }

    @Test
    void brickHealthNeverGoesNegative() {
        Wall brick = Wall.brick(0, 0);

        brick.takeDamage(Wall.BRICK_HIT_POINTS * 3);

        assertEquals(0, brick.getHealth());
    }

    @Test
    void theBorderIsImmuneToEverything() {
        Wall border = Wall.border(0, 0, 800, 10);

        border.takeDamage(Wall.BRICK_HIT_POINTS * 100);

        assertFalse(border.isDestructible());
        assertTrue(border.isAlive(), "the border is never destroyed");
    }
}
