# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

TankFight is an IntelliJ IDEA-managed Java project (JDK 25) for a Battle-City-style tank battle game built with Swing. Package/folder scaffolding is in place under `src/tankfight/`, but all classes are currently empty stubs awaiting real game logic.

## Commands

There is no Maven/Gradle wrapper — this is a plain IntelliJ module (`TankFight.iml`) with source files under `src/`.

- Compile: `javac -d out $(find src -name "*.java")`
- Run: `java -cp out tankfight.Main`
- No test framework is configured yet.

## Architecture notes

- All code lives under the `tankfight` package. Subpackages:
  - `tankfight` — `Main.java`, the entry point (standard `public class Main` with `public static void main(String[] args)`)
  - `tankfight.game` — `GameWindow` (JFrame) and `GamePanel` (JPanel, render loop, game state)
  - `tankfight.entities` — `Tank`, `Bullet`, `Wall`
  - `tankfight.input` — `KeyHandler` (keyboard input)
  - `tankfight.util` — `Direction` (movement enum)
- All of the above are currently empty stubs (no fields, methods, or logic yet).
- `Main.java` was converted from JDK 25's compact source file style (JEP 512, no class wrapper) to a standard class-based entry point, since it now needs to live in a package and compile/run alongside the rest of the game via `javac`/`java -cp`.
