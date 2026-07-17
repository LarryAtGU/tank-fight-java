package tankfight.model;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WallTest {

    @Test
    void boundsMatchConstructorArguments() {
        Wall wall = new Wall(10, 20, 120, 20);

        assertEquals(new Rectangle(10, 20, 120, 20), wall.getBounds());
    }
}
