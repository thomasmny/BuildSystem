# Plan 013: Reshape the player domain into cohesive services

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/player buildsystem-core/src/main/java/de/eintosti/buildsystem/world/navigator buildsystem-core/src/main/java/de/eintosti/buildsystem/world/SpawnManager.java`
> Expected drift: plan 002 (NoClipManager sync task), 003/004 (receiver swaps),
> 008 (menus migrated), 009 (listener registration). Beyond that: compare excerpts,
> on mismatch STOP.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED (scoreboard + navigator behavior is highly visible to players)
- **Depends on**: 008, 009
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`PlayerServiceImpl` (391 lines) is the player-domain junk drawer: player storage
facade, build-mode tracking, world-creation limits, **scoreboard sidebar updates**
(`forceUpdateSidebar`, lines 166/184 — overlapping `SettingsManager`'s scoreboard
methods at 63–166), and the **armor-stand navigator** (give-item, open/close,
`getTargetEntity` raycasting at 283, `closeNewNavigator` at 361 — overlapping
`world/navigator/ArmorStandManager`). Two features are each split across two
classes; one class holds four features. This plan redistributes by feature, per
`plans/000-target-architecture.md` §4/§6.7, and renames `*Manager` → `*Service`
to match the architecture's naming rule.

## Current state

(Verified at `67beca7`.)

- `player/PlayerServiceImpl.java` (391) — implements `api.player.PlayerService`.
  Method map: `getPlayerStorage` 86, `getOpenNavigator` 90, `getBuildModePlayers`
  95, `isInBuildMode` 100, `canCreateWorld` 105, `getMaxWorlds` 121,
  `forceUpdateSidebar(BuildWorld)` 166, `forceUpdateSidebar(Player)` 184,
  `giveNavigator` 197, `getTargetEntity` 283 (private), `closeNewNavigator` 361,
  `save` 383.
- `player/settings/SettingsManager.java` (168) — `getSettings` 54,
  `displayScoreboard(Player)` 63, `displayScoreboard()` 85, `updateScoreboard` 93,
  `hideScoreboard` 152, `hideScoreboards` 166. Uses FastBoard.
- `world/navigator/ArmorStandManager.java` (128) — armor-stand spawning/removal
  for the "new navigator".
- `player/settings/NoClipManager.java` — post-plan-002: sync timer.
- `world/SpawnManager.java` (136) — spawn set/teleport/save; lives at `world/` root.
- `player/customblock/CustomBlockManager.java` — listener + placement logic
  (registration via `ListenerRegistrar` since plan 009).
- API constraint: `api.player.PlayerService` defines the methods core must keep
  exposing through `PlayerServiceImpl` — read the interface first; **moved logic
  must stay reachable via the API methods** (delegation is fine).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §4 (package tree), §6.7, §8 (naming).

## Scope

**In scope**:
- `navigator/NavigatorService.java` (create; absorbs `giveNavigator`,
  `getOpenNavigator`, `closeNewNavigator`, `getTargetEntity` from
  `PlayerServiceImpl` AND the whole of `world/navigator/ArmorStandManager.java`)
- `player/settings/SettingsService.java` (rename of `SettingsManager`; absorbs both
  `forceUpdateSidebar` methods)
- `player/noclip/NoClipService.java` (move+rename of `NoClipManager`)
- `world/spawn/SpawnService.java` (move+rename of `SpawnManager`)
- `player/PlayerServiceImpl.java` shrinks to: storage facade, build-mode set,
  limits, save — plus thin delegations for any API-mandated method whose logic moved
- `BuildSystemPlugin` wiring + all callers of renamed/moved classes
- `NavigatorListener` retargets to `NavigatorService`

**Out of scope** (do NOT touch):
- `api/player/**` — frozen; delegation keeps it satisfied
- Scoreboard line content/format, navigator armor-stand visuals — zero behavior change
- `BuildPlayerImpl`/`SettingsImpl`/`CachedValuesImpl` data classes (plan 015/016
  may touch docs only)
- `customblock/` beyond constructor-injection consistency

## Git workflow

- Conventional commits: one move/rename per commit (`git mv` so history follows),
  delegation wiring in the same commit as each move.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: `SettingsService`

`git mv` `SettingsManager.java` → `SettingsService.java` (same package), rename
class, update all references
(`grep -rln "SettingsManager" buildsystem-core/src/main/java`). Move both
`forceUpdateSidebar` overloads from `PlayerServiceImpl` into it (they manipulate
scoreboards — read them first; they likely call `updateScoreboard`). If
`api.player.PlayerService` declares `forceUpdateSidebar`, keep a one-line
delegation in `PlayerServiceImpl`.

**Verify**: compile → exit 0; `grep -rn "SettingsManager" buildsystem-core/src/main/java` → no matches.

### Step 2: `NavigatorService`

Create `de/eintosti/buildsystem/navigator/NavigatorService.java`. Move from
`PlayerServiceImpl`: `giveNavigator`, `getOpenNavigator`, `closeNewNavigator`,
`getTargetEntity`, and the fields they use (find them by following the method
bodies). Fold `ArmorStandManager`'s public surface in as well — either merge its
code directly or keep it as a package-private collaborator
`navigator/ArmorStands.java`; decide by size after reading (if the merged class
stays ≤ ~300 lines, merge). Update `NavigatorListener`, `PlayerQuitListener`,
`BuildSystemPlugin.onDisable` (`closeNewNavigator` call, line 192 at `67beca7`)
and other callers. API-declared methods keep delegations in `PlayerServiceImpl`.

**Verify**: compile → exit 0;
`ls buildsystem-core/src/main/java/de/eintosti/buildsystem/world/navigator/ArmorStandManager.java 2>/dev/null` → gone (merged or moved).

### Step 3: `NoClipService` and `SpawnService`

`git mv` + class rename + caller updates:
- `player/settings/NoClipManager.java` → `player/noclip/NoClipService.java`
- `world/SpawnManager.java` → `world/spawn/SpawnService.java`
(`grep -rln "NoClipManager\|SpawnManager" buildsystem-core/src/main/java` to find
callers; `BuildSystemPlugin` getters rename accordingly: `getNoClipService`,
`getSpawnService` — update `BuildSystemApi` if it exposes them.)

**Verify**: compile → exit 0; greps for old names → no matches.

### Step 4: Shrink check + constructor hygiene

`PlayerServiceImpl` should now be ≤ ~200 lines holding storage facade, build-mode,
limits, save, and API delegations. While in the moved classes, apply constructor
injection per 000 §5 (take `Messages`, `ConfigService`, etc. — not just `plugin`)
and remove any `// TODO(plan-014): inject` markers you can now satisfy.

**Verify**: `wc -l .../player/PlayerServiceImpl.java` → ≤ ~220.

### Step 5: Tests

The moved logic is Bukkit-bound; required tests are limited to:
- `player/PlayerServiceImplTest` for `canCreateWorld`/`getMaxWorlds` limit math
  (mock `Player` permissions for the per-permission limit lookup — read
  `getMaxWorlds` 121–164 first: it scans `buildsystem.create.public.<n>`-style
  permission nodes).

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 5 (≥ 4 cases: unlimited (-1), config default, permission-node override,
private vs public visibility). Operator smoke: scoreboard on/off via `/settings`,
navigator item give + open both navigator types, no-clip near blocks, `/spawn`.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `grep -rn "SettingsManager\|NoClipManager\|SpawnManager\|ArmorStandManager" buildsystem-core/src/main/java` → no matches
- [ ] `NavigatorService` exists; `PlayerServiceImpl` ≤ ~220 lines
- [ ] `git log --follow` works on the moved files (moves were `git mv`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `api.player.PlayerService` declares navigator/scoreboard methods whose delegation
  would require exposing the new services on the API — report the method list.
- `forceUpdateSidebar` and `updateScoreboard` turn out to be *different* rendering
  paths (not duplication) — merge nothing; just move, and note it.
- Hidden coupling: any moved method mutates `PlayerServiceImpl` private state
  that can't move with it.

## Maintenance notes

- Naming rule now holds: `*Service` everywhere (storage classes keep `*Storage`).
- Plan 014 finishes the injection pass; plan 016 verifies the final package tree
  (`navigator/` at root per 000 §4).
- Reviewer focus: API delegation completeness (`BuildSystemApi` + `PlayerService`
  interface methods all still wired).
