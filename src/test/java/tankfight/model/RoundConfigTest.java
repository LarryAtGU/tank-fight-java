package tankfight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundConfigTest {

    @Test
    void firstLevelUsesTheGentlestSettings() {
        RoundConfig config = RoundConfig.forLevel(RoundConfig.MIN_LEVEL);

        assertEquals(RoundConfig.MIN_TOTAL_ENEMIES, config.totalEnemies());
        assertEquals(RoundConfig.MIN_CONCURRENT_ENEMIES, config.maxConcurrentEnemies());
    }

    @Test
    void lastLevelUsesTheHarshestSettings() {
        RoundConfig config = RoundConfig.forLevel(RoundConfig.MAX_LEVEL);

        assertEquals(RoundConfig.MAX_TOTAL_ENEMIES, config.totalEnemies());
        assertEquals(RoundConfig.MAX_CONCURRENT_ENEMIES, config.maxConcurrentEnemies());
    }

    @Test
    void everyLevelStaysInsideTheAgreedRanges() {
        for (int level = RoundConfig.MIN_LEVEL; level <= RoundConfig.MAX_LEVEL; level++) {
            RoundConfig config = RoundConfig.forLevel(level);

            assertTrue(config.totalEnemies() >= RoundConfig.MIN_TOTAL_ENEMIES
                            && config.totalEnemies() <= RoundConfig.MAX_TOTAL_ENEMIES,
                    "level " + level + " total out of range: " + config.totalEnemies());
            assertTrue(config.maxConcurrentEnemies() >= RoundConfig.MIN_CONCURRENT_ENEMIES
                            && config.maxConcurrentEnemies() <= RoundConfig.MAX_CONCURRENT_ENEMIES,
                    "level " + level + " concurrency out of range: " + config.maxConcurrentEnemies());
            assertTrue(config.maxConcurrentEnemies() <= config.totalEnemies());
        }
    }

    @Test
    void difficultyNeverDropsAsLevelRises() {
        RoundConfig previous = RoundConfig.forLevel(RoundConfig.MIN_LEVEL);
        for (int level = RoundConfig.MIN_LEVEL + 1; level <= RoundConfig.MAX_LEVEL; level++) {
            RoundConfig config = RoundConfig.forLevel(level);

            assertTrue(config.totalEnemies() >= previous.totalEnemies());
            assertTrue(config.maxConcurrentEnemies() >= previous.maxConcurrentEnemies());
            assertTrue(config.spawnIntervalMs() <= previous.spawnIntervalMs());
            previous = config;
        }
    }

    @Test
    void levelsOutsideTheSupportedRangeAreClamped() {
        assertEquals(RoundConfig.forLevel(RoundConfig.MIN_LEVEL), RoundConfig.forLevel(-5));
        assertEquals(RoundConfig.forLevel(RoundConfig.MAX_LEVEL), RoundConfig.forLevel(999));
    }

    @Test
    void rejectsRoundsThatCouldNeverBePlayed() {
        assertThrows(IllegalArgumentException.class, () -> new RoundConfig(1, 0, 3, 1000));
        assertThrows(IllegalArgumentException.class, () -> new RoundConfig(1, 20, 0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new RoundConfig(1, 20, 3, -1));
    }
}
