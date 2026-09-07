# Repository Guidelines

## Project Structure & Module Organization

This repository ports Create Simulated to Minecraft 1.20.1 Fabric for Homestead. `settings.gradle.kts` includes only `simulated:fabric` in the active build.

- `simulated/fabric/src/main/java/dev/simulated_team/simulated/`: active Java code, organized into content, registries (`index`), networking, mixins, and Fabric integration.
- `simulated/fabric/src/main/resources/`: Fabric metadata and handwritten resources; `src/main/generated/` contains generated assets and data.
- `simulated/common/`: upstream reference code and shared assets copied during resource processing. Its Java sources are not compiled by this port.
- `simulated/neoforge/`, `aeronautics/`, and `offroad/`: upstream implementations and future port references.
- `sable-backport/`: physics backport preparation tools; `tools/`: migration and resource validation scripts.
- `FABRIC_PORT_PLAN.md`: milestones, deviations, and outstanding defects; `CLAUDE.md`: local development and release workflow.

## Build, Test, and Development Commands

Use Java 17 and the Gradle wrapper from the repository root. These examples use PowerShell; on Unix, replace `.\gradlew.bat` with `./gradlew`.

- `.\gradlew.bat :simulated:fabric:build` — compile and package jars in `simulated/fabric/build/libs/`.
- `.\gradlew.bat :simulated:fabric:runClient` — launch the development client.
- `.\gradlew.bat :simulated:fabric:runServer` — launch the development dedicated server.
- `.\gradlew.bat :simulated:fabric:runDatagen` — regenerate resources after registration changes.
- `python tools/check_resources.py` — check model, texture, sound, and translation references after building; requires dependency jars in the Gradle cache.
- `python tools/mixin_audit/run.py` — check every mixin still matches the 1.20.1 class it targets (signatures, injection points, `@Local` ordinals). Run after touching anything under `mixin/`; a mismatch here is a crash at game start, and Mixin only reports the first one.

## Coding Style & Naming Conventions

Follow adjacent Java code: four-space indentation, same-line opening braces, `PascalCase` classes, `camelCase` members, and `UPPER_SNAKE_CASE` constants. Use lowercase snake_case resource IDs and feature packages. Preserve existing `Sim` registry naming. No automated formatter or linter is configured. Change generators rather than manually patching generated JSON.

## Testing Guidelines

Run the build and resource checker before submitting. Upstream Minecraft GameTests (`*Test.java`) live under `simulated/neoforge/.../gametest`; they are outside the active Fabric build. No Fabric unit-test suite or coverage threshold is configured. Validate gameplay in Homestead and test networking, persistence, and world-state changes on a dedicated server. Record reproduction steps and results; compilation alone does not establish gameplay correctness.

## Commit & Pull Request Guidelines

Recent commits use short imperative subjects, such as `Register Fabric steering wheel helm`. Keep changes focused. Work on `fabric-homestead-1.20.1`; treat `main` as upstream reference. Commit and push only when requested. PRs should explain behavior, link relevant issues, list validation results, and include screenshots for visual changes. Update the port plan for deviations. Preserve unrelated working-tree changes and verify Homestead compatibility before changing pinned dependencies.
