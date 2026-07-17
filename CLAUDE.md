# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

TankFight is an IntelliJ IDEA-managed Java project (JDK 25) for a local 2-player tank battle game built with Swing. It follows a Model-View-Controller split (`tankfight.model` / `tankfight.view` / `tankfight.controller`), with unit tests covering the model and controller layers.

## Commands

There is no Maven/Gradle wrapper — this is a plain IntelliJ module (`TankFight.iml`) using the Maven-style `src/main/java` / `src/test/java` directory convention, built with plain `javac`/`java`.

- Compile: `javac -d out $(find src/main/java -name "*.java")`
- Run: `java -cp out tankfight.Main`
- Tests use JUnit 5 (Jupiter), vendored as a standalone jar (not committed — gitignored via `lib/*.jar`; fetch it with the curl command in `README.md`) rather than pulled via a build tool:
  ```
  javac -cp "out:lib/junit-platform-console-standalone-6.1.2.jar" -d out-test $(find src/test/java -name "*.java")
  java -jar lib/junit-platform-console-standalone-6.1.2.jar execute --class-path "out:out-test" --scan-class-path
  ```

## Architecture

**`tankfight.model`** — pure game state and rules, no Swing/AWT-rendering dependency (only `java.awt.Rectangle` for geometry, which is headless-safe).
- `Entity` (abstract) — base class for anything with position/size (`x`, `y`, `width`, `height`, `getBounds()`); `setPosition` is package-private so only `GameModel` (same package) can move entities.
- `Movable` / `Damageable` (interfaces) — `Tank` implements both; `Bullet` implements `Movable` only.
- `Tank`, `Bullet`, `Wall` — entities. `Tank` holds a `Player` (`ONE`/`TWO`), not a `Color` — rendering color is a view concern, decided in `GameRenderer`.
- `PlayerAction` (record) — one tick's input intent for one player (`moveDirection` nullable, `fire` boolean); this is the sole channel through which the controller talks to the model.
- `GameModel` — owns both tanks, the wall layout, and the live bullet list; `update(PlayerAction, PlayerAction, long now)` runs one tick of movement/collision/firing/win-condition logic. This is the class most worth unit-testing directly, since it has no Swing dependency.

**`tankfight.view`** — rendering only, takes a `GameModel` and draws it; never mutates game state.
- `GameView` (interface) — `void refresh()`, the abstraction the controller uses to trigger a repaint without depending on Swing directly.
- `GameRenderer` (package-private) — draws walls/bullets/tanks/HUD/game-over overlay onto a `Graphics2D`, given a `GameModel`. Owns the player→color mapping.
- `GamePanel` — `JPanel implements GameView`; delegates `paintComponent` to `GameRenderer`.
- `GameWindow` — `JFrame` wrapper; does not call `setVisible(true)` itself, that's left to the composition root.

**`tankfight.controller`** — input handling and the game loop timer.
- `InputSource` (interface) — `boolean isPressed(int keyCode)`, decouples the controller from raw `KeyEvent`s; `KeyboardInput` is the real `KeyListener`-backed implementation.
- `KeyBindings` (record) — one player's up/down/left/right/fire key codes.
- `GameController` — owns a `javax.swing.Timer` (60 FPS); each tick converts `InputSource` + `KeyBindings` into `PlayerAction`s, calls `GameModel.update(...)`, and calls `GameView.refresh()`. The restart key triggers `GameModel.reset()` while `isGameOver()` is true. `tick()` is package-private specifically so tests can invoke it directly without waiting on the real `Timer`.

**`tankfight.Main`** — the composition root. Constructs the model, view, and controller and wires them together (this is the only place all three packages meet); nothing else in the codebase should depend on more than one of `model`/`view`/`controller` at a time.

This structure was built by parallel subagents against a contract frozen up front (exact class/method signatures decided before any implementation started), so if you're extending it, keep changes to one layer's public API accompanied by a check of the other two layers that depend on it.
