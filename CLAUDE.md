# BuildSystem — Claude Code guide

## Build / test / verify

| Purpose    | Command                                                              | Expect        |
|------------|----------------------------------------------------------------------|---------------|
| Compile    | `./gradlew :buildsystem-core:compileJava`                            | exit 0        |
| Tests      | `./gradlew :buildsystem-core:test`                                   | exit 0        |
| Full build | `./gradlew build`                                                    | exit 0 + jar  |

Java 25 toolchain via `buildSrc`; version catalog at `gradle/libs.versions.toml`.

## Architecture pointer

Read `ARCHITECTURE.md` (repo root) before making structural changes. It describes
the as-built package tree, composition-root construction order, extension recipes,
and threading rules.

## Three iron rules

1. **API freeze is two-phase**: `buildsystem-api` is pre-1.0 with **0 external
   consumers**, so signatures may currently be changed, renamed, or removed freely.
   Once the first external release ships, the freeze applies: signatures never removed,
   deprecate first (`@Deprecated(forRemoval=true, since="<version>")` + `@deprecated`
   Javadoc). New API members always carry useful Javadoc and `@since`.

2. **Bukkit main-thread**: all Bukkit API calls (worlds, players, inventories,
   blocks, scoreboards) must run on the server main thread. File IO and network
   calls must be async. Hop pattern: validate on main → async IO → sync apply.

3. **No static mutable state**: services are injected from the composition root
   (`BuildSystemPlugin.onEnable()`). `BuildSystemPlugin.get()` does not exist.
   Singletons die here.

## Comment policy

- No comments that restate what the code does.
- Javadoc is mandatory on all public API (`buildsystem-api`); optional on internals.
- Add a comment only when the WHY is non-obvious: a hidden constraint, a subtle
  invariant, a workaround for a known bug.

## Commit style

Conventional commits: `refactor: ...`, `fix: ...`, `test: ...`, `docs: ...`,
`chore: ...`. One commit per logical change.

## `plans/` directory

`plans/` is git-ignored and must never be committed. It holds the implementation
plan series (000–018) that drove the 2026 refactor. `plans/README.md` has the
execution order and status of each plan. `plans/000-target-architecture.md` is
the design reference.
