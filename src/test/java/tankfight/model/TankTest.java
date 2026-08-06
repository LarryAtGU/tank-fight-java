package tankfight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TankTest {

    @Test
    void allyTankCarriesItsPlayerSlotAndAllySide() {
        Tank tank = Tank.ally(Player.ONE, 50, 50, Direction.DOWN);

        assertEquals(Side.ALLIES, tank.getSide());
        assertEquals(Player.ONE, tank.getPlayer());
        assertEquals(Direction.DOWN, tank.getDirection());
        assertEquals(Tank.ALLY_HEALTH, tank.getHealth());
        assertEquals(Tank.ALLY_HEALTH, tank.getMaxHealth());
        assertEquals(Tank.ALLY_SPEED, tank.getSpeed());
        assertTrue(tank.isAlive());
    }

    @Test
    void enemyTankHasNoPlayerSlot() {
        Tank tank = Tank.enemy(100, 20, Direction.DOWN);

        assertEquals(Side.ENEMIES, tank.getSide());
        assertNull(tank.getPlayer());
        assertEquals(Tank.ENEMY_HEALTH, tank.getMaxHealth());
        assertEquals(Tank.ENEMY_SPEED, tank.getSpeed());
    }

    @Test
    void takeDamageReducesHealthAndKeepsTankAliveAboveZero() {
        Tank tank = Tank.ally(Player.ONE, 0, 0, Direction.UP);

        tank.takeDamage(30);

        assertEquals(Tank.ALLY_HEALTH - 30, tank.getHealth());
        assertTrue(tank.isAlive());
    }

    @Test
    void takeDamageFloorsAtZeroAndKillsTank() {
        Tank tank = Tank.ally(Player.ONE, 0, 0, Direction.UP);

        tank.takeDamage(1000);

        assertEquals(0, tank.getHealth());
        assertFalse(tank.isAlive());
    }

    @Test
    void fireRespectsCooldownThenAllowsFiringAgain() {
        Tank tank = Tank.ally(Player.ONE, 0, 0, Direction.UP);
        long now = 1_000L;

        assertTrue(tank.canFire(now));

        tank.fire(now);

        assertFalse(tank.canFire(now));
        assertTrue(tank.canFire(now + Tank.ALLY_FIRE_COOLDOWN_MS));
    }

    @Test
    void enemyCooldownIsSlowerThanAllyCooldown() {
        Tank enemy = Tank.enemy(0, 0, Direction.DOWN);
        long now = 1_000L;

        enemy.fire(now);

        assertFalse(enemy.canFire(now + Tank.ALLY_FIRE_COOLDOWN_MS));
        assertTrue(enemy.canFire(now + Tank.ENEMY_FIRE_COOLDOWN_MS));
    }

    @Test
    void firedBulletInheritsSideOwnerAndDirection() {
        Tank tank = Tank.ally(Player.TWO, 10, 20, Direction.LEFT);

        Bullet bullet = tank.fire(1_000L);

        assertEquals(Side.ALLIES, bullet.getSide());
        assertEquals(Player.TWO, bullet.getOwner());
        assertEquals(Direction.LEFT, bullet.getDirection());
    }

    @Test
    void enemyBulletHasNoOwnerToCreditWithKills() {
        Bullet bullet = Tank.enemy(10, 20, Direction.DOWN).fire(1_000L);

        assertEquals(Side.ENEMIES, bullet.getSide());
        assertNull(bullet.getOwner());
    }
}
