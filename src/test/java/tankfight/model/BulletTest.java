package tankfight.model;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulletTest {

    @Test
    void exposesOwnerDamageAndBounds() {
        Bullet bullet = new Bullet(Player.ONE, 42, 84, Direction.RIGHT);

        assertEquals(Player.ONE, bullet.getOwner());
        assertEquals(20, bullet.getDamage());
        assertEquals(new Rectangle(42, 84, 8, 8), bullet.getBounds());
    }
}
