# Changelog

All notable changes to **BuildSystem** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 4.0.0

Major release. Breaking changes for `buildsystem-api` consumers and for two
config toggles. `config.yml` and `messages.yml` migrate automatically.

### Requirements

- Java 25. Older runtimes will not start the plugin.
- Minecraft 1.21 minimum.

### Added

- **Custom world statuses.** Statuses are no longer a fixed enum — admins create,
  restyle, reorder, and delete them in-game (`/setup` → World Statuses). Each
  status has its own name, colour, icon, ordering, a building-allowed flag, and an
  optional auto-progress target; whether a status appears in the navigator is
  decided by category membership, not a per-status flag. The six built-ins
  (`not_started`, `in_progress`, `almost_finished`, `finished`, `archive`,
  `hidden`) are seeded and can be restyled or deleted (never the last remaining
  status). Assign with `buildsystem.setstatus.<id>` — built-in ids drop
  underscores for backwards compatibility, custom ids are used verbatim.
- **Navigator categories.** The fixed public/archive/private navigator sections
  become fully customizable categories. A category groups worlds by visibility +
  a set of statuses, with its own name/colour/icon. A world is shown in *every*
  category that groups it, so overlapping categories each list it. Create with
  `buildsystem.create.<categoryId>`; admins with the create-permission bypass may
  create in any category.
- **In-game `/setup` GUI.** A redesigned hub branching to editors for the default
  world-type icons, world statuses, and the navigator layout — with a 16-colour
  dye picker, a scrollable/filterable item picker, optional player-head skull
  textures, and confirm/reset controls. "Reset to defaults" restores the
  built-ins (world statuses, navigator categories, and default world-type icons);
  deleting the storage file does too.
- **Navigator layout editor** (`/setup` → Navigator Layout). One menu both manages
  categories and arranges where each category and the settings button appear in
  the inventory navigator: pick a category (or the settings button) up and place
  it into a navigator slot, drop it outside to remove it, drop it on the delete
  target to delete the category, right-click to edit it, and create new categories
  from within the same screen. The editor temporarily takes over the player's
  inventory for its controls and restores it on close, quit, or shutdown.
- **Per-world, per-folder, per-category skull-texture icons.** When an icon is a
  player head, a custom texture (or the `%viewer%` sentinel for the viewing
  player's own head) can be configured.
- World-editor toggles now show their current value (`Currently: Enabled/Disabled`)
  in the lore, in addition to the enchant glint.
- Pinned worlds (#319). Pinned worlds sort above all others in every navigator
  list regardless of the active sort and show a configurable prefix. Toggle via
  the edit menu; permission `buildsystem.edit.pin`.
- `/worlds saveTemplate <world> [templateName]` (#449). Copies a world into
  `templates/` at runtime. Permission `buildsystem.savetemplate`.
- Per-option `/settings` permissions: `buildsystem.setting.<option>` (#250).
- Per-template and per-generator create permissions:
  `buildsystem.create.template.<name>`, `buildsystem.create.generator.<name>`
  (#322, #323). All default to granted; deny a node to restrict it.
- `settings.world-permission-whitelist`: restrict the values `/worlds
  setPermission` may assign to a configured list.

### Changed

- `config.yml`/`messages.yml` are versioned and migrated on load.
- API: `BuildWorldManipulationEvent` moved to
  `de.eintosti.buildsystem.api.event.world`.
- API: world creation/import is now fluent — `WorldService.newWorld(name)` /
  `importWorld(name)`, terminated with `build()`.
- API: `Type<T>` renamed to `Property<T>`.
- API: `BuildWorld.getWorld()` returns `Optional<World>` (was nullable `World`).
- API: `BuildWorldStatus` is now an interface resolved through the new
  `WorldStatusRegistry`, not an enum. `BuildWorldStatusChangeEvent` carries
  interface instances.
- API: the `NavigatorCategory` enum becomes an interface backed by the new
  `NavigatorCategoryRegistry`; `Folder.getCategory()` returns it.
- API: world public/private is replaced by a `Visibility`
  (`EVERYONE`/`ADDED_PLAYERS`) on `WorldData`
  (`getVisibility()`/`setVisibility()`); `isPrivateWorld()`/`setPrivateWorld()`
  are gone.
- API: the instance is registered through Bukkit's `ServicesManager`; obtain it
  via `BuildSystemProvider.get()`.

### Added (API)

- World lifecycle events in `event.world`: `BuildWorldCreateEvent` (cancellable),
  `BuildWorldPostCreateEvent`, `BuildWorldDeleteEvent` (cancellable),
  `BuildWorldPostDeleteEvent`, `BuildWorldUnimportEvent`, `BuildWorldRenameEvent`,
  `BuildWorldStatusChangeEvent`.
- Backup and folder events in `event.backup` and `event.folder`.
- `WorldService.importWorlds()` for bulk import, spread across ticks.
- Typed `WorldData` access: every setting is read and written through a
  `WorldDataKey<T>` catalog — `data.get(WorldDataKey.PERMISSION)` /
  `data.set(WorldDataKey.BLOCK_BREAKING, true)` — instead of a getter/setter per
  setting (`getCustomSpawnLocation()` remains as a helper). The built-in keys
  (`PERMISSION`, `PROJECT`, `STATUS`, `DIFFICULTY`, `BLOCK_BREAKING`, …) live on
  `WorldDataKey`.
- `WorldStatusRegistry` and `NavigatorCategoryRegistry`, exposed via
  `BuildSystem.getStatusRegistry()` / `getNavigatorCategoryRegistry()`.
- `Displayable.getIconSkullTexture()` and `getHeadProfile()` for custom head
  icons.

### Removed

- API: `BuildWorld.setLoaded(boolean)` (`isLoaded()` remains).
- API: `WorldData.getConfigFormat()` and `WorldData.getAllData()`.
- API: `BuildSystemProvider.register()` / `unregister()` are no longer public.
- API: `BuildWorld.asProfileable()` — head icons are now resolved through
  `Displayable.getHeadProfile()` / `getIconSkullTexture()`.

### Fixed

- Paper 26.1: `IncompatibleClassChangeError` on `org.bukkit.GameRule` (a class on
  spigot-api, an interface on Paper). Game-rule access goes through XSeries
  `XGameRule` so the plugin runs on both (#457).
- Game-rule names updated to the current API.
- Main-thread safety: NoClip block check, world-rename registry mutation (spawn
  captured before unload), and post-async-skull inventory updates now run on the
  main thread; async menu callbacks hardened.
- Backups: bounded `BackupService` thread pool; storage credentials validated
  before use; SFTP connection failures raised as `IOException` instead of `null`.
- World rename persistence and unload-time parsing.
- Duplicate listener registration on reload.
- Placeholder substitution is literal, not regex-based.
- Inventory clicks are matched against the menu inventory only; a click in the
  player's own inventory no longer triggers the menu button in the same slot.

### Security

- Path-traversal guards on template and world directory resolution. World names
  are also checked at the public `WorldService.newWorld(String)` and on deletion,
  so a name resolving outside the world container (e.g. `../../plugins/x`) is
  rejected rather than creating or deleting a directory outside it (#481).

### Performance

- Navigator armor stands cached, removing a per-tick entity scan.
- NoClip check no longer allocates a `Location` per tick.
- Reduced allocation/work in scoreboard rendering, color processing, and the
  template menu.

### Migration (server admins)

The following happen automatically on the first 4.0.0 boot — no manual steps
required for a working server:

- **World statuses** — stored enum names (`IN_PROGRESS`) are lowercased to the
  new id format (`in_progress`). Unknown values fall back to `not_started`.
- **World visibility** — the per-world `private: true/false` boolean is converted
  to `ADDED_PLAYERS`/`EVERYONE`. Nothing is lost.
- **Folder categories** — stored upper-case enum values (`PUBLIC`, `ARCHIVE`,
  `PRIVATE`) are lowercased to the matching built-in category id. Unknown values
  fall back to `public`.
- **Status display names** — if `statuses.yml` does not yet exist, each built-in
  status is seeded from the server's existing `status_<id>` key in `messages.yml`
  (colour code split off into `color`, remainder becomes `displayName`). After
  that first boot, `statuses.yml` is authoritative; the `status_*` keys in
  `messages.yml` are unused and can be deleted manually.
- **`config.yml` / `messages.yml`** — both files are versioned and missing keys
  are added on load; existing customisations are preserved.

Two config flags removed in 4.0.0 — if present they are silently ignored:

- `settings.per-option-permissions` (superseded by the always-on
  `buildsystem.setting.<option>` nodes).
- `settings.restrict-template-access` (superseded by the deny-to-restrict
  `buildsystem.create.template.<name>` model).

### Upgrade notes (API consumers)

Recompile against `buildsystem-api` 4.0.0. Update `BuildWorldManipulationEvent`
imports to `api.event.world`, migrate to `newWorld`/`importWorld` + `build()`, and
adapt to `Property<T>` and `Optional<World>`. `BuildWorldStatus` and
`NavigatorCategory` are now interfaces — compare them by id (`equals()` /
`getId()`), never with `==` or `switch`. Replace `BuildWorld.asProfileable()` with
the `Displayable` head-profile hooks.

**Full changelog**: https://github.com/thomasmny/BuildSystem/compare/3.0.2...4.0.0
