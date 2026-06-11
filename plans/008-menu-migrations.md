# Plan 008: Migrate all menus onto the framework and delete the old GUI plumbing

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/menu`
> Plan 007 must have landed (the `menu/` package exists, `SpeedInventory`
> migrated). If not: STOP.

## Status

- **Priority**: P2
- **Effort**: L (the largest single plan — 15 menus; budget accordingly, it is
  acceptable to split execution across sessions per menu group)
- **Risk**: MED (every GUI in the plugin is touched; click behavior must be
  preserved exactly)
- **Depends on**: 007
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

After plan 007 the framework exists but 15 menus still run on the legacy
map-registration path, including the two worst god-methods in the codebase:
`EditInventory.onClick` (449-line class, one giant click switch with per-case
permission checks) and `DisplayablesInventory` (594 lines mixing navigator
rendering, pagination, folder/world creation chat-flows, and click dispatch).
Finishing the migration deletes `InventoryManager`, `InventoryHandler`,
`PaginatedInventory`, `BuildSystemHolder`, `BuildWorldHolder`, `InventoryListener`,
and most of `InventoryUtils` — and with them the per-player `invIndex` leak.

## Current state

The 15 remaining menu classes (verified list at `67beca7`):

| Class | Lines | Notes |
|---|---|---|
| `world/navigator/inventory/NavigatorInventory.java` | — | main navigator |
| `world/navigator/inventory/DisplayablesInventory.java` | 594 | paginated; world/folder lists; creates folders via `PlayerChatInput` callbacks in click cases 48–50 |
| `world/navigator/inventory/` (check for siblings) | — | migrate everything in the package |
| `world/modification/EditInventory.java` | 449 | god click-switch; `addSettingsItem` dup at lines 160–174 |
| `world/modification/GameRulesInventory.java` | 298 | paginated |
| `world/modification/DeleteInventory.java` | — | confirm dialog |
| `world/modification/SetupInventory.java` | — | default-icon setup |
| `world/creation/CreateInventory.java` | 310 | paginated; re-lists template dir on every open (lines 87–109) |
| `world/data/StatusInventory.java` | — | status picker |
| `world/backup/BackupsInventory.java` | — | backup list |
| `world/builder/BuilderInventory.java` | — | paginated builder list |
| `player/settings/SettingsInventory.java` | 242 | `addSettingsItem` dup at lines 95–109 |
| `player/settings/DesignInventory.java` | — | glass color picker |
| `player/customblock/CustomBlockInventory.java` | — | secret blocks |
| (already migrated in 007) `player/settings/SpeedInventory.java` | — | exemplar |

Legacy plumbing to delete at the end: `util/inventory/InventoryManager.java`,
`InventoryHandler.java`, `PaginatedInventory.java`, `BuildSystemHolder.java`,
`BuildWorldHolder.java`, `listener/InventoryListener.java`, plus the
`InventoryManager` field/getter in `BuildSystemPlugin` (line 433 at `67beca7`).
`InventoryUtils` shrinks to whatever `isNavigator`/`MaterialUtils` still need —
move survivors into `menu/` or `util/` as fits, delete the rest.

Duplicated toggle-item builder to consolidate (verified identical except a
parameter name):

```java
// EditInventory.java:160-174  AND  SettingsInventory.java:95-109
private void addSettingsItem(Player player, Inventory inventory, int position, XMaterial material, boolean isEnabled, String displayNameKey, String loreKey) {
    ItemStack itemStack = material.parseItem();
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(Messages.getString(displayNameKey, player));
    itemMeta.setLore(Messages.getStringList(loreKey, player));
    itemMeta.addItemFlags(ItemFlag.values());
    itemStack.setItemMeta(itemMeta);
    if (isEnabled) {
        itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
    }
    inventory.setItem(position, itemStack);
}
```

→ becomes one `ItemBuilder` chain (`.hideAttributes().glow(isEnabled)`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |
| Legacy gone | `ls buildsystem-core/src/main/java/de/eintosti/buildsystem/util/inventory 2>/dev/null` | only files explicitly kept (or directory gone) |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.4, §7.
- Plan 007's migrated `SpeedInventory` is the structural exemplar — read it first
  and match it.

## Scope

**In scope**: the 15 menu classes above; deletion of the legacy plumbing; every
call site that opens these menus (constructor/open-call shape); extraction of
focused private methods out of the two god click-handlers (see step 3); moving
`CreateInventory`'s template listing behind a small cache.

**Out of scope** (do NOT touch):
- Click *behavior*: every slot must do exactly what it does today (same permission
  checks, same sounds, same messages, same follow-up menu).
- `NavigatorListener` (armor-stand navigator interaction) except where it opens one
  of these menus.
- `PlayerChatInput` internals — the chat-input flows in `DisplayablesInventory`
  keep using it; only their *location* changes (step 3b).
- The world/folder domain logic invoked by clicks (services stay as they are).

## Git workflow

- One commit per menu (or per menu group); final commit deletes legacy plumbing.
- Conventional commits: `refactor: migrate XInventory to menu framework`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Simple fixed-slot menus (8 menus)

`DesignInventory, CustomBlockInventory, StatusInventory, DeleteInventory,
SetupInventory, SettingsInventory, NavigatorInventory, BackupsInventory` — for each:

1. Extend `Menu` (constructor takes its real dependencies + whatever identifies the
   subject: `BuildWorld`, etc. — replacing `BuildWorldHolder` usage with plain
   constructor state on the menu).
2. `populate(Player)` = the old item-adding code, rewritten on `ItemBuilder`.
3. `handleClick` = the old `onClick`, minus any `event.getInventory()` re-lookup
   gymnastics; `event.setCancelled(...)` calls stay exactly as they are.
4. Remove `registerInventoryHandler` calls; open via `menu.open(player)`.
5. Update open call sites (`grep -rn "new <Name>Inventory" buildsystem-core/src/main/java`).

**Verify after each menu**: compile → exit 0; grep the migrated file for
`InventoryManager|InventoryHandler|BuildSystemHolder` → no matches.

### Step 2: Paginated menus (3 menus)

`GameRulesInventory, CreateInventory, BuilderInventory` — same as step 1 but on
`PaginatedMenu`. The old code's `Inventory[] inventories` pre-built-pages model
disappears: `populate` renders the **current page** into the single inventory and
page navigation calls `previousPage/nextPage` then re-`populate`s. Preserve each
menu's items-per-page constant and navigation slots.

`CreateInventory` extra: extract the template-directory listing (old lines 87–109)
into a private method called once per `open` (NOT once per page flip), keeping
behavior (fresh listing per open) while dropping the per-page re-listing.

### Step 3: The two god-menus

**3a. `EditInventory` (449 lines).** Migrate to `Menu`, then split:
- `populate` decomposes into the existing private add-item helpers (already
  per-feature: `addTimeItem`, `addSettingsItem` → `ItemBuilder`, etc.).
- `handleClick`'s big switch: keep ONE switch over slots, but each case body becomes
  a call to a private method named for the feature
  (`toggleBlockBreaking(player, world)`, `openGameRules(player)`, …). No case body
  longer than ~10 lines. The repeated per-case permission guard becomes one private
  `hasEditPermission(player, world, setting)` helper preserving the exact checks.

**3b. `DisplayablesInventory` (594 lines).** Migrate to `PaginatedMenu`, then:
- The folder/world creation chat-input flows embedded in click cases 48–50 move to
  private methods (`promptFolderCreation(player)`, …) — same `PlayerChatInput`
  usage, just named and out of the switch.
- The displayable-list computation (`cachedDisplayables`, `generatedInventories`)
  collapses into `populate` + a `List<Displayable>` computed per open (snapshot
  semantics today: list is computed at open and on explicit refresh — preserve
  exactly that; do not add live refresh).
- Read the whole file before editing; it is the riskiest migration. If its click
  routing depends on PDC item tags (`DISPLAYABLE_TYPE_KEY`), keep those reads
  identical.

**Verify**: compile after each; `wc -l` on both files → each ≤ ~350 lines;
no method > 60 lines (`awk` or manual check on the two click handlers).

### Step 4: Delete the legacy plumbing

Delete: `util/inventory/InventoryManager.java`, `InventoryHandler.java`,
`PaginatedInventory.java`, `BuildSystemHolder.java`, `BuildWorldHolder.java`,
`listener/InventoryListener.java`; remove the `inventoryManager` field + getter
from `BuildSystemPlugin` and its `InventoryListener` registration line. Shrink
`InventoryUtils`: anything still referenced moves to `menu/` (`isNavigator` is used
by listeners — check `grep -rn "InventoryUtils\." buildsystem-core/src/main/java`)
and the class is deleted or reduced to what remains.

**Verify**:
- compile → exit 0
- `grep -rn "InventoryManager\|InventoryHandler\|PaginatedInventory\|BuildSystemHolder\|BuildWorldHolder" buildsystem-core/src/main/java --include='*.java'` → no matches
- `./gradlew :buildsystem-core:test` → exit 0 (the plan 001 `PaginatedInventoryTest`
  must be retargeted to `PaginatedMenu` or deleted in favor of plan 007's test)

## Test plan

- Plan 007's `PaginatedMenuTest` covers paging; no new unit tests are required for
  the migrations themselves (they are Bukkit-bound).
- Operator smoke list (write it into the PR description): `/worlds` navigator → all
  category pages → page navigation; `/worlds edit <world>` → toggle three settings →
  verify persisted; create world via navigator incl. folder creation chat flow;
  `/settings` → toggle each; secret blocks menu; backup menu list/restore prompt;
  delete-world confirm dialog.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] All 16 menus extend `Menu`/`PaginatedMenu`; legacy plumbing deleted (step-4 grep)
- [ ] `EditInventory` and `DisplayablesInventory` ≤ ~350 lines each, no method > 60 lines
- [ ] `grep -rn "invIndex" buildsystem-core/src/main/java` → no matches
- [ ] `plans/README.md` status row updated (note any menu deferred, with reason)

## STOP conditions

Stop and report back (do not improvise) if:

- A menu mutates another player's open inventory or is shared across viewers
  (the one-instance-per-open model would change behavior) — report which menu.
- `DisplayablesInventory`'s pagination interacts with its caching in a way the
  per-open snapshot model cannot reproduce (e.g. page state deliberately survives
  reopen) — report before changing UX.
- Any click case's behavior cannot be preserved without the old
  `InventoryManager` registration semantics.
- You are more than ~2/3 through the effort budget with step 3 not started —
  report progress and hand off rather than rushing the god-menus.

## Maintenance notes

- After this plan, "add a menu" = extend `Menu`, build items with `ItemBuilder`,
  open with `.open(player)`. Reviewers should reject new `Bukkit.createInventory`
  calls outside `Menu`.
- The per-open snapshot semantics of the navigator (stale until reopened) is
  pre-existing behavior, now explicit — a future "live refresh" feature would hook
  `populate` re-runs, not caches.
- Plan 013 may move menu classes into `player/menu/` & `world/menu/` packages —
  pure `git mv`, deliberately deferred to the structure pass (plan 016 verifies).
