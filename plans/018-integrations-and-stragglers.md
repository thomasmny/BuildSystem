# Plan 018: Integration modules and remaining straggler classes

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/expansion buildsystem-core/src/main/java/de/eintosti/buildsystem/event buildsystem-core/src/main/java/de/eintosti/buildsystem/world/display/CustomizableIcons.java buildsystem-core/src/main/java/de/eintosti/buildsystem/util/PlayerChatInput.java`
> Expected drift: receiver swaps (003/004) and plan 015's event fix only. On
> structural mismatch with the excerpts: STOP.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW (isolated modules; soft-dependency class-loading is the only trap)
- **Depends on**: 003, 004, 009, 014
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

This plan gives every class the main series didn't claim an explicit home and a
modern shape, so plan 016's conformance pass closes a *complete* set. Concretely:
the optional-plugin integrations (`expansion/`) move into `integration/` with
constructor injection and a uniform activation pattern; and four straggler classes
(`CustomizableIcons`, `PlayerChatInput`, `EventDispatcher`, `util/color/ColorAPI`)
get explicitly placed and cleaned rather than swept blind. Without this plan these
files would only ever be touched by the mechanical sweep — i.e., never actually
reviewed.

## Current state

(Verified at `67beca7`.)

- `expansion/placeholderapi/PlaceholderApiExpansion.java` (208) —
  `onPlaceholderRequest` (112) dispatches; settings placeholders resolve via a
  large `switch (settingIdentifier.toLowerCase(...))` (136–~150: navigatortype,
  glasscolor, worldsort, clearinventory, …) and world placeholders via a second
  dispatch (read the rest of the file). Registered from
  `BuildSystemPlugin.registerExpansions()` (line 322) when PlaceholderAPI present.
- `expansion/luckperms/LuckPermsExpansion.java` (66) + `calculators/`
  `BuildModeCalculator` (52), `RoleCalculator` (96) — context calculators,
  `registerAll`/`unregisterAll` from the plugin class.
- `event/EventDispatcher.java` — helper that decides whether a Bukkit interaction
  constitutes "world manipulation" and fires `BuildWorldManipulationEvent`
  (line 71). `event/player/PlayerInventoryClearEvent.java` — internal event.
- `world/display/CustomizableIcons.java` (137) — loads/serves the configurable
  navigator/status icons from `setup.yml` (constructed first in `initClasses()`,
  `BuildSystemPlugin.java:227`); paired with `YamlSetupStorage` and
  `SetupInventory`.
- `util/PlayerChatInput.java` — chat-prompt helper used by menus and world
  creation flows.
- `util/color/ColorAPI.java` (232) + `patterns/` — hex/gradient/rainbow color
  engine for messages; attributed external-ish code style, consumed by `i18n`.
- After plan 009 there is already an `integration/worldedit/WorldEditCommands.java`;
  after plan 014 nothing here uses `BuildSystemPlugin.get()`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.9 (integration rules), §4 (tree), §8.

## Scope

**In scope**:
- `git mv` `expansion/placeholderapi` → `integration/placeholderapi`,
  `expansion/luckperms` → `integration/luckperms`; delete the empty `expansion/`
- Internal cleanup of `PlaceholderApiExpansion` (step 2)
- A uniform activation seam in the composition root (step 3)
- Place the stragglers (step 4): `CustomizableIcons` → `world/display/` stays but
  gets constructor injection + a one-line class Javadoc stating its role;
  `PlayerChatInput` → `menu/` (it is a UI input mechanism — its consumers are
  menus/creation flows); `EventDispatcher` → stays in `event/`, gains injection;
  `util/color/` stays (cohesive engine), modernized only
- Listener `WorldManipulateByAxiomListener` → `integration/axiom/` (it exists only
  when AxiomPaper is present; same class-loading rule), `EditSessionListener` →
  `integration/worldedit/` (same reasoning)

**Out of scope** (do NOT touch):
- Placeholder identifiers and values — every `%buildsystem_…%` placeholder must
  resolve to exactly the same string as before
- LuckPerms context keys/values
- `ColorAPI`'s color math
- The conditional-registration *logic* (plan 009's registrar owns it; this plan
  only moves the classes it registers)

## Git workflow

- Conventional commits; `git mv` for every move (history must follow).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Moves

Execute the moves listed in scope with `git mv`, fix package/imports, compile.
The class-loading rule (000 §6.9) must hold afterwards: classes importing
PlaceholderAPI/LuckPerms/WorldEdit/Axiom types are referenced **only** from the
activation seam (verify: `grep -rn "integration\." buildsystem-core/src/main/java --include='*.java' | grep -v "integration/"` → only the composition root / registrar).

### Step 2: `PlaceholderApiExpansion` cleanup

Split `onPlaceholderRequest` into named private methods per placeholder family
(settings switch → `settingsPlaceholder(...)`, world-data dispatch →
`worldPlaceholder(...)` — follow the file's actual structure after reading it
fully). No method > 50 lines. Keep the switch expressions (they are already
modern); the goal is navigability, not rewriting. While here: constructor takes
the services it reads (`PlayerService`, `WorldService`, `ConfigService`) instead
of the plugin where feasible.

### Step 3: Activation seam

In the composition root, today's `registerExpansions()`/`unregisterExpansions()`
(BuildSystemPlugin.java:322–354) becomes one `Integrations` class in
`integration/` owning: presence checks, activate-all, deactivate-all. Each
integration exposes `register()`/`unregister()` (PAPI + LuckPerms already do).
`onEnable`/`onDisable` call `integrations.activate()` / `.deactivate()`.

### Step 4: Stragglers

- `CustomizableIcons`: constructor injection audit (it currently takes `plugin`),
  read its load/save path (paired with `YamlSetupStorage`) and ensure plan 017's
  shutdown-join covers setup saves; add the missing class-level Javadoc.
- `PlayerChatInput`: `git mv` to `menu/`; verify it cleans its listener/state when
  the player quits mid-prompt (read it — if it leaks a conversation/listener on
  quit, fix with the same pattern it uses for completion; report what you found).
- `EventDispatcher`: inject dependencies; confirm plan 015 gave
  `BuildWorldManipulationEvent` its own `HandlerList`.
- `util/color/`: apply the 000 §8 modernization rules mechanically; keep
  attribution comments.

### Step 5: Tests

- `integration/placeholderapi` is mock-testable: `PlaceholderApiExpansionTest`
  asserting 5+ known identifiers resolve against mocked settings/world data
  (e.g. `settings_navigatortype`, a world `%buildsystem_world_status%`-family
  placeholder — use the real identifier strings from the file).
- No tests for LuckPerms calculators (require LuckPerms types; compile-only).

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 5. Operator smoke (needs PAPI installed): `%buildsystem_settings_navigatortype%`
and one world placeholder via `/papi parse me …`; LuckPerms context visible in
`/lp user … contexteditor`; Axiom/WorldEdit-gated listeners register only when the
plugins are present (check startup log on a bare server).

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `expansion/` gone; `integration/{placeholderapi,luckperms,worldedit,axiom}/` populated
- [ ] Class-loading rule grep (step 1) passes
- [ ] `PlaceholderApiExpansion`: no method > 50 lines
- [ ] Straggler table from step 4 fully executed (or deviations reported)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Moving a soft-dependency class changes when its imports are class-loaded in a
  way you cannot verify (e.g. a moved class is referenced from an always-loaded
  class) — report the reference chain.
- `PlayerChatInput` has lifecycle issues beyond a quit-leak (e.g. overlapping
  prompts corrupt each other) — report, don't redesign inline.
- Any placeholder/context identifier would change.

## Maintenance notes

- New optional-plugin support = new `integration/<name>/` package + one
  `Integrations` entry; nothing else may import its types.
- The PAPI placeholder set is user-facing API in practice — treat identifier
  changes like API breaks.
