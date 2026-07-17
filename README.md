# Tank Fight

A local 2-player tank battle game built with Java Swing.

## Requirements

- JDK 25 or later

## Setup & Run

Clone the repo, then from the project root:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out tankfight.Main
```

This compiles all sources into `out/` and launches the game window.

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

## Controls

| Action        | Player 1 (blue) | Player 2 (red) |
|---------------|------------------|-----------------|
| Move up       | `W`              | `↑`             |
| Move down     | `S`              | `↓`             |
| Move left     | `A`              | `←`             |
| Move right    | `D`              | `→`             |
| Fire          | `Space`          | `Enter`         |

When a tank's health reaches zero, the match ends — press `R` to restart.
