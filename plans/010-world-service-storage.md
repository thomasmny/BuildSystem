# Plan 010: Make world flows thread-correct and untangle service/storage/domain coupling

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/world buildsystem-core/src/main/java/de/eintosti/buildsystem/storage`
> Expected drift: plan 002 fixed `renameWorld`'s thread shape; plans 003/004 swapped
> config/message receivers; plan 008 migrated menus. The service/storage structure
> must otherwise match the excerpts; on mismatch STOP.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: HIGH (world create/rename/delete are the plugin's most destructive
  flows; mistakes lose user data — work in small verified steps)
- **Depends on**: 002, 003, 004
- **Category**: tech-debt / bug
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

The world domain works but its data flow is tangled and its concurrency is hopeful:
registry maps are plain `HashMap`/`HashBiMap` read from async contexts; the domain
object `WorldDataImpl` reaches back through the plugin singleton into the service
that owns it; delete persists asynchronously so a crash can resurrect a deleted
world's metadata; per-world unload timers have no central lifecycle. This plan makes
ownership explicit — **storage owns maps (thread-safe), service owns flows
(main-thread orchestrated), domain objects own no service references** — per
`plans/000-target-architecture.md` §6.5.

## Current state

(Verified at `67beca7` unless marked.)

- `world/WorldServiceImpl.java` (431 lines) — flows: `startWorldNameInput` (100),
  `importWorlds` (174, uses a `BukkitRunnable` batch import), `unimportWorld` (227),
  `deleteWorld(Player, BuildWorld)` (234) delegating to `deleteWorld(BuildWorld)`
  (254, composes `unimportWorld` + file deletion futures), `renameWorld` (287,
  thread shape fixed by plan 002), `removePlayersFromWorld` (370),
  `save()` (413). `init()` (79) loads worlds + folders and schedules tasks.
- `storage/WorldStorageImpl.java` — `private final Map<UUID, BuildWorld>
  buildWorldsByUuid = new HashMap<>()` and `HashBiMap<UUID, String>
  worldIdentifiers` (lines 64–65); `addBuildWorld` does two un-synchronized puts
  (102–105); `removeBuildWorld` two removes + folder detach (107–117); lookups
  lowercase the name (76).
- `storage/yaml/YamlWorldStorage.java` (290) + `storage/yaml/AbstractYamlStorage.java`
  — async YAML persistence returning `CompletableFuture`.
- `world/data/WorldDataImpl.java:133-141` (verified):
  ```java
  @Nullable
  private Folder getAssignedFolder() {
      WorldService worldService = BuildSystemPlugin.get().getWorldService();
      BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(this.worldName);
      if (buildWorld != null) {
          return buildWorld.getFolder();
      }
      return null;
  }
  ```
  A data object resolving its own world through the global singleton to ask for its
  folder — used by the folder-override accessors (permission/project).
- `world/BuildWorldImpl.java` — constructs its own `WorldLoaderImpl`/`WorldUnloaderImpl`
  (~line 123); unload timers live per-world with no central registry.
  `BuildSystemPlugin.reloadConfigData` (line 415) iterates all worlds calling
  `getUnloader().manageUnload()`.
- `world/display/FolderImpl.java` — mutable `List` fields for worlds/subfolders
  (~lines 47–48); check what `getWorlds()`/`getSubFolders()` return.
- `Bukkit.getWorlds().getFirst()` at `WorldServiceImpl.java:376` (inside
  `removePlayersFromWorld`) throws if the server has zero worlds — theoretical, but
  switch to a guarded lookup while in the file.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.5, §7 (threading model — binding here).

## Scope

**In scope**:
- `storage/WorldStorageImpl.java` — thread-safe maps, atomic rename support
- `world/WorldServiceImpl.java` — flow thread-shapes, delete durability
- `world/data/WorldDataImpl.java` — remove the singleton back-reference
- `world/BuildWorldImpl.java`, `world/util/WorldUnloaderImpl.java` — central unload
  task tracking
- `world/display/FolderImpl.java` — defensive collection exposure
- Unit tests for storage registry semantics

**Out of scope** (do NOT touch):
- `BuildWorldCreatorImpl` (plan 011)
- `world/backup/**` (plan 012)
- API interfaces (`api.world.*`, `api.storage.*`) — implementations must keep
  satisfying them unchanged
- YAML file formats — bytes on disk stay compatible (worlds.yml etc.)

## Git workflow

- Conventional commits, one concern per commit (storage maps; rename atomicity;
  delete durability; WorldDataImpl decoupling; unloader registry; folder
  collections).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Thread-safe storage registry

In `WorldStorageImpl`:
- Replace `HashMap` with `ConcurrentHashMap` for `buildWorldsByUuid`.
- Replace `HashBiMap` with `Maps.synchronizedBiMap(HashBiMap.create())` (Guava is
  already on the classpath via spigot) OR — preferred — drop the bimap for a second
  `ConcurrentHashMap<String, UUID> uuidByName` maintained alongside, since the only
  inverse use is name→UUID lookup (`WorldStorageImpl.java:76`). Check all
  `worldIdentifiers` usages first: `grep -n "worldIdentifiers" .../WorldStorageImpl.java`.
- Add `synchronized void rename(BuildWorld world, String oldName, String newName)`
  that updates the name index atomically (remove old key, put new), and make
  `addBuildWorld`/`removeBuildWorld` `synchronized` (cheap; these are rare
  operations — correctness over micro-performance).
- `WorldServiceImpl.renameWorld` calls `rename(...)` instead of `addBuildWorld`
  re-put (post-plan-002 the call sits in the main-thread block; keep it there).

**Verify**: compile; new unit test (step 6) for rename leaves exactly one name
mapping.

### Step 2: Durable delete

Trace `deleteWorld(BuildWorld)` (254) → `unimportWorld(buildWorld, false)` (227):
identify where the world is removed from storage and where `worlds.yml` is
persisted. Make the metadata removal durable **before** the world directory is
deleted from disk: the future chain must be
`unimport (registry remove, main) → persist worlds.yml (async, AWAITED) → delete world folder (async)`.
If persistence today happens only via the periodic `save()`, add an explicit
awaited save of the worlds file into the chain. The goal (state it in the commit):
a crash mid-delete may leave an orphaned world FOLDER (harmless, re-importable) but
never an orphaned REGISTRY ENTRY pointing at a deleted folder (breaks startup
loading today).

### Step 3: Decouple `WorldDataImpl`

Replace `getAssignedFolder()`'s singleton chain with a
`Supplier<@Nullable Folder> folderResolver` field injected at construction.
Construction sites of `WorldDataImpl` (find:
`grep -rn "new WorldDataImpl" buildsystem-core/src/main/java`) pass
`() -> buildWorld.getFolder()` or equivalent from a context that owns the world
reference. If a construction site cannot supply one (e.g. deserialization order),
allow a setter called immediately after the `BuildWorld` is wired —
fail loud (`IllegalStateException`) if the override accessors run unresolved.
Remove the `BuildSystemPlugin` import from `WorldDataImpl`.

### Step 4: Central unload-task tracking

In `WorldUnloaderImpl`: find the scheduled task field. Add a small
`world/lifecycle/UnloadScheduler` (or equivalent registry owned by
`WorldServiceImpl`) through which unload tasks are created/cancelled so that
(a) `reloadConfigData`'s re-evaluation iterates one registry rather than all worlds,
and (b) plugin disable cancels everything in one call from `onDisable`. Keep the
per-world `manageUnload()` decision logic where it is — only task *ownership*
centralizes.

### Step 5: Folder collection hygiene + misc

- `FolderImpl`: getters return `List.copyOf(...)` (check the API contract Javadoc
  first — if the API promises a live view, keep it and note that in the report
  instead of changing).
- `WorldServiceImpl.java:376`: replace `Bukkit.getWorlds().getFirst()` with a
  guarded lookup that logs + returns early if empty.

### Step 6: Tests

`storage/WorldStorageImplTest.java` (the storage is constructible if its
constructor deps are mockable — check; it takes plugin/service references):
- add → lookup by uuid, by name (case-insensitive), by wrong name → null
- rename → old name gone, new name resolves, uuid unchanged
- remove → both indexes empty, folder detach called (mock `Folder`)
- concurrent smoke: 4 threads × 250 add/remove cycles on distinct worlds → no
  exception, final size correct (plain `ExecutorService` test)

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 6 (≥ 5 cases incl. the concurrency smoke). Operator smoke list: create →
rename → teleport → delete a world; delete a world and hard-kill the server right
after confirming — on restart no broken registry entry.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `grep -n "HashMap\|HashBiMap" buildsystem-core/src/main/java/de/eintosti/buildsystem/storage/WorldStorageImpl.java` → no matches
- [ ] `grep -n "BuildSystemPlugin" buildsystem-core/src/main/java/de/eintosti/buildsystem/world/data/WorldDataImpl.java` → no matches
- [ ] Delete flow: registry persistence is awaited before folder deletion (assert
      by reading the final chain; cite it in the completion report)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The API (`api.storage.WorldStorage` / `api.world.display.Folder`) contractually
  exposes live mutable collections — report before changing return semantics.
- `unimportWorld`'s `save` parameter (line 227) does something other than control
  persistence — understand it fully before step 2; report if ambiguous.
- `WorldDataImpl` construction order makes the folder resolver genuinely circular
  (world needs data, data needs world) and the setter escape hatch doesn't fit.
- Any step would change the on-disk YAML format.

## Maintenance notes

- The threading rules (000 §7) are now load-bearing in this package; reviewers
  should check every new `CompletableFuture` chain ends in a main-thread hop before
  touching Bukkit state.
- Plan 011 splits the creator; plan 014 finishes constructor injection here.
- The delete-durability ordering is deliberate (orphan folder over orphan entry) —
  documented in step 2; don't "optimize" it back.
