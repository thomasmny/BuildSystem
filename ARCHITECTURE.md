# BuildSystem Architecture

BuildSystem is a Minecraft (Spigot) plugin for managing build worlds. This document
describes the **as-built** architecture after the plan-002–018 refactor series.

## Modules

| Module | Role |
|--------|------|
| `buildsystem-api` | Public interfaces: `BuildSystem`, `BuildWorld`, `PlayerService`, events, API types. Coordinates are frozen; signatures never removed, only deprecated. |
| `buildsystem-core` | Implementation; JavaPlugin subclass; composition root. |

## Package tree (`buildsystem-core`)

```
de.eintosti.buildsystem
├── BuildSystemPlugin        JavaPlugin subclass; composition root ONLY
├── BuildSystemApi           adapter implementing api.BuildSystem
├── config/
│   ├── PluginConfig         immutable record tree mirroring config.yml
│   ├── ConfigService        loads/reloads/migrates; holds current snapshot
│   └── migration/           ConfigMigrationManager + versioned steps
├── i18n/
│   ├── Messages             instance service: catalog + send/get methods
│   ├── Placeholders         literal (non-regex) placeholder substitution
│   └── TextResolver         seam for external placeholder expansion (PAPI lives in integration/)
├── command/
│   ├── CommandBase          abstract: player check, permission, usage
│   ├── CommandRegistrar     registers all executors + completers
│   ├── SubCommand           name/permission/execute/complete contract
│   ├── SubCommandDispatcher generic dispatch + completion
│   └── subcommand/worlds/   one file per /worlds subcommand
├── menu/
│   ├── Menu                 abstract InventoryHolder; slot→action routing
│   ├── PaginatedMenu        per-instance page state
│   ├── MenuListener         the ONLY inventory listener; routes via holder
│   ├── ItemBuilder          fluent ItemStack construction
│   ├── InventoryUtils       stateless ItemStack factory (createItem/createSkull)
│   ├── MenuItems            injected item builders needing config/settings state
│   └── PlayerChatInput      chat-prompt UI helper; lives in menu/ (UI mechanism)
├── world/
│   ├── WorldServiceImpl     registry facade + orchestration
│   ├── BuildWorldImpl       domain object (world + data + builders)
│   ├── lifecycle/           WorldLoaderImpl, WorldTeleporterImpl, WorldUnloaderImpl,
│   │                        WorldPermissionsImpl
│   ├── creation/            BuildWorldCreatorImpl + strategies; generator/
│   ├── folder/              FolderImpl
│   ├── builder/             BuildersImpl (domain), package-info
│   ├── backup/              BackupService, BackupImpl; storage/ (local/sftp/s3)
│   ├── spawn/               SpawnService
│   ├── display/             CustomizableIcons (configurable navigator icons)
│   ├── menu/                ALL world-related menus, every one named *Menu
│   │                        (NavigatorMenu, EditMenu, CreateMenu, DeleteMenu,
│   │                        SetupMenu, GameRulesMenu, BuilderMenu, StatusMenu,
│   │                        BackupsMenu, folder/world list menus)
│   ├── data/                WorldDataImpl + type/ (ConfigurableType)
│   └── navigator/           settings/ (WorldDisplayImpl, WorldFilterImpl)
├── player/
│   ├── PlayerServiceImpl    BuildPlayer registry + session state
│   ├── BuildPlayerImpl      per-player domain object
│   ├── CachedValuesImpl     cached inventory/gamemode/speed for build-mode
│   ├── LogoutLocationImpl   serializable logout position
│   ├── settings/            SettingsService, SettingsImpl
│   ├── noclip/              NoClipService (synchronous proximity check)
│   ├── customblock/         CustomBlockManager + CustomBlockMenu (secret blocks)
│   └── menu/                DesignMenu, SettingsMenu, SpeedMenu
├── protection/
│   └── WorldProtectionPolicy  single source for "may player modify world?"
├── listener/                thin event adapters grouped by Bukkit concern
├── navigator/               NavigatorService (armor-stand "new navigator")
├── storage/                 in-memory registries + async YAML persistence
│   └── yaml/                YamlWorldStorage, YamlPlayerStorage, etc.
├── integration/             soft-dependency modules; nothing outside may import
│   ├── placeholderapi/      PlaceholderApiExpansion, Integrations activation seam
│   ├── luckperms/           LuckPermsExpansion + calculators/
│   ├── worldedit/           EditSessionListener, WorldEditCommands
│   └── axiom/               WorldManipulateByAxiomListener
├── event/                   custom Bukkit events (BuildWorldManipulationEvent, …)
└── util/                    small, stateless helpers (DirectionUtil, NumberUtils,
                             ServerModeChecker, ArgumentParser, StringCleaner,
                             MaterialUtils, UpdateChecker, color/)
```

**Divergences from the original target spec (plans/000-target-architecture.md §4)**:
- `world/builder/` kept as a package (clean domain grouping; not inlined into `world/`).
- `world/data/` and `world/data/type/` kept (clean data-layer grouping).
- `world/navigator/` kept for its `settings/` sub-package (`WorldDisplayImpl`,
  `WorldFilterImpl`). The armor-stand `NavigatorService` lives at top-level `navigator/`.
- `world/display/` kept for `CustomizableIcons` (icon customization; separate from folder logic).
- `event/` kept as a top-level package (custom events and `EventDispatcher`).
- The `BuildSystemApi` adapter lives at the root package (next to `BuildSystemPlugin`),
  per the target spec; `BuildSystemProvider.register`/`unregister` are public binding
  points only the providing plugin calls.

**Naming convention**: every class extending `menu/Menu` is named `*Menu`. `*Inventory`
is reserved for Bukkit's own `Inventory` type, never our menu classes.

## Composition root construction order

`BuildSystemPlugin.onEnable()` wires in this order:
1. `ConfigService` (loads + migrates config)
2. `Messages` (i18n catalog)
3. `CustomizableIcons` (loads setup.yml icons)
4. `CustomBlockManager`, `PlayerServiceImpl`, `NavigatorService`, `NoClipService`
5. `WorldServiceImpl` (creates `WorldStorageImpl`, `FolderStorageImpl` internally)
6. `BackupService`, `SettingsService`, `SpawnService`
7. `CommandRegistrar`, `ListenerRegistrar`
8. `Integrations.activate()` (PAPI, LuckPerms — presence-checked at activation)
9. `UpdateChecker`, `BuildSystemApi` registration, bStats metrics

**Rules**:
- No service registers itself with Bukkit in its constructor; that is the registrar's job.
- A class takes the narrowest dependencies it needs, never `BuildSystemPlugin`, unless it
  genuinely needs `JavaPlugin` (schedulers, data folder).
- Getters on the plugin class exist only for `BuildSystemApi` binding.

## Threading rules

1. **Bukkit API** (worlds, players, inventories, blocks, scoreboards): main thread only.
2. **File IO** (YAML save, world copy/delete, zip, upload) and **network** (Mojang
   profiles, SFTP/S3, update check): never on the main thread.
3. Hop pattern: validate on main → `Scheduler.runTaskAsynchronously(io)` →
   `Scheduler.runTask(apply)`.
4. Data structures crossing the boundary are immutable snapshots or `ConcurrentHashMap`.

## Extension recipes

### Add a command
1. Create `command/subcommand/<feature>/<Name>SubCommand.java` implementing `SubCommand`.
2. Register it in `CommandRegistrar.registerAll()`.
3. Colocate tab completion in the same file. Derive the permission from
   `getArgument().getPermission()` — never hardcode the node, so command and
   completion can never drift.
4. For a world-scoped `/worlds` subcommand, run the shared preamble via
   `GuardedWorldCommand.requireWorld(...)` instead of re-checking
   permission/usage/existence by hand.

### Add a menu
1. Extend `Menu` (or `PaginatedMenu`) in the appropriate `*/menu/` package, named `*Menu`.
2. Open it from a listener or command; `MenuListener` routes clicks via `InventoryHolder`.
3. No UUID maps — each `new ...Menu(...)` call is a per-open instance.

### Add an optional-plugin integration
1. Create `integration/<name>/` package with your integration class(es).
2. Expose `register()`/`unregister()` (or equivalent lifecycle methods).
3. Wire presence check + lifecycle call in `integration/Integrations.java`.
4. **Rule**: no class outside `integration/<name>/` may import soft-dependency types.

### Add a world protection rule
All "may player modify?" logic lives exclusively in `protection/WorldProtectionPolicy`.
Add a new `DenialReason` enum value and a check method there; callers call the policy,
never duplicate the guard.

## API compatibility policy

The policy is **two-phase**:

- **Now (pre-1.0, 0 external consumers)**: `buildsystem-api` signatures may be changed,
  renamed, or removed freely. New members carry useful Javadoc and `@since`.
- **After the first external release ships**: the freeze applies — adding new
  methods/types is allowed; changing signatures or removing is **never** done without
  deprecating first (`@Deprecated(forRemoval = true, since = "<version>")` +
  `@deprecated` Javadoc), kept until a major version designated for removal.

Consumer count is verified by Maven Central search before treating the API as frozen.
