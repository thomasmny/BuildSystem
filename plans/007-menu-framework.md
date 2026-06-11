# Plan 007: Build the menu framework (holder-routed Menu, ItemBuilder, pagination)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/util/inventory buildsystem-core/src/main/java/de/eintosti/buildsystem/listener/InventoryListener.java`
> Expected drift from plan 002: `InventoryManager.handleClose` lost its double
> remove; `InventoryUtils.addWorldItem` gained a main-thread hop; the map field is
> private. Anything else: compare excerpts before proceeding.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW (new code + one exemplar migration; old path keeps running)
- **Depends on**: 002, 004
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

The GUI layer routes clicks through a `Map<Inventory, InventoryHandler>`
(`util/inventory/InventoryManager`) that every menu must register into, and
pagination state lives in per-menu `Map<UUID, Integer>` fields
(`PaginatedInventory.invIndex`) that are never cleaned up — a slow leak and an
awkward lifecycle. Meanwhile `BuildSystemHolder` already implements
`InventoryHolder`, so the codebase is halfway to the modern idiom: **the holder IS
the menu**. This plan finishes that move: one listener, no registration map, no
UUID maps, plus a shared `ItemBuilder` to replace the ad-hoc item assembly
duplicated across 16 menu classes (e.g. `addSettingsItem` exists char-near-identical
in `EditInventory.java:160-174` and `SettingsInventory.java:95-109`).

## Current state

(All verified at `67beca7`.)

- `util/inventory/InventoryHandler.java` — interface with default no-op
  `onOpen/onClick/onClose(Inventory*Event)`.
- `util/inventory/InventoryManager.java` — `Map<Inventory, InventoryHandler>
  activeInventories`; `registerInventoryHandler`, `handleOpen/Click/Close` (close
  removes the entry).
- `listener/InventoryListener.java` — forwards the three inventory events to the
  manager; self-registers in its constructor.
- `util/inventory/BuildSystemHolder.java` — `implements InventoryHolder`, creates
  its inventory via `Bukkit.createInventory(this, size, title)`.
  `BuildWorldHolder extends BuildSystemHolder` adds a `BuildWorld` field.
- `util/inventory/PaginatedInventory.java` — abstract, `implements
  InventoryHandler`; fields `Map<UUID, Integer> invIndex` and `Inventory[]
  inventories`; methods `getInvIndex/resetInvIndex/setInvIndex`,
  `decrementInv/incrementInv` (play XSound chicken-egg/item-break),
  `calculateNumPages`.
- `util/inventory/InventoryUtils.java` (325 lines) — static helpers: `createItem`,
  `createSkull`, `addGlassPane`, `getColoredGlassPane` (player design-color pane),
  `addWorldItem` (async XSkull texture + PDC tagging via `DISPLAYABLE_TYPE_KEY` /
  `DISPLAYABLE_NAME_KEY`), `isNavigator`, `storeWorldInformation`.
- 16 menu classes use these (list in plan 008's scope).
- `BuildSystemPlugin` holds `InventoryManager` (`getInventoryManager()`, line 433).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.4 (menu design), §7 (threading).

## Scope

**In scope**:
- Create package `de/eintosti/buildsystem/menu/` with: `Menu.java`,
  `PaginatedMenu.java`, `MenuListener.java`, `ItemBuilder.java`, `Heads.java`
- Migrate ONE exemplar menu: `player/settings/SpeedInventory.java`
- Register `MenuListener` alongside the old `InventoryListener` (both live until
  plan 008 finishes)
- Tests for `ItemBuilder` slot math-free parts and `PaginatedMenu` paging

**Out of scope** (do NOT touch):
- The other 15 menu classes (plan 008)
- Deleting `InventoryManager`/`InventoryHandler`/`PaginatedInventory`/
  `BuildSystemHolder`/`InventoryUtils` — they still serve the unmigrated menus;
  plan 008 deletes them
- `NavigatorListener` (armor-stand navigator; plan 013 territory)
- The PDC `NamespacedKey`s used on items — keep the exact same keys (items already
  in player inventories must keep working)

## Git workflow

- Conventional commits: `refactor: add menu framework`, `refactor: migrate
  SpeedInventory to menu framework`, `test: ...`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: `Menu`

```java
@NullMarked
public abstract class Menu implements InventoryHolder {

    protected final Messages messages;
    private final Inventory inventory;

    protected Menu(Messages messages, int size, String title) {
        this.messages = messages;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public final Inventory getInventory() { return inventory; }

    protected abstract void populate(Player player);

    public void open(Player player) {
        populate(player);
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {}   // overridden by menus
    public void handleClose(InventoryCloseEvent event) {}
    public void handleOpen(InventoryOpenEvent event) {}
}
```

Notes:
- One `Menu` instance per open action (created at `open` call sites) — instance
  state is per-viewer by construction.
- Do NOT add a slot→lambda action map yet; the existing menus are switch-based and
  plan 008 ports them switch-based. (A lambda DSL can come later; it is not worth
  rewriting 16 click handlers twice.)

### Step 2: `MenuListener`

```java
public final class MenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu menu) {
            menu.handleClick(event);
        }
    }
    // same pattern for InventoryOpenEvent / InventoryCloseEvent
}
```

- `getHolder(false)` avoids a block-state snapshot; if that overload does not exist
  in spigot-api 26.1.2, use `getHolder()`.
- No self-registration: `BuildSystemPlugin.onEnable` registers it explicitly next to
  the existing listeners (one line; the full listener-registration cleanup is plan 009).

### Step 3: `PaginatedMenu`

```java
public abstract class PaginatedMenu extends Menu {
    private int page;                       // replaces Map<UUID, Integer>
    protected abstract int totalItems();
    protected final int page() { return page; }
    protected final int totalPages(int itemsPerPage) { ... }   // same math as PaginatedInventory.calculateNumPages
    protected boolean previousPage(Player player, int itemsPerPage) { ... }  // same XSound feedback as decrementInv
    protected boolean nextPage(Player player, int itemsPerPage) { ... }      // same XSound feedback as incrementInv
}
```

Replicate `PaginatedInventory`'s exact sounds and boundary behavior (verified:
page changes play `XSound.ENTITY_CHICKEN_EGG`, refusals play
`XSound.ENTITY_ITEM_BREAK`; `calculateNumPages(0, n) == 1`).

### Step 4: `ItemBuilder` + `Heads`

`ItemBuilder` — fluent, covering every shape the 16 menus build today (consult
`InventoryUtils` and the two `addSettingsItem` copies):

```java
ItemBuilder.of(XMaterial.OAK_LOG).name(displayName).lore(lines)
    .hideAttributes()                  // itemMeta.addItemFlags(ItemFlag.values())
    .glow(enabled)                     // addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1) when true
    .pdc(key, value)                   // PersistentDataContainer string entries
    .build();
```

Plus statics mirroring current helpers: `glassPane(player, settings)` (design-color
pane named " "), `skull(...)`. `Heads` owns the async world-icon texture flow —
port `InventoryUtils.addWorldItem` (with plan 002's main-thread hop) and add the
guard from 000 §6.4: before `inventory.setItem` on the main thread, skip if the
target inventory is no longer `player.getOpenInventory().getTopInventory()`.
Keep `DISPLAYABLE_TYPE_KEY`/`DISPLAYABLE_NAME_KEY` semantics identical (same
`NamespacedKey` names — copy them, don't rename).

### Step 5: Migrate `SpeedInventory` as the exemplar

`player/settings/SpeedInventory.java` is the smallest real menu. Convert it:
extend `Menu`, build items with `ItemBuilder`, move its `onClick` registration off
`InventoryManager` (no `registerInventoryHandler` call; `MenuListener` routes via
holder). Open call sites (`grep -rn "new SpeedInventory" buildsystem-core/src/main/java`)
switch to `new SpeedInventory(...).open(player)` shape.

**Verify**: compile → exit 0;
`grep -n "InventoryManager\|registerInventoryHandler" buildsystem-core/src/main/java/de/eintosti/buildsystem/player/settings/SpeedInventory.java` → no matches.

### Step 6: Tests

- `menu/PaginatedMenuTest.java`: page starts at 0; `nextPage` at last page returns
  false and stays; `previousPage` at 0 returns false; totals math matches
  `calculateNumPages` cases from plan 001's `PaginatedInventoryTest`. Mock `Player`
  for the sound calls or stub `XSound` interaction if it requires a server — if
  XSound's `play` cannot run headless, extract the sound feedback behind a
  protected method and override it in the test subclass.
- `menu/ItemBuilderTest.java`: only if `ItemStack`/`ItemMeta` work headless
  (they need the server's ItemFactory — they likely do NOT). If not constructible,
  skip ItemBuilder unit tests and note it in the plan status; do not add MockBukkit.

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 6; the real verification of the framework is plan 008's migration plus the
operator's smoke test (`/speed` menu: open, click each speed, close).

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `menu/` package exists with the five classes
- [ ] `SpeedInventory` routes through `MenuListener` (greps in step 5)
- [ ] Old path still intact for the other 15 menus (no other menu file modified)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `Inventory.getHolder()` does not return the custom holder for chest inventories
  in spigot-api 26.1.2 (API change) — the whole routing approach needs review.
- `SpeedInventory` turns out to depend on `PaginatedInventory` or on open-state
  tracked elsewhere (it shouldn't; it's a fixed-slot menu).
- `ItemBuilder` needs a capability not listed in step 4 to express an existing item
  — report the gap instead of bloating the builder speculatively.

## Maintenance notes

- Plan 008 migrates the remaining 15 menus and deletes the old plumbing — keep both
  paths working until then.
- The "skip stale async skull apply" guard in `Heads` is deliberate new behavior
  (fixes phantom updates); reviewer should be told.
- Future slot→lambda DSL: only worth it if menus start sharing layouts; revisit
  after plan 008.
