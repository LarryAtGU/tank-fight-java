# Tank Fight

A co-op tank defence game built with Java Swing. One or two allied players — each either a
person at the keyboard or the computer — hold off waves of AI enemy tanks.

## Requirements

- JDK 25 or later

## Setup & Run

Clone the repo, then from the project root:

```bash
javac -d out $(find src/main/java -name "*.java") && cp -r src/main/resources/* out/
java -cp out tankfight.Main
```

This compiles all sources into `out/`, copies the tank/wall/bullet sprite images onto the classpath, and launches the game window.

## Running the tests

The project uses JUnit 5 (Jupiter), vendored as a standalone jar rather than pulled via Maven/Gradle (there's no build tool wrapper in this repo). Fetch it once:

```bash
mkdir -p lib
curl -o lib/junit-platform-console-standalone-6.1.2.jar \
  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.2/junit-platform-console-standalone-6.1.2.jar
```

Then compile and run the suite:

```bash
javac -d out $(find src/main/java -name "*.java")
javac -cp "out:lib/junit-platform-console-standalone-6.1.2.jar" -d out-test $(find src/test/java -name "*.java")
java -jar lib/junit-platform-console-standalone-6.1.2.jar execute --class-path "out:out-test" --scan-class-path
```

## How to play

The game opens on a setup menu where you choose how many players are in the round (1 or 2),
whether each one is `HUMAN` or `COMPUTER`, and which level to play. Either player's keys can
drive the menu.

| Menu action | Keys                    |
|-------------|-------------------------|
| Select row  | `W`/`S` or `↑`/`↓`      |
| Change value| `A`/`D` or `←`/`→`      |
| Start round | `Space` or `Enter`      |

Both players are on the same side. Enemy tanks enter from three points along the top edge —
top-left, top-middle and top-right — then roam and fire at random. You score 100 points for
every enemy tank you destroy, tracked per player and as a team total.

Every tank on the field — yours and the enemy's — carries a health bar above it, turning red
below 30%, so you can see how close anything is to being destroyed. The strip along the bottom
repeats each player's health alongside their score and the round's progress.

**Friendly fire is off in both directions.** Allied bullets pass straight through allies, and
enemies can't hit each other either, so a teammate in your line of fire is never a problem.

- **Round cleared** when every enemy tank in the round has been destroyed.
- **Defeat** when both allies have been destroyed. There are no respawns — a destroyed ally is
  out for the rest of the round.

## Controls

| Action        | Player 1 (blue) | Player 2 (green) |
|---------------|-----------------|------------------|
| Move up       | `W`             | `↑`              |
| Move down     | `S`             | `↓`              |
| Move left     | `A`             | `←`              |
| Move right    | `D`             | `→`              |
| Fire          | `Space`         | `Enter`          |

Press `Esc` at any time during a round to abandon it and go back to the setup menu.

When the round ends, press `R` to replay the same level, or `M` (or `Esc`) to return to the menu.

## Levels

Level number sets the round's shape: how many enemy tanks it fields in total, how many may be
on screen at once, and how fast they arrive. Levels 1–8 ramp from 15 enemies (3 at a time) up
to 50 enemies (8 at a time). See `RoundConfig.forLevel(int)`.
