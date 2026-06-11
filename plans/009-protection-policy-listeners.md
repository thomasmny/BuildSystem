# Plan 009: Centralize build-protection rules and restructure the listener layer

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/listener`
> Expected drift from earlier plans: `Messages.`/`Config.` receiver swaps (003/004),
> `InventoryListener` deleted (008). The protection-check *logic* in the excerpts
> below must still match; otherwise STOP.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED (protection logic is security-relevant; a wrong consolidation lets
  players build where they shouldn't)
- **Depends on**: 003, 004 (008 recommended first to get listener count down)
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

The question "may this player modify this world?" is answered independently in at
least four listeners with subtly different orderings — verified at `67beca7`:

- `SettingsInteractListener.isValid` (lines 381–413): bypass → archive+
  `buildsystem.bypass.archive` → `blockPlacement` setting → builders+
  `buildsystem.bypass.builders` → creator fallback.
- `PlayerCommandPreprocessListener.disableArchivedWorlds`/`checkBuilders`
  (lines 275–302): same checks, reshuffled into two methods.
- `EditSessionListener.disableArchivedWorlds`/`disableNonBuilders` (lines 81–118):
  a third copy of the same pair.
- `WorldManipulateListener`: a fourth variant via `canModify`.

Additionally `SettingsInteractListener` (424 lines) is five unrelated features in
one class, and every listener self-registers in its constructor. When a protection
rule changes (say, a new bypass permission), four files must change in lockstep
today — that's how protection holes happen. Target
(`plans/000-target-architecture.md` §6.8): one `WorldProtectionPolicy`, thin
listeners, central registration.

## Current state

- `listener/` — 24 classes after plan 008 (`InventoryListener` deleted). All
  self-register via `plugin.getServer().getPluginManager().registerEvents(this, plugin)`
  in their constructors; `BuildSystemPlugin.registerListeners()` (lines 272–297 at
  `67beca7`) instantiates them.
- `SettingsInteractListener.java` (424 lines) — five independent `@EventHandler`
  features: `manageIronDoorSetting` (92), `manageSlabSetting` (128),
  `managePlacePlantsSetting` (175), `manageInstantPlaceSignsSetting` (230),
  `manageDisabledInteractSetting` (305), plus `isValid` (381) shared by them, plus
  `onInventoryOpen` (420) which exists only to suppress inventory-opens caused by
  `manageDisabledInteractSetting`.
- `PlayerCommandPreprocessListener.java` (302 lines) — command blocking in build
  worlds + a ~170-entry hardcoded WorldEdit/VoxelSniper command set
  (`DISABLED_COMMANDS`, starting ~line 45).
- `BlockPhysicsListener.java` — the guard
  `if (buildWorld == null || buildWorld.getData().physics().get())` repeated in 8
  handlers (lines 67, 111, 120, 129, 138, 153, 162, 171).
- `world/util/WorldPermissionsImpl.java` — existing per-world permission helper
  (`canBypassBuildRestriction`, `canPerformCommand`); the policy builds ON it, does
  not replace it (it implements an API interface).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.8, §5.

## Scope

**In scope**:
- Create `protection/WorldProtectionPolicy.java` (+ unit tests)
- Split `SettingsInteractListener` into per-feature listeners under `listener/`
- Rewrite the four duplicated check sites to call the policy
- Create `listener/ListenerRegistrar.java`; remove constructor self-registration
  from ALL listeners; delete `BuildSystemPlugin.registerListeners()` in favor of the
  registrar (keep the conditional integration listeners from `registerExpansions()`
  — lines 322–344 — registered conditionally in the registrar)
- Extract `DISABLED_COMMANDS` to `integration/worldedit/WorldEditCommands.java`
- `BlockPhysicsListener`: collapse the 8-fold guard into one private
  `isPhysicsEnabled(Block)` helper used by all handlers

**Out of scope** (do NOT touch):
- `WorldPermissionsImpl` semantics (API-bound)
- Event priorities/`ignoreCancelled` flags — copy each `@EventHandler` annotation
  exactly
- The teleport/join/quit/chat listeners' feature logic (only their registration
  and any protection-check call sites change)
- Making `DISABLED_COMMANDS` user-configurable (deliberate non-goal: it is
  integration knowledge, not configuration; revisit only if users ask)

## Git workflow

- Conventional commits: policy + tests; listener split; registrar; extraction.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: `WorldProtectionPolicy` — transcribe, don't redesign

```java
@NullMarked
public final class WorldProtectionPolicy {

    public enum Denial { NONE, ARCHIVED, NOT_A_BUILDER, SETTING_DISABLED }

    // (worldStorage or no deps at all — the checks below only need BuildWorld + Player)

    public boolean canBypass(Player player, BuildWorld world) {
        return world.getPermissions().canBypassBuildRestriction(player);
    }

    public Denial checkArchive(Player player, BuildWorld world) { ... }    // archive status + "buildsystem.bypass.archive"
    public Denial checkBuilders(Player player, BuildWorld world) { ... }   // buildersEnabled + isBuilder + "buildsystem.bypass.builders" + creator rule
    public Denial checkSetting(Player player, BuildWorld world, Type<Boolean> setting) { ... }

    public Denial mayModify(Player player, BuildWorld world) { ... }       // bypass → archive → builders (the common chain)
    public Denial mayModify(Player player, BuildWorld world, Type<Boolean> setting) { ... }
}
```

The method bodies are **transcriptions** of `SettingsInteractListener.isValid`
(lines 381–413 — the most complete variant, excerpted in "Why this matters"). The
creator fallback (`return builders.isCreator(player)` when builders are enabled and
the player isn't one) must be preserved exactly. Before writing, diff the four
existing variants against each other; where they genuinely differ (e.g.
`PlayerCommandPreprocessListener` checks bypass per-rule rather than up front),
keep per-call-site composition possible via the fine-grained `checkX` methods so
each listener reproduces ITS current behavior — do **not** force all four onto one
chain if their semantics differ. Document any found difference in the PR.

**Verify**: unit tests (step 5) green before converting any listener.

### Step 2: Convert the four duplicated sites

One listener at a time, replace the private check methods with policy calls and
delete the private copies. After each: compile, and re-read the diff to confirm
check order and permission strings are unchanged.

### Step 3: Split `SettingsInteractListener`

Five new listeners in `listener/`: `IronDoorListener`, `SlabListener`,
`PlantPlacementListener`, `InstantSignPlacementListener`,
`DisabledInteractionsListener` (this one takes the `onInventoryOpen` suppressor
with it — they share state; check what state the two handlers share, likely a
set of players whose interaction triggered the open, and keep that state inside the
one class). Each gets its feature's `@EventHandler`(s) + helpers, calls the policy
instead of `isValid`. Delete `SettingsInteractListener`.

### Step 4: `ListenerRegistrar` + de-self-registration

- New `listener/ListenerRegistrar.java` with `registerAll()`: instantiates and
  registers every listener (including the five new ones), replicating today's
  conditional registrations: `WorldManipulateByAxiomListener` only if AxiomPaper
  present, `EditSessionListener` only if WorldEdit/FAWE present AND
  `settings.builder.block-worldedit-non-builder` (see `registerExpansions()`,
  `BuildSystemPlugin.java:322-344`).
- Remove `registerEvents(this, plugin)` from every listener constructor.
- `BuildSystemPlugin.onEnable` calls the registrar once;
  delete `registerListeners()`.

**Verify**: `grep -rn "registerEvents" buildsystem-core/src/main/java --include='*.java' | grep -v "ListenerRegistrar\|MenuListener registration site"` → matches only in `ListenerRegistrar` (and `CustomBlockManager` if plan 013 hasn't moved it yet — record what you find).

### Step 5: Extract the WorldEdit command set + physics guard helper

- `integration/worldedit/WorldEditCommands.java`:
  `public static final Set<String> RESTRICTED = Set.of( ...the ~170 entries verbatim... );`
  `PlayerCommandPreprocessListener` imports it; the inline set is deleted.
- `BlockPhysicsListener`: add
  `private boolean physicsAllowed(World world)` encapsulating
  `buildWorld == null || buildWorld.getData().physics().get()` and use it in all 8
  handlers.

### Step 6: Tests

`protection/WorldProtectionPolicyTest.java` (Mockito mocks for `Player`,
`BuildWorld`, `WorldData`, `Builders`, `WorldPermissions`):
- bypass permission short-circuits everything
- archive world + no bypass → `ARCHIVED`; with `buildsystem.bypass.archive` → pass
- builders enabled + non-builder + no bypass → `NOT_A_BUILDER`; creator → pass
- setting disabled → `SETTING_DISABLED`
- ordering: archive denial wins over builder denial (matches `isValid`)

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 6 (≥ 8 cases). Operator smoke list: non-builder tries to place a block in a
builders-enabled world; archived world blocks commands and WorldEdit for
non-bypass players; `/physics off` world ignores block updates.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `SettingsInteractListener.java` deleted; five feature listeners exist
- [ ] `grep -rn "canBypassBuildRestriction" buildsystem-core/src/main/java/de/eintosti/buildsystem/listener` → no matches (all go through the policy)
- [ ] All listener registration flows through `ListenerRegistrar` (step-4 grep)
- [ ] `DISABLED_COMMANDS` no longer in `PlayerCommandPreprocessListener`
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The four protection variants differ in a way the fine-grained policy methods
  cannot express without changing one of them — report the exact difference.
- The two coupled handlers in `SettingsInteractListener`
  (`manageDisabledInteractSetting` + `onInventoryOpen`) share state with any OTHER
  handler in the class.
- A listener's constructor does work beyond field assignment + self-registration
  (hidden initialization that the registrar timing would change).

## Maintenance notes

- New protection rules go into `WorldProtectionPolicy` and nowhere else; reviewers
  should reject `hasPermission("buildsystem.bypass.…")` outside it.
- The policy's fine-grained methods exist because the legacy call sites differ —
  once plans 010–013 settle, consider collapsing to the two `mayModify` overloads.
- Plan 014 finishes constructor injection for the listeners the registrar builds.
