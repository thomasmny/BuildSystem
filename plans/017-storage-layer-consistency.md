# Plan 017: Bring the player/folder/setup/spawn storages up to the world storage's standard

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/storage`
> Expected drift: plan 010 (WorldStorageImpl thread-safety, delete durability),
> plans 003/004 (receiver swaps). The player/folder/setup/spawn storages should be
> otherwise untouched; on structural mismatch with the excerpts: STOP.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED (persistence of player settings and folders; data-format must not
  change)
- **Depends on**: 010 (it sets the pattern this plan replicates)
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

Plan 010 makes the **world** registry thread-safe and its flows explicit — but the
same registry pattern exists three more times with the same defects:
`PlayerStorageImpl` and `FolderStorageImpl` hold plain `HashMap` registries read
from async save pipelines, `FolderStorageImpl` takes the whole `WorldServiceImpl`
as a constructor dependency (service→storage→service cycle), and the three
`storage/factory/*` classes are one-line indirections that hide the wiring the
composition root should own. Leaving these as-is would make the storage layer
internally inconsistent — half modern, half legacy.

## Current state

(Verified at `67beca7`.)

- `storage/PlayerStorageImpl.java` (91 lines) — `abstract class … implements
  PlayerStorage`; `private final Map<UUID, BuildPlayer> buildPlayers = new
  HashMap<>()` (lines 44/50); `loadPlayers()` (53) populates from the subclass's
  `load()`; `createBuildPlayer(UUID)` (65) is get-or-create. Subclass:
  `storage/yaml/YamlPlayerStorage.java` (245).
- `storage/FolderStorageImpl.java` (122 lines) — same pattern;
  `Map<String, Folder> foldersByName = new HashMap<>()` (48/55), lowercase-keyed
  (61); **constructor takes `(BuildSystemPlugin, WorldServiceImpl)`** (50).
  Subclass: `storage/yaml/YamlFolderStorage.java` (189).
- `storage/yaml/AbstractYamlStorage.java` (70) — shared async YAML save/load
  plumbing. Also: `YamlSetupStorage` (80), `YamlSpawnStorage` (45) — small,
  separate shapes; read them before touching.
- `storage/factory/` — `WorldStorageFactory` (48), `PlayerStorageFactory` (46),
  `FolderStorageFactory` (48). Verified shape (`WorldStorageFactory.createStorage`):
  ```java
  public WorldStorageImpl createStorage(WorldServiceImpl worldService) {
      return new YamlWorldStorage(plugin, worldService);
  }
  ```
  Each factory has exactly one production path (YAML); they exist to ease a future
  database backend but today are pure indirection.
- API constraint: `api/storage/PlayerStorage`, `api/storage/FolderStorage`
  (and `WorldStorage`) are public interfaces — implementations must keep
  satisfying them unchanged.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.5, §7.
- Plan 010's completed diff (read it via `git log`) — this plan replicates its
  decisions; do not invent different ones.

## Scope

**In scope**:
- `storage/PlayerStorageImpl.java`, `storage/FolderStorageImpl.java` —
  `ConcurrentHashMap` registries, `synchronized` compound mutations (mirroring
  plan 010's `WorldStorageImpl` treatment)
- `FolderStorageImpl` constructor: replace the `WorldServiceImpl` dependency with
  the narrowest thing its methods actually use (read the usages — likely
  `WorldStorage` for world↔folder resolution); break the cycle
- Delete `storage/factory/` — the composition root constructs
  `YamlWorldStorage`/`YamlPlayerStorage`/`YamlFolderStorage` directly (the
  "future backend" seam is the `*StorageImpl` abstract class itself, which stays)
- `storage/yaml/AbstractYamlStorage` — verify all five YAML storages run saves on
  one consistent executor and return joinable futures (the shutdown join in
  `BuildSystemPlugin.onDisable` must cover player/folder/spawn saves as it does
  today via `saveBuildConfig`, line 391 at `67beca7`); fix inconsistencies, don't
  redesign
- Unit tests for both registries

**Out of scope** (do NOT touch):
- YAML file formats (players.yml, folders.yml, spawn.yml, setup.yml byte
  compatibility)
- `WorldStorageImpl` (done in 010)
- The API interfaces
- Introducing a database backend or any new storage type

## Git workflow

- Conventional commits: one per storage class, one for factory deletion, one for
  tests.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Registry thread-safety

`PlayerStorageImpl`: `HashMap` → `ConcurrentHashMap`; `createBuildPlayer(UUID)`'s
get-or-create (line 65: `getOrDefault` + put — read the exact body) becomes
`computeIfAbsent` so concurrent joins can't double-create. `loadPlayers()` (53)
must swap contents atomically: build the new map fully, then
`clear()`+`putAll` inside a `synchronized` block — or better, document that
`loadPlayers` runs once during `init()` before any concurrent access and assert it
(`isEmpty()` check). Mirror the same treatment in `FolderStorageImpl`
(lowercase-keyed name map; mirror plan 010's name-index handling on rename if
folders can be renamed — check `FolderImpl` for a rename path first).

**Verify**: compile;
`grep -n "new HashMap" buildsystem-core/src/main/java/de/eintosti/buildsystem/storage/PlayerStorageImpl.java buildsystem-core/src/main/java/de/eintosti/buildsystem/storage/FolderStorageImpl.java` → no matches.

### Step 2: Break the folder→service cycle

List every use of the `worldService` field in `FolderStorageImpl` and
`YamlFolderStorage` (`grep -n "worldService" …`). Replace the constructor
parameter with the narrowest dependency that covers them (expected:
`WorldStorage`, possibly plus a `Supplier` for anything lazily needed). Update the
construction site(s).

### Step 3: Delete the factories

Inline the three factories' single calls at their call sites (find:
`grep -rn "StorageFactory" buildsystem-core/src/main/java`), then delete
`storage/factory/`. The composition root (or `WorldServiceImpl.init` /
`PlayerServiceImpl.init`, wherever construction currently happens) constructs the
YAML implementations directly.

**Verify**: compile; `ls buildsystem-core/src/main/java/de/eintosti/buildsystem/storage/factory 2>/dev/null` → gone.

### Step 4: Save-pipeline consistency check

Read `AbstractYamlStorage` (70 lines) and each subclass's save path. Confirm:
(a) saves run off the main thread, (b) every storage's save future is included in
the shutdown join (trace `saveBuildConfig`'s `allOf` — worlds, players, spawn at
`67beca7`; folders and setup must be covered too — find where they save and add to
the join if missing, reporting what you found), (c) no storage writes the same
file from two threads concurrently (if `AbstractYamlStorage` lacks per-file
serialization, add a per-instance synchronized write or single-thread executor —
match what plan 010 did).

### Step 5: Tests

- `storage/PlayerStorageImplTest`: get-or-create idempotency (two calls, one
  instance), concurrent `createBuildPlayer` for the same UUID from 4 threads →
  exactly one instance.
- `storage/FolderStorageImplTest`: case-insensitive lookup, `folderExists`,
  create→get round-trip (mock the persistence subclass methods — the abstract
  classes need a test subclass with stubbed `load()`/`save()`).

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 5 (≥ 5 cases). Operator smoke: join with an existing player profile, change
a setting, restart, setting persisted; create folder, restart, folder persists with
its worlds.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] No `HashMap` registries in `storage/` (step-1 grep, plus the same for any
      registry found in setup/spawn storages — report if they have none)
- [ ] `FolderStorageImpl` no longer depends on `WorldServiceImpl`
- [ ] `storage/factory/` deleted
- [ ] Shutdown join provably covers all storages (cite the chain in the report)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `FolderStorageImpl`'s `worldService` usage includes flow orchestration (not just
  lookups) — that's misplaced logic needing a judgment call.
- The YAML formats would change in any byte-visible way.
- Folder rename exists and its name-index handling can't mirror plan 010's
  approach.

## Maintenance notes

- The storage layer is now uniform: abstract `*StorageImpl` = registry +
  contract, `Yaml*` subclass = persistence. A future database backend implements
  the abstract class — that seam replaced the deleted factories.
- Reviewer focus: step 4 (shutdown data-loss windows).
