package tankfight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TankTest {

    @Test
    void initialState() {
        Tank tank = new Tank(Player.ONE, 50, 50, Direction.DOWN);

        assertEquals(Player.ONE, tank.getPlayer());
        assertEquals(Direction.DOWN, tank.getDirection());
        assertEquals(100, tank.getHealth());
        assertEquals(100, tank.getMaxHealth());
        assertTrue(tank.isAlive());
    }

    @Test
    void takeDamageReducesHealthAndKeepsTankAliveAboveZero() {
        Tank tank = new Tank(Player.ONE, 0, 0, Direction.UP);

        tank.takeDamage(30);

        assertEquals(70, tank.getHealth());
        assertTrue(tank.isAlive());
    }

    @Test
    void takeDamageFloorsAtZeroAndKillsTank() {
        Tank tank = new Tank(Player.ONE, 0, 0, Direction.UP);

        tank.takeDamage(1000);

        assertEquals(0, tank.getHealth());
        assertFalse(tank.isAlive());
    }

    @Test
    void fireRespectsCooldownThenAllowsFiringAgain() {
        Tank tank = new Tank(Player.ONE, 0, 0, Direction.UP);
        long now = 1_000L;

        assertTrue(tank.canFire(now));

        tank.fire(now);

        assertFalse(tank.canFire(now));
        assertTrue(tank.canFire(now + 500));
    }

    @Test
    void fireProducesBulletMatchingOwnerAndDirection() {
        Tank tank = new Tank(Player.TWO, 10, 20, Direction.LEFT);
        long now = 1_000L;

        Bullet bullet = tank.fire(now);

        assertEquals(Player.TWO, bullet.getOwner());
        assertEquals(Direction.LEFT, bullet.getDirection());
    }
}
