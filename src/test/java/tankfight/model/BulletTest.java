package tankfight.model;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulletTest {

    @Test
    void exposesSideOwnerDamageAndBounds() {
        Bullet bullet = new Bullet(Side.ALLIES, Player.ONE, 42, 84, Direction.RIGHT);

        assertEquals(Side.ALLIES, bullet.getSide());
        assertEquals(Player.ONE, bullet.getOwner());
        assertEquals(20, bullet.getDamage());
        assertEquals(new Rectangle(42, 84, 8, 8), bullet.getBounds());
    }

    @Test
    void advanceMovesOneSpeedStepAlongItsDirection() {
        Bullet bullet = new Bullet(Side.ENEMIES, null, 100, 100, Direction.DOWN);

        bullet.advance();

        assertEquals(100, bullet.getX());
        assertEquals(100 + bullet.getSpeed(), bullet.getY());
    }
}
