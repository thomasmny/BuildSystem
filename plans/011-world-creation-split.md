# Plan 011: Split BuildWorldCreatorImpl into creation strategies and collaborators

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/world/creation`
> Expected drift: receiver swaps from plans 003/004 only. Structural drift: STOP.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED (world creation touches disk and Bukkit world gen; behavior must
  be byte-identical for level.dat handling)
- **Depends on**: 003, 004, 010
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`world/creation/BuildWorldCreatorImpl.java` is 611 lines doing seven jobs: fluent
builder state, creation orchestration, generator-based creation, template-based
creation, Bukkit `WorldCreator` assembly + world settings, NBT `level.dat`
data-version guarding, and per-world generation-metadata persistence. Every
world-creation bug fix wades through all seven. The seams are already visible as
method clusters — this plan cuts along them without changing the public
`BuildWorldCreator` API (which is part of `buildsystem-api`).

## Current state

`BuildWorldCreatorImpl` method map (verified at `67beca7`):

| Lines | Cluster | Methods |
|---|---|---|
| 96–177 | builder state | ctors; `setName/setCreator/setTemplate/setType/setCustomGenerator/setFolder/setPrivate/setDifficulty/setCreationDate` |
| 180–209 | orchestration | `createWorld(Player)` (180), `importWorld(Player, boolean)` (197) |
| 211–284 | sources | `createWorldFromGenerator` (211), `createWorldFromTemplate` (228), `createAndRegisterBuildWorld` (258) |
| 286–428 | Bukkit factory | `getServerDataVersion` (286), `generateBukkitWorld(boolean)` (292), `createBukkitWorldCreator` (322), `applyDefaultWorldSettings` (390), `applyGameRule` (404), `applyPostGenerationSettings` (414) |
| 430–493 | data-version guard | `isDataVersionTooHigh` (430), `parseDataVersion` (438), `updateWorldDataVersion` (455) |
| 495–586 | generation metadata | `saveGenerationData` (495), `renameIncorrectWorldTypeFile` (528), `loadGenerationData` (554) |
| 588–611 | post-creation | `teleportAfterCreation` (588), anonymous `CustomGenerator` (607) |

- `api/world/creation/BuildWorldCreator` — the API interface the fluent surface
  implements; **unchanged** by this plan.
- `generateBukkitWorld` is also called from outside (e.g.
  `WorldServiceImpl.renameWorld` — `new BuildWorldCreatorImpl(plugin, buildWorld).generateBukkitWorld(false)`).
  Find all external callers first:
  `grep -rn "BuildWorldCreatorImpl\|generateBukkitWorld" buildsystem-core/src/main/java --include='*.java'`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.6.

## Scope

**In scope** — new classes in `world/creation/`:
- `WorldDataVersionGuard.java` (the 430–493 cluster + `getServerDataVersion` +
  `renameIncorrectWorldTypeFile` — all `level.dat`/NBT logic)
- `GenerationDataStore.java` (`saveGenerationData`/`loadGenerationData` + the
  anonymous `CustomGenerator`)
- `BukkitWorldFactory.java` (`generateBukkitWorld`, `createBukkitWorldCreator`,
  `applyDefaultWorldSettings`, `applyGameRule`, `applyPostGenerationSettings`)
- `BuildWorldCreatorImpl.java` shrinks to: builder state + `createWorld`/
  `importWorld` orchestration + `createWorldFromGenerator`/`createWorldFromTemplate`
  (which become thin: validate → delegate to factory/store) + `teleportAfterCreation`
- External `generateBukkitWorld` callers switch to `BukkitWorldFactory`

**Out of scope** (do NOT touch):
- `api/world/creation/**` — the API contract is frozen
- `CreateInventory` / `startWorldNameInput` UX flow
- The actual NBT logic semantics — `WorldDataVersionGuard` is a *move*, not a
  rewrite; the `dev.dewy.nbt` usage moves verbatim
- `world/creation/generator/**` (custom generator resolution — only its call sites
  move)

## Git workflow

- Conventional commits, one extraction per commit, `BuildWorldCreatorImpl` shrink
  last.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Extract `WorldDataVersionGuard`

Move `isDataVersionTooHigh`, `parseDataVersion`, `updateWorldDataVersion`,
`getServerDataVersion`, `renameIncorrectWorldTypeFile` into
`world/creation/WorldDataVersionGuard.java` (constructor: `(BuildSystemPlugin plugin)`
or just the logger + world-container path — take the minimum the bodies need).
Method bodies move verbatim; only field references become parameters/fields.
`BuildWorldCreatorImpl` keeps delegating so the API method `isDataVersionTooHigh()`
(check if it's on the API interface) still works.

**Verify**: compile → exit 0.

### Step 2: Extract `GenerationDataStore`

Same procedure for `saveGenerationData`/`loadGenerationData` and the anonymous
`CustomGenerator` (line 607) it returns. Note what storage medium these use (read
the bodies — file-based metadata next to the world or inside plugin data) and keep
paths identical.

**Verify**: compile → exit 0.

### Step 3: Extract `BukkitWorldFactory`

Move the Bukkit factory cluster. `generateBukkitWorld(boolean checkVersion)`'s
version check calls the guard from step 1 — the factory takes the guard as a
constructor dependency. Update external callers (found in "Current state") to use
the factory directly; `WorldServiceImpl.renameWorld`'s
`new BuildWorldCreatorImpl(plugin, buildWorld).generateBukkitWorld(false)` becomes a
factory call — no more constructing a creator just to regenerate a world.

**Verify**: compile → exit 0;
`grep -rn "new BuildWorldCreatorImpl" buildsystem-core/src/main/java | grep -v "creation/"` → only legitimate creator (not regenerate) usages remain — list them in the report.

### Step 4: Shrink the orchestrator

`BuildWorldCreatorImpl` retains builder state + orchestration; wherever a moved
method was private-called, call the collaborator. Target: file ≤ ~250 lines, no
method > 50 lines. Modernize while in there per 000 §8 (switch expressions etc.) —
but no behavior change.

**Verify**: `wc -l .../BuildWorldCreatorImpl.java` → ≤ 250; compile → exit 0.

### Step 5: Tests

- `WorldDataVersionGuardTest`: `parseDataVersion` against a fixture `level.dat`
  (create a minimal NBT file in test resources using the same `dev.dewy.nbt`
  library in a `@BeforeAll`; if crafting a valid fixture exceeds ~30 lines of test
  setup, test only `getServerDataVersion`'s parsing of `Bukkit.getVersion()`-style
  strings with mocked input and note the gap).
- `GenerationDataStoreTest`: round-trip save → load in `@TempDir`.

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 5. Operator smoke: create one world of each type (normal, flat, void,
template, custom generator), import a world, verify the data-version warning still
appears when importing a newer-version world.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] Three new collaborator classes exist; `BuildWorldCreatorImpl` ≤ ~250 lines
- [ ] API surface unchanged: `git diff 67beca7..HEAD -- buildsystem-api` shows no
      signature changes from this plan
- [ ] No external caller constructs a creator solely to call `generateBukkitWorld`
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `isDataVersionTooHigh` (or any moved method) is declared on the API
  `BuildWorldCreator` interface AND its body cannot delegate cleanly.
- The NBT cluster references creator builder-state fields beyond
  name/type/template (hidden coupling the line map missed).
- Template creation (`createWorldFromTemplate`, line 228) does file IO on the main
  thread in a way that step boundaries would expose — note it for plan 016's known
  issues rather than fixing here.

## Maintenance notes

- New world-source types (e.g. a future "download from URL" source) get their own
  collaborator + a `createWorldFromX` orchestrator branch — the pattern is now
  visible.
- Reviewer focus: step 3's external-caller migration (the rename flow regenerating
  worlds).
