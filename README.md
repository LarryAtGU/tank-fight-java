# Tank Fight

A local 2-player tank battle game built with Java Swing.

## Requirements

- JDK 25 or later

## Setup & Run

Clone the repo, then from the project root:

```bash
javac -d out $(find src -name "*.java")
java -cp out tankfight.Main
```

This compiles all sources into `out/` and launches the game window.

## Controls

| Action        | Player 1 (blue) | Player 2 (red) |
|---------------|------------------|-----------------|
| Move up       | `W`              | `↑`             |
| Move down     | `S`              | `↓`             |
| Move left     | `A`              | `←`             |
| Move right    | `D`              | `→`             |
| Fire          | `Space`          | `Enter`         |

When a tank's health reaches zero, the match ends — press `R` to restart.
