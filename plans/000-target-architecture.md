# Plan 000: Target Architecture — BuildSystem designed from scratch

> **This is not an executable plan.** It is the design reference that plans 002–016
> implement. Every executor must read the sections named in its plan's "Suggested
> executor toolkit" before starting. Where a plan and this document conflict, the
> plan wins (it is newer and more specific); report the conflict in your summary.

- **Planned at**: commit `67beca7`, 2026-06-11
- **Scope**: `buildsystem-core` may be rebuilt freely. `buildsystem-api` is a published
  artifact — existing public types/methods must keep working (deprecate, never delete).

---

## 1. What this system is

BuildSystem is a Spigot plugin for build servers: players create, manage, and browse
worlds ("BuildWorlds") through chest-GUI navigators; each world has data (status,
project, permission, visibility, builders), lifecycle (load/unload on demand), and
protection rules (who may build where). Cross-cutting features: per-player settings,
per-world backups (local/SFTP/S3), folders to group worlds, spawn management, and
integrations (LuckPerms, PlaceholderAPI, WorldEdit, AxiomPaper).

The redesign keeps the **feature set and runtime behavior identical** (except for the
bugs fixed in plan 002) and replaces the internal structure.

## 2. Design goals

1. **One composition root, zero static state.** All wiring happens in
   `BuildSystemPlugin#onEnable` (composition root). No `BuildSystemPlugin.get()`,
   no static mutable fields, no self-registering constructors. Dependencies arrive
   via constructors.
2. **Domain logic lives in services; adapters stay thin.** Commands, listeners, and
   menus translate Bukkit events into service calls. A rule that exists in two
   adapters belongs in a service or policy object.
3. **Honest threading.** Bukkit API only on the main thread. File/network IO only off
   it. Every async hop is explicit through one small `Scheduler` utility.
4. **Immutable configuration snapshots.** Config is a tree of Java records loaded
   once and swapped atomically on reload.
5. **Modern Java 25, modern Bukkit.** Records for values, sealed interfaces where the
   API already uses them, pattern matching, switch expressions,
   `InventoryHolder`-routed menus, `PersistentDataContainer` for item identity.
   NOT adopted: Adventure/Component chat API (this plugin targets spigot-api with
   legacy `&` color codes throughout `messages.yml`; XSeries stays for cross-version
   material/sound handling).
6. **Comment policy** (from the maintainer): no comments that restate code. Public
   API methods in `buildsystem-api` get useful Javadoc. Internal code documents
   *non-obvious design decisions only*.

## 3. Module layout (unchanged)

```
buildsystem-api/    published API: interfaces, events, exceptions  (binary compatible)
buildsystem-core/   the plugin implementation                       (free to rebuild)
```

## 4. Target package tree (buildsystem-core)

```
de.eintosti.buildsystem
├── BuildSystemPlugin            JavaPlugin subclass; composition root ONLY
├── BuildSystemApi               adapter implementing api.BuildSystem (exists today)
├── config/
│   ├── PluginConfig             record tree mirroring config.yml (immutable)
│   ├── ConfigService            loads/reloads/migrates; holds current snapshot
│   └── migration/               existing ConfigMigrationManager + steps (kept)
├── i18n/
│   ├── Messages                 instance service: catalog + send/get methods
│   ├── MessageFile              bundled-defaults + user-file merge logic
│   └── Placeholders             literal (non-regex) placeholder substitution
├── command/
│   ├── CommandBase              abstract: player check, permission, usage, messages
│   ├── CommandRegistrar         registers every executor+completer in one place
│   ├── SubCommand               name/permission/execute/complete contract
│   ├── SubCommandDispatcher     generic "/cmd <sub> ..." dispatch + completion
│   └── <feature commands>       one file per command, completion colocated
├── menu/
│   ├── Menu                     abstract, implements InventoryHolder; slot→action
│   ├── PaginatedMenu            page state per menu instance (no UUID maps)
│   ├── MenuListener             the ONLY inventory listener; routes via holder
│   ├── ItemBuilder              fluent ItemStack construction (name/lore/glow/pdc)
│   └── Heads                    async skull texture fetch, main-thread apply
├── world/
│   ├── WorldService             registry facade + orchestration (impl of api)
│   ├── BuildWorldImpl ...       domain objects (world, data, builders)
│   ├── lifecycle/               loader, unloader, teleporter (per-world helpers)
│   ├── creation/                creator orchestrator + strategies (§6.6)
│   ├── folder/                  FolderImpl + folder logic
│   ├── backup/                  BackupService + storage/ (local, sftp, s3)
│   ├── spawn/                   SpawnService
│   └── menu/                    world-related menus (navigator, edit, create, ...)
├── player/
│   ├── PlayerService            BuildPlayer registry + session state
│   ├── settings/                Settings domain + SettingsService (scoreboard etc.)
│   ├── noclip/                  NoClipService (sync timer)
│   ├── customblock/             CustomBlocks (secret blocks menu + placement)
│   └── menu/                    settings/speed/design menus
├── protection/
│   └── WorldProtectionPolicy    THE single source of "may player modify world?"
├── listener/                    thin adapters, grouped by feature (§6.8)
├── storage/                     repositories: in-memory registry + yaml persistence
│   └── yaml/
├── integration/
│   ├── luckperms/  placeholderapi/  worldedit/  axiom/
├── navigator/                   armor-stand "new navigator" (NavigatorService)
└── util/                        small, stateless helpers only
```

Old → new mapping highlights (full table in each plan):

| Today | Target |
|---|---|
| `Messages` (static, 1102 lines) | `i18n/Messages` instance + bundled `messages.yml` resource |
| `config/Config` (static fields) | `config/PluginConfig` records + `ConfigService` |
| `util/inventory/*` + per-feature `*Inventory` | `menu/` framework + feature menus |
| `command/tabcomplete/*` | deleted; completion lives on each command/subcommand |
| `world/modification/*Inventory` | `world/menu/` |
| `player/settings/NoClipManager` | `player/noclip/NoClipService` |
| listener "isValid/canBypass" private methods | `protection/WorldProtectionPolicy` |
| `BuildSystemPlugin.get()` | constructor injection from composition root |

## 5. Composition root

`BuildSystemPlugin` contains **only**: `onLoad`, `onEnable`, `onDisable`, and final
fields for the services it wires. Construction order:

```java
// onEnable, in order; constructor args show the dependency DAG
ConfigService config = new ConfigService(this);                 // loads + migrates
Messages messages = new Messages(this, config);
WorldStorages storages = ...;                                   // repositories
WorldService worlds = new WorldService(this, config, messages, storages, ...);
PlayerService players = new PlayerService(this, config, storages, ...);
WorldProtectionPolicy protection = new WorldProtectionPolicy(config, worlds);
...
new CommandRegistrar(this, /* deps */).registerAll();
new ListenerRegistrar(this, /* deps */).registerAll();
```

Rules:
- No service registers itself with Bukkit in its constructor. Registration is the
  registrar's job. (Today every command/listener self-registers — that pattern dies.)
- A class takes the narrowest dependencies it needs (`Messages`, `WorldService`),
  never the plugin, unless it genuinely needs `JavaPlugin` (schedulers, data folder).
- Getters on the plugin class exist only for `BuildSystemApi` to bind the API.

## 6. Subsystem designs

### 6.1 Config

`PluginConfig` is a tree of nested records, one per config.yml section, e.g.:

```java
public record PluginConfig(Messages messages, Settings settings, World world, Folder folder) {
    public record Settings(boolean updateChecker, boolean scoreboard, Archive archive, ...) {
        public record Archive(boolean vanish, boolean changeGamemode, GameMode worldGameMode) {}
    }
    ...
}
```

`ConfigService` parses `FileConfiguration` → `PluginConfig` in one place, holds a
`volatile PluginConfig current`, and `reload()` builds a fresh tree then swaps the
reference (readers never see a half-loaded config). Services that need config receive
the `ConfigService` and call `config.current().settings()...` at use time — never
cache primitive values across reloads unless the field is documented reload-exempt.
The existing `config/migration/` machinery is kept as-is.

### 6.2 Messages (i18n)

- Defaults ship as a real `messages.yml` **resource in the jar** (generated once from
  the 483 `setMessage(...)` calls in today's `Messages.createMessageFile`), not as
  Java code. On startup: copy resource if absent, merge missing keys into the user
  file (preserving user edits), load into an immutable `Map<String, String>`.
- `Messages` is an instance: `send(CommandSender, String key, Map.Entry<String,Object>... placeholders)`,
  `get(...)`, `getList(...)`, `sendPermissionError(...)` — same call shapes as today
  so migration is mechanical.
- Placeholder substitution is **literal string replacement** (today's
  `String.replaceAll` regex bug is fixed in plan 002 and must not be reintroduced).
- PlaceholderAPI presence is checked at call time via the integration module, not in
  a static initializer.

### 6.3 Command framework

```java
abstract class CommandBase implements CommandExecutor, TabCompleter {
    // ctor takes (JavaPlugin plugin, Messages messages, String commandName)
    protected abstract boolean run(Player player, String[] args);
    protected List<String> complete(Player player, String[] args) { return List.of(); }
    // onCommand: resolves Player (console → "sender_not_player" warning, identical
    // to today's behavior), delegates to run(). onTabComplete delegates to complete().
    // Shared helpers: requirePermission(player, perm), resolveWorld(args, index),
    // resolveBuildWorld(args, index) — the blocks duplicated across 16 commands today.
}
```

- `CommandRegistrar` maps command name → instance and calls
  `plugin.getCommand(name).setExecutor/setTabCompleter`. The nine
  `command/tabcomplete/*` classes are deleted; their logic moves into `complete()`
  of the owning command so permission checks can never drift between execution and
  completion again.
- `/worlds` (and any future multi-arg command) uses `SubCommandDispatcher`:
  subcommands are **registered once** (a `Map<String, SubCommand>` built in the
  command's constructor), not re-instantiated per invocation; each `SubCommand`
  carries `name()`, `permission()`, `execute(Player, String[])`, and
  `complete(Player, String[])`. `WorldsTabCompleter`'s 301 lines dissolve into the
  per-subcommand `complete()` methods.

### 6.4 Menu framework

The single biggest structural change. Modern Bukkit idiom:

```java
abstract class Menu implements InventoryHolder {
    private final Inventory inventory;   // Bukkit.createInventory(this, size, title)
    protected Menu(int size, String title) { ... }
    @Override public Inventory getInventory() { return inventory; }
    protected abstract void populate(Player player);
    protected void onClick(InventoryClickEvent event) { /* dispatch to slot actions */ }
    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action);
    public void open(Player player) { populate(player); player.openInventory(inventory); }
}
```

- `MenuListener` is the **only** inventory listener:
  `if (event.getInventory().getHolder(false) instanceof Menu menu) menu.onClick(event);`
  No `InventoryManager` map, no title matching, no leak on quit — GC of the menu
  instance is the cleanup.
- One menu instance per (player, open) — page index is a plain `int` field on
  `PaginatedMenu`. The `Map<UUID, Integer> invIndex` pattern is gone.
- `ItemBuilder` covers every item shape used today: material+name+lore, toggle items
  (enchant glow), colored glass filler from player design settings, skulls, PDC
  tagging (`DISPLAYABLE_TYPE_KEY` etc. keep their NamespacedKeys for item
  compatibility with items already in player inventories).
- `Heads`: skull textures resolve async (XSkull `applyAsync`) but the inventory
  mutation is rescheduled onto the main thread, and is skipped if the menu is no
  longer the player's open inventory.

### 6.5 World domain

- `WorldService` (implements `api.world.WorldService`): owns the world registry
  facade and the orchestration flows (create/import/rename/delete/unimport).
  Flows are explicit methods with a strict thread shape:
  main (validate, unload, snapshot Bukkit state) → async (file IO only) → main
  (registry mutation, Bukkit world recreate, player teleport, messages).
  **Registry maps are mutated on the main thread only.**
- `world/lifecycle/`: today's `WorldLoaderImpl`/`WorldUnloaderImpl`/
  `WorldTeleporterImpl` keep their API contracts but get dependencies via
  constructor; unload timers are tracked centrally so disable/reload cancels them.
- `storage/`: repository per aggregate (worlds, folders, players, spawn, setup —
  matching today's `storage/yaml/*`). In-memory registries use `ConcurrentHashMap`;
  YAML writes happen on a single-threaded IO executor; `save()` futures are joined
  on shutdown (as today). Delete flows persist synchronously-enough that a crash
  cannot resurrect a deleted world.

### 6.6 World creation

Today's 611-line `BuildWorldCreatorImpl` splits along its existing seams:

- `BuildWorldCreator` (API) keeps its fluent builder contract — the impl keeps the
  setters and orchestrates only.
- `WorldSource` strategy: `GeneratorWorldSource`, `TemplateWorldSource`,
  `StandardWorldSource` — each produces a Bukkit `World` for a creation request.
- `WorldDataVersionGuard`: the NBT `level.dat` data-version parsing/updating
  (`parseDataVersion`, `updateWorldDataVersion`, `isDataVersionTooHigh`,
  `renameIncorrectWorldTypeFile`).
- `GenerationDataStore`: read/write of per-world generation metadata
  (`saveGenerationData`, `loadGenerationData`).
- `BukkitWorldFactory`: `WorldCreator` assembly + default gamerules/difficulty
  (`createBukkitWorldCreator`, `applyDefaultWorldSettings`, `applyGameRule`,
  `applyPostGenerationSettings`).
- Post-creation teleport + success message stay in the orchestrator.

### 6.7 Player domain

- `PlayerService` (implements `api.player.PlayerService`): BuildPlayer registry,
  join/quit session handling, logout locations, cached values.
- `settings/SettingsService`: scoreboard display (FastBoard), settings application.
- `noclip/NoClipService`: the block-proximity check moves to a **synchronous**
  repeating task (it reads world blocks, which is not legal async); collections
  become plain since everything is main-thread.
- `customblock/`: secret-blocks menu + placement transformation, unchanged in
  behavior, registered exactly once.

### 6.8 Protection policy and listeners

`WorldProtectionPolicy` answers, in one place, the question currently re-implemented
in `SettingsInteractListener.isValid`, `PlayerCommandPreprocessListener`,
`EditSessionListener`, `WorldManipulateListener`, and `BuildModePreventationListener`:

```java
public enum DenialReason { NONE, ARCHIVED, NOT_A_BUILDER, WORLD_SETTING_DISABLED }
DenialReason mayModify(Player player, BuildWorld world);            // archive/builder/bypass
DenialReason mayModify(Player player, BuildWorld world, Type<Boolean> setting);
boolean canBypass(Player player, BuildWorld world);                  // build-mode, permission
```

Listeners stay one-file-per-Bukkit-concern but become thin: resolve `BuildWorld`,
ask the policy or a service, act. The repeated
`getBuildWorld(...) == null || data.physics().get()` guard collapses into a helper.
`PlayerCommandPreprocessListener`'s 170-entry hardcoded WorldEdit command blacklist
moves into a dedicated `WorldEditCommands` constant class in `integration/worldedit/`
(kept in code — it is integration knowledge, not user config — but out of the
listener).

### 6.9 Integrations

Each optional plugin gets a self-contained module in `integration/<name>/` with a
single activation check in the composition root (as `registerExpansions()` does
today). Integration classes must not be referenced from anywhere else (class-loading
safety when the soft dependency is absent).

### 6.10 API binding (buildsystem-api)

- The API module is already interface-clean (services, model interfaces, events with
  proper `HandlerList`s, sealed `Builder` with package-private `BuilderImpl`).
  It does **not** get restructured.
- Compatibility rule for every plan: public API types/methods may gain Javadoc,
  `@Deprecated(forRemoval = true, since = "3.1.0")` plus a `@deprecated` Javadoc tag
  pointing at the replacement — but signatures never change and nothing is removed.
- `BuildSystemApi` (core) remains the adapter that exposes core services through API
  interfaces.

## 7. Threading model (binding rules for all plans)

1. Bukkit API calls (worlds, players, inventories, blocks, scoreboards): main thread.
2. File IO (YAML save, world folder copy/delete, zip), network (Mojang profiles,
   SFTP/S3, update check): never on the main thread.
3. The hop pattern is always: validate on main → `Scheduler.async(io)` →
   `Scheduler.sync(apply)`. `util/Scheduler` is a ~30-line wrapper over
   `BukkitScheduler` so call sites read uniformly and tests can fake it.
4. Data structures crossing the boundary are immutable snapshots or concurrent maps.

## 8. Conventions for every executor

- License header: copy the exact 17-line GPL header from any existing file.
- `@NullMarked` on every new class; `@Nullable` from jspecify where needed.
- Naming: services end in `Service`, repositories in `Storage` (matching the API),
  menus in `Menu`, listeners in `Listener`, records describing config in the
  `PluginConfig` tree.
- No `*Util` dumping grounds for domain logic — utils are stateless and generic.
- Imports: no wildcard imports (matches existing style).
- Commit style: conventional commits (`refactor: ...`, `fix: ...`, `test: ...`).
- When porting a file, modernize it: records for immutable values, switch
  expressions, pattern matching for instanceof, `String.formatted`, `List.of`.
- Comment policy as in §2.6 — when in doubt, delete the comment.

## 9. Migration strategy (why the plans are ordered as they are)

Strangler pattern: new packages are built next to the old ones, callers are switched
subsystem-by-subsystem, old classes are deleted the moment their last caller moves.
The plugin must compile and behave identically after every plan. Order:

1. **001** tests, **002** bug fixes (fix known bugs first so they aren't ported).
2. **003 config / 004 messages** — leaf dependencies of everything else.
3. **005 command framework, 006 worlds subcommands** — biggest adapter layer.
4. **007 menu framework, 008 menu migration** — second adapter layer.
5. **009 protection policy + listeners.**
6. **010 world service/storage flows, 011 creation split, 012 backups.**
7. **013 player domain, 014 composition root + DI completion** (singleton dies here).
8. **015 API javadoc/deprecations, 016 final structure conformance + docs + sweep.**
