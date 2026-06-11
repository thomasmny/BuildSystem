# Plan 014: Finish constructor injection and delete the plugin singleton

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `grep -rn "BuildSystemPlugin.get()" buildsystem-core/src/main/java --include='*.java'`
> Record the current list — it is this plan's worklist. (At `67beca7` there were 14
> sites in 13 files; plans 003/004/008/010/013 should have removed several and added
> `// TODO(plan-014): inject` markers elsewhere.)

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED (initialization-order bugs surface at runtime, not compile time)
- **Depends on**: 003, 004, 008, 009, 010, 011, 013
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`BuildSystemPlugin.get()` is the last global. Every class that calls it has a
hidden dependency that defeats testing and hides initialization-order requirements
(it throws if called before `onLoad` sets `instance` —
`BuildSystemPlugin.java:219-224`). The architecture (000 §5) requires one
composition root and constructor-passed dependencies everywhere. Earlier plans
established the pattern; this plan hunts down the remainder and deletes
`instance`/`get()` so the pattern can never silently regress.

## Current state

At `67beca7`, `BuildSystemPlugin.get()` callers (13 files; several already
eliminated by earlier plans — your drift-check grep is authoritative):

| File | Why it uses the singleton | Strategy |
|---|---|---|
| `Messages.java` | static `PLUGIN` | gone via plan 004 |
| `config/Config.java` | static `PLUGIN` | gone via plan 003 |
| `world/data/WorldDataImpl.java` | folder resolution | gone via plan 010 |
| `util/FileUtils.java` | logger/data folder in static methods | add explicit `Plugin`/`Logger`/`Path` **parameters** to the static methods; callers pass theirs |
| `util/UUIDFetcher.java` | scheduler/logging in static fetch | same: parameterize, or convert to instance owned by `PlayerService` if it caches |
| `world/BuildWorldImpl.java` | creates its lifecycle helpers | constructor takes what it forwards (see step 2) |
| `world/util/WorldLoaderImpl.java` | plugin for scheduler/worlds | constructor parameter, passed by `BuildWorldImpl` |
| `world/util/WorldUnloaderImpl.java` | same | same |
| `world/util/WorldTeleporterImpl.java` | same | same |
| `world/util/WorldPermissionsImpl.java` | config/service lookups | constructor parameter or method parameter — pick per call shape (`of(buildWorld)` static factory may gain a context arg; check its ~20 callers first) |
| `world/display/FolderImpl.java` | service lookups | constructor parameter from `FolderStorageImpl` |
| `player/customblock/CustomBlock.java` | logging | parameterize |
| (the plugin class itself) | — | delete `instance`, `get()` |

Plus: every `// TODO(plan-014): inject` marker left by plans 003/004
(`grep -rn "TODO(plan-014)" buildsystem-core/src/main/java`), and
`util/UpdateChecker.java`'s `init(plugin, id)`/`get()` static-singleton pattern
(lines 77–111 region) → becomes a plain instance constructed in `onEnable` where
`performUpdateCheck` (BuildSystemPlugin.java:356) uses it.

Domain objects (`BuildWorldImpl`, `FolderImpl`) are constructed in storage/creator
code — find every `new BuildWorldImpl(`/`new FolderImpl(` site before starting and
list them in your working notes; those constructors grow parameters and all sites
must be updated coherently.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |
| Singleton gone | `grep -rn "BuildSystemPlugin.get()\|private static BuildSystemPlugin instance" buildsystem-core/src/main/java` | no matches |
| Markers gone | `grep -rn "TODO(plan-014)" buildsystem-core/src/main/java` | no matches |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §5 — the composition-root rules, including the
  construction-order sketch.

## Scope

**In scope**: the worklist table above (as updated by your drift-check grep), the
TODO markers, `UpdateChecker`, and `BuildSystemPlugin` itself (field order, final
fields where possible, getters only for what `BuildSystemApi` needs).

**Out of scope** (do NOT touch):
- Introducing a DI framework or service-locator object — plain constructors only
- `buildsystem-api` — no API change
- Behavior of any parameterized method

## Git workflow

- Conventional commits, one file-cluster per commit (utils; world lifecycle;
  folder; update checker; final singleton deletion).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Parameterize the static utilities

`FileUtils`, `UUIDFetcher`, `CustomBlock`: change each singleton-using method to
take the narrowest dependency (`Logger`, `JavaPlugin` for scheduler, `File` data
folder) as its first parameter; update callers (they all have a plugin/service in
scope after earlier plans — if one doesn't, its enclosing class gets the dependency
via constructor, recursively). Compile after each class.

### Step 2: World lifecycle helpers

`BuildWorldImpl` constructor gains the dependencies its helpers need and passes
them down to `WorldLoaderImpl`/`WorldUnloaderImpl`/`WorldTeleporterImpl`
(constructor parameters there; the `of(...)` static factories gain parameters or
become constructors). Update every `new BuildWorldImpl(` site (yaml deserialization
+ creator). `WorldPermissionsImpl.of(buildWorld)`: inspect callers — if they all
run in contexts with services available, add the needed context parameter; if the
permission logic only needs `BuildWorld` + `Player` + config snapshot, pass the
snapshot.

**Verify**: compile → exit 0 after each class; run tests.

### Step 3: `UpdateChecker`

Convert from `init/get` statics to a constructor-instance used in
`performUpdateCheck`. Keep the borrowed-code attribution comment (it credits an
external author — that is a license/courtesy comment, not noise).

### Step 4: Delete the singleton

Remove `instance`, `get()`, and the `instance = this`/`instance = null`
assignments from `BuildSystemPlugin` (`onLoad` line 141, `onDisable` line 216,
accessor lines 219–224 at `67beca7`). Compile — anything still calling `get()`
fails loudly now; fix stragglers by injection (never by re-adding the static).
Then tidy the composition root: fields `final` where construction order allows,
construction order matching 000 §5's sketch, getters trimmed to what
`BuildSystemApi` + cross-service wiring genuinely use.

**Verify**: all four table commands pass.

### Step 5: Tests

No new test surface (this plan removes a global; existing tests prove
non-regression). Add one compile-guard test only if cheap: a reflection test
asserting `BuildSystemPlugin` has no static fields of its own type.

## Test plan

Existing suite green; the singleton-gone grep is the real gate. Operator smoke:
full server start/stop cycle, `/config reload`, world create — watching for
initialization-order exceptions in the log.

## Done criteria

- [ ] All four table commands pass
- [ ] `UpdateChecker` no longer has `init`/`get` statics
- [ ] `BuildSystemPlugin` has no static self-reference; service fields final where
      possible
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- A `get()` caller runs in a context where no injection path exists without
  changing an API-frozen constructor signature — report the chain.
- Initialization order genuinely requires lazy access (A needs B, B needs A) —
  report the cycle; do not hide it behind a supplier without sign-off.
- The drift-check grep shows > 25 remaining sites (earlier plans under-delivered;
  the effort estimate is invalid — re-scope before proceeding).

## Maintenance notes

- From here on, "how do I get X?" has one answer: constructor. Reviewers should
  reject any new static mutable state in core.
- If a future feature genuinely needs static access (e.g. a Bukkit API callback
  with no context), route it through the composition root explicitly and document
  why.
