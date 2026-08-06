# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

TankFight is an IntelliJ IDEA-managed Java project (JDK 25) for a co-op tank defence game built with Swing: one or two allied players (each human or AI) against a round's worth of AI-driven enemy tanks. It follows a Model-View-Controller split (`tankfight.model` / `tankfight.view` / `tankfight.controller`), with unit tests covering the model and controller layers.

## Commands

There is no Maven/Gradle wrapper — this is a plain IntelliJ module (`TankFight.iml`) using the Maven-style `src/main/java` / `src/test/java` directory convention, built with plain `javac`/`java`.

- Compile: `javac -d out $(find src/main/java -name "*.java") && cp -r src/main/resources/* out/`
- Run: `java -cp out tankfight.Main`
- Tests use JUnit 5 (Jupiter), vendored as a standalone jar (not committed — gitignored via `lib/*.jar`; fetch it with the curl command in `README.md`) rather than pulled via a build tool:
  ```
  javac -cp "out:lib/junit-platform-console-standalone-6.1.2.jar" -d out-test $(find src/test/java -name "*.java")
  java -jar lib/junit-platform-console-standalone-6.1.2.jar execute --class-path "out:out-test" --scan-class-path
  ```

## Gameplay rules

Enemies spawn from three entry points on the top edge and roam/fire randomly; allies defend from the bottom. Bullets only damage tanks whose `Side` differs from their own, so friendly fire is impossible in both directions — and an allied bullet passes *through* a teammate rather than being absorbed by them. Destroying an enemy scores `GameModel.POINTS_PER_KILL` for the player who fired, tracked per player and as a team total. A round is won when every enemy in it has been destroyed and lost when every ally has; there are no ally respawns.

## Architecture

**`tankfight.model`** — pure game state and rules, no Swing/AWT-rendering dependency (only `java.awt.Rectangle` for geometry, which is headless-safe).
- `Entity` (abstract) — base class for anything with position/size (`x`, `y`, `width`, `height`, `getBounds()`); `setPosition` is package-private so only `GameModel` (same package) can move entities. Tests in `tankfight.model` rely on this to park tanks at fixed spots.
- `Movable` / `Damageable` (interfaces) — `Tank` implements both; `Bullet` implements `Movable` only.
- `Side` (enum) — `ALLIES` / `ENEMIES`. The single thing all friendly-fire and targeting rules key off.
- `Tank`, `Bullet`, `Wall` — entities. Both `Tank` and `Bullet` carry a `Side`; `Tank` also carries a nullable `Player` (the ally slot it fills, `null` for enemies) and `Bullet` carries the nullable `Player` to credit kills to. Build tanks through `Tank.ally(...)` / `Tank.enemy(...)`, which apply the two stat profiles (allies are tougher, faster, and reload quicker). Rendering is a view concern, decided in `GameRenderer`.
- `PlayerAction` (record) — one tick's intent for one tank (`moveDirection` nullable, `fire` boolean). Both human input and AI produce these, so the model applies both through one code path and neither side gets abilities the other lacks.
- `TankAi` (interface) + `RandomTankAi` / `AllyTankAi` — enemy behaviour (stateless wander-and-shoot; the tank's own `getDirection()` is its "keep going" memory) and AI-player behaviour (close on the nearest enemy, fire when aligned). Both take a `Random` so they're reproducible in tests.
- `RoundConfig` (record) — a round's difficulty: total enemies, max concurrent, spawn interval. `forLevel(int)` ramps levels 1–8 from 15/3 to 50/8; the bounds live here as constants.
- `GameSetup` — the menu's mutable state (player count, per-player `ControlType`, level, cursor row). Lives in the model, not the view, so menu navigation is testable headlessly; the controller only calls `moveCursor` / `adjust`.
- `Phase` (`MENU` / `PLAYING` / `ROUND_OVER`) and `RoundOutcome` (`VICTORY` / `DEFEAT`) — the screen state machine, owned by `GameModel`.
- `GameModel` — owns the ally list, the live enemy list, walls, bullets, scores, and spawn budgeting. `update(PlayerAction, PlayerAction, long now)` runs one tick and is a no-op outside `PLAYING`. `startRound()` builds a round from the setup; `startRound(RoundConfig)` takes an explicit difficulty (tests use it to pin a round's shape). `canMove(Tank, Direction)` is public so `TankAi` implementations steer without duplicating collision rules. Destroyed enemies leave `getEnemies()`; destroyed allies stay in `getAllies()` with zero health so the HUD keeps their slot. This is the class most worth unit-testing directly, since it has no Swing dependency — and the seeded `GameModel(Random, TankAi)` constructor makes spawning and enemy behaviour deterministic.

**`tankfight.view`** — rendering only, takes a `GameModel` and draws it; never mutates game state.
- `GameView` (interface) — `void refresh()`, the abstraction the controller uses to trigger a repaint without depending on Swing directly.
- `GameRenderer` (package-private) — draws the menu, the play field, the HUD strip and the round-over overlay onto a `Graphics2D`, given a `GameModel`. Every tank on the field carries a small health bar above it, drawn as a pass *after* all the sprites so a tank driving past a neighbour can't cover the neighbour's bar, and on the unrotated graphics so bars stay level. The panel is taller than the field (`PANEL_HEIGHT` = `GameModel.HEIGHT` + `HUD_HEIGHT`) so the HUD never covers the enemy entry points along the top edge. Owns the tank→sprite mapping and loads tank/wall/bullet PNGs from `src/main/resources/images` via classpath (`getResourceAsStream("/images/...")`) — the compile command copies that directory into `out/` so the resources are on the runtime classpath. Player two's green tank is hue-shifted from the blue sprite at class-init (`recolor`), so there's no third PNG to keep in sync.
- `GamePanel` — `JPanel implements GameView`; delegates `paintComponent` to `GameRenderer` and sizes itself from `GameRenderer.PANEL_WIDTH/HEIGHT`.
- `GameWindow` — `JFrame` wrapper; does not call `setVisible(true)` itself, that's left to the composition root.

**`tankfight.controller`** — input handling and the game loop timer.
- `InputSource` (interface) — `isPressed(int)` (level-triggered, for driving tanks) and `consumePress(int)` (edge-triggered and consumed by the call, for menu navigation so a held key doesn't race through options at 60 FPS); `KeyboardInput` is the real `KeyListener`-backed implementation and filters OS key-repeat out of `consumePress`.
- `KeyBindings` (record) — one player's up/down/left/right/fire key codes.
- `ActionProvider` (interface) + `HumanActionProvider` / `AiActionProvider` — where a player slot's `PlayerAction` comes from. Swapping the implementation is the whole mechanism behind "either player can be human or AI"; the model never learns which.
- `GameController` — owns a `javax.swing.Timer` (60 FPS) and dispatches each tick on `model.getPhase()`: menu navigation, one `GameModel.update(...)`, or the restart/menu keys. The abort key (`Esc`) is checked *before* the update so an abandoned round doesn't advance one last frame; it works in both `PLAYING` and `ROUND_OVER`. Providers are re-bound from `GameSetup` on every `startRound()`, so changing a slot to AI takes effect on the next round. `tick()` is package-private specifically so tests can invoke it directly without waiting on the real `Timer`.

**`tankfight.Main`** — the composition root. Constructs the model, view, and controller and wires them together (this is the only place all three packages meet); nothing else in the codebase should depend on more than one of `model`/`view`/`controller` at a time.

If you're extending this, keep changes to one layer's public API accompanied by a check of the other two layers that depend on it.
