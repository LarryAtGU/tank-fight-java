package tankfight.model;

/**
 * The difficulty knobs for one round.
 *
 * <p>{@code totalEnemies} is how many enemy tanks the round spawns in total before it can be
 * cleared; {@code maxConcurrentEnemies} caps how many of them are alive on screen at once.
 * {@link #forLevel(int)} derives both from a level number, ramping 15 &rarr; 50 total and
 * 3 &rarr; 8 concurrent across {@link #MIN_LEVEL}..{@link #MAX_LEVEL}. Callers that want a
 * hand-tuned round can build the record directly.
 */
public record RoundConfig(int level, int totalEnemies, int maxConcurrentEnemies, long spawnIntervalMs) {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 8;

    public static final int MIN_TOTAL_ENEMIES = 15;
    public static final int MAX_TOTAL_ENEMIES = 50;
    public static final int MIN_CONCURRENT_ENEMIES = 3;
    public static final int MAX_CONCURRENT_ENEMIES = 8;

    private static final long SLOWEST_SPAWN_MS = 2500;
    private static final long FASTEST_SPAWN_MS = 900;

    public RoundConfig {
        if (totalEnemies < 1) {
            throw new IllegalArgumentException("totalEnemies must be at least 1, was " + totalEnemies);
        }
        if (maxConcurrentEnemies < 1) {
            throw new IllegalArgumentException(
                    "maxConcurrentEnemies must be at least 1, was " + maxConcurrentEnemies);
        }
        if (spawnIntervalMs < 0) {
            throw new IllegalArgumentException("spawnIntervalMs must not be negative, was " + spawnIntervalMs);
        }
    }

    /** Difficulty for {@code level}, clamped into {@link #MIN_LEVEL}..{@link #MAX_LEVEL}. */
    public static RoundConfig forLevel(int level) {
        int l = clamp(level, MIN_LEVEL, MAX_LEVEL);
        int steps = l - MIN_LEVEL;
        int span = MAX_LEVEL - MIN_LEVEL;

        int total = clamp(MIN_TOTAL_ENEMIES + steps * ramp(MIN_TOTAL_ENEMIES, MAX_TOTAL_ENEMIES, span),
                MIN_TOTAL_ENEMIES, MAX_TOTAL_ENEMIES);
        int concurrent = clamp(
                MIN_CONCURRENT_ENEMIES + steps * (MAX_CONCURRENT_ENEMIES - MIN_CONCURRENT_ENEMIES) / span,
                MIN_CONCURRENT_ENEMIES, MAX_CONCURRENT_ENEMIES);
        long interval = Math.max(FASTEST_SPAWN_MS,
                SLOWEST_SPAWN_MS - steps * (SLOWEST_SPAWN_MS - FASTEST_SPAWN_MS) / span);

        return new RoundConfig(l, total, concurrent, interval);
    }

    private static int ramp(int min, int max, int span) {
        return (max - min) / span;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
