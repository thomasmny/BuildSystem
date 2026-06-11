# Plan 003: Replace static Config fields with an immutable PluginConfig snapshot

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/config buildsystem-core/src/main/resources/config.yml`
> Plans 001–002 are expected to have landed; their changes do not touch these
> paths. On any other mismatch with the excerpts below, STOP.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED (touches 36 files mechanically; reload semantics change from
  field-mutation to snapshot-swap)
- **Depends on**: 001, 002
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`config/Config.java` (647 lines) exposes ~50 `public static` **mutable** fields in
nested classes (`Config.Settings.scoreboard`, `Config.World.Unload.enabled`, …),
populated by `Config.load()`. Costs: reload is non-atomic (readers can observe a
half-updated config), nothing is unit-testable without a running plugin
(`Config.java:67` does `BuildSystemPlugin.get()` in a static initializer), and a live
`BackupStorage` service object is created *inside the config class*
(`Config.java:404/545/612`), inverting the dependency direction. Target architecture
(`plans/000-target-architecture.md` §6.1): immutable record tree + `ConfigService`
with atomic snapshot swap; backup storage construction moves to `BackupService`.

## Current state

- `buildsystem-core/src/main/java/de/eintosti/buildsystem/config/Config.java` — the
  static config. Structure (verified at `67beca7`):
  - Lines 103–117 `Config.Messages` (3 fields), 122–232 `Config.Settings` with nested
    `Archive`, `DisabledPhysics`, `SaveFromDeath`, `BuildMode`, `Builder`,
    `Navigator`; 235–424 `Config.World` with nested `Default` (+ `Permission`,
    `Time`, `Settings`, `Settings.BuildersEnabled`), `Limits`, `Unload`, `Backup`
    (+ `AutoBackup`); 427–437 `Config.Folder`.
  - `load()` (lines 442–554) reads every value from `PLUGIN.getConfig()` with
    defaults inline — this is the complete key list for the records.
  - `parseGameMode` (562), `parseWorldEditWand` (578), the gamerule-parsing stream
    (483–518), and `createBackupStorage` (612–646: builds `S3BackupStorage` /
    `SftpBackupStorage` / `LocalBackupStorage` from config values).
  - `getConfig()` (75), `getVersion()` (85), `setVersion(int)` (94) — used by
    `config/migration/ConfigMigrationManager`.
- `BuildSystemPlugin.java:146` calls `Config.load()` in `onLoad`;
  `BuildSystemPlugin.reloadConfigData` (line 403) calls `reloadConfig()` then
  `Config.load()`; `registerStats()` (lines 299–320) reads several `Config.*` fields.
- `world/backup/BackupService.java` — `BackupService(BuildSystemPlugin)` (line 53),
  `reload()` (line 68), `getStorage()` (line 78); currently reads
  `Config.World.Backup.storage`.
- 61 static read sites across 36 files:
  `grep -rln "Config\.[A-Z]" buildsystem-core/src/main/java --include='*.java'`
- **Bonus bug to fix here**: the default world blacklists misspell the end
  dimension — `"worth_the_end"` instead of `"world_the_end"` at `Config.java:252`,
  `Config.java:389`, and `buildsystem-core/src/main/resources/config.yml:67` and
  `:71`. The deletion blacklist therefore does not protect `world_the_end`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |
| No stale readers | `grep -rn "Config\.[A-Z]" buildsystem-core/src/main/java --include='*.java' \| grep -v "PluginConfig\|ConfigService\|config/migration"` | no matches |

## Suggested executor toolkit

- Read `plans/000-target-architecture.md` §5 (composition root), §6.1 (config
  design), §8 (conventions) before starting.

## Scope

**In scope**:
- Create `buildsystem-core/src/main/java/de/eintosti/buildsystem/config/PluginConfig.java`
- Create `buildsystem-core/src/main/java/de/eintosti/buildsystem/config/ConfigService.java`
- Delete `buildsystem-core/src/main/java/de/eintosti/buildsystem/config/Config.java`
  (final step, after all readers migrated)
- Modify the 36 reader files (mechanical accessor swap only — no other changes)
- Modify `BuildSystemPlugin.java` (hold/construct `ConfigService`, expose
  `getConfigService()`)
- Modify `world/backup/BackupService.java` + the three `world/backup/storage/*.java`
  classes (storage construction moves here; they receive settings via constructor)
- Modify `config/migration/ConfigMigrationManager.java` (version accessors move to
  `ConfigService`)
- Fix the `worth_the_end` typo in `config.yml` and the record defaults
- New test file `config/PluginConfigTest.java`

**Out of scope** (do NOT touch):
- `Messages.java` (plan 004), command/listener/menu logic beyond the accessor swap,
  any behavior change other than the typo fix.
- Removing `BuildSystemPlugin.get()` calls in classes without a plugin reference —
  use the transitional rule below; plan 014 eliminates the singleton.

## Git workflow

- Conventional commits, suggested split: (1) records+service, (2) backup-storage
  move, (3) reader migration, (4) delete `Config`, (5) typo fix + tests.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Create the `PluginConfig` record tree

One top-level record, nested records mirroring today's nesting **exactly** (same
field names, so reader migration is a mechanical rename). Sketch (complete the
fields from `Config.load()` lines 442–554 — that method is the authoritative list of
keys, types, and defaults):

```java
public record PluginConfig(
        Messages messages, Settings settings, World world, Folder folder) {

    public record Messages(boolean spawnTeleportMessage, boolean joinQuitMessages, String dateFormat) {}

    public record Settings(boolean updateChecker, boolean scoreboard, Archive archive,
                           DisabledPhysics disabledPhysics, SaveFromDeath saveFromDeath,
                           BuildMode buildMode, Builder builder, Navigator navigator) {
        public record Archive(boolean vanish, boolean changeGamemode, GameMode worldGameMode) {}
        ...
    }

    public record World(boolean lockWeather, String invalidCharacters, int importAllDelay,
                        Set<String> deletionBlacklist, Default defaults, Limits limits,
                        Unload unload, Backup backup) {
        ...
        public record Backup(int maxBackupsPerWorld, StorageSettings storage, AutoBackup autoBackup) {
            public sealed interface StorageSettings permits Local, Sftp, S3 {}
            public record Local() implements StorageSettings {}
            public record Sftp(String host, int port, String username, String password, String path) implements StorageSettings {}
            public record S3(String url, String accessKey, String secretKey, String region, String bucket, String path) implements StorageSettings {}
            public record AutoBackup(boolean enabled, boolean onlyActiveWorlds, int interval) {}
        }
    }

    public record Folder(boolean overridePermissions, boolean overrideProjects) {}
}
```

Notes:
- `Config.World.Default` becomes record `Default` accessed as `defaults()`
  (`default` is a keyword).
- Collections passed into records must be wrapped immutably (`Set.copyOf`,
  `List.copyOf`) in a compact constructor.
- Fix the typo while transcribing defaults: `"world_the_end"`, not
  `"worth_the_end"`.
- The `Backup.storage` live object does **not** move into the record — only its
  settings (see step 3).

### Step 2: Create `ConfigService`

```java
public final class ConfigService {
    private final BuildSystemPlugin plugin;
    private volatile PluginConfig current;

    public ConfigService(BuildSystemPlugin plugin) { this.plugin = plugin; }

    public PluginConfig current() { return current; }
    public void load() { this.current = parse(plugin.getConfig()); }   // full reparse, atomic swap
    public int getVersion() { ... }       // moved from Config.getVersion
    public void setVersion(int version) { ... }
    private PluginConfig parse(FileConfiguration config) { ... }
}
```

Move into `ConfigService` unchanged in behavior: the body of `Config.load()`
(rewritten to build records bottom-up instead of assigning statics),
`parseGameMode`, `parseWorldEditWand`, the gamerule-parsing stream, and the
*settings-reading* part of `createBackupStorage` (the `switch` on
`world.backup.storage.type` now yields a `StorageSettings` record instead of a live
storage object; the unknown-type warning stays). `getVersion`/`setVersion` move
here; update `ConfigMigrationManager` to call them on the service (it runs in
`onLoad` before anything else, so pass the service into its constructor).

In `BuildSystemPlugin.onLoad`, replace `Config.load()` with construction +
`configService.load()`; in `reloadConfigData`, replace `Config.load()` with
`configService.load()`. Add a `getConfigService()` getter.

**Verify**: `./gradlew :buildsystem-core:compileJava` → may still fail (readers not
migrated); that's expected — proceed, the gate is at step 4.

### Step 3: Move backup-storage construction into `BackupService`

- Give `S3BackupStorage`, `SftpBackupStorage` constructors that accept their
  settings record (replacing the current loose string/int parameter lists — adjust,
  don't duplicate). `LocalBackupStorage` keeps `(plugin)`.
- `BackupService` builds the storage from
  `plugin.getConfigService().current().world().backup().storage()` via an exhaustive
  `switch` over the sealed interface, in its constructor and in `reload()`
  (`reload()` must `close()` the previous storage first — check `getStorage().close()`
  exists; it is called today from `BuildSystemPlugin.onDisable` line 195).
- Replace every read of `Config.World.Backup.storage` with
  `plugin.getBackupService().getStorage()`. Find them:
  `grep -rn "Backup.storage\|Backup\.storage" buildsystem-core/src/main/java`.

### Step 4: Migrate all 61 readers

Mechanical rule — for each match of `grep -rn "Config\.[A-Z]" ...`:

1. The class already has a `plugin` field (most commands/listeners/services):
   replace `Config.X.Y.z` with `plugin.getConfigService().current().x().y().z()`.
   When a method reads ≥ 2 values, bind `PluginConfig config = plugin.getConfigService().current();`
   once at the top of the method — one snapshot per operation, never cache it in a
   field.
2. The class has no plugin reference (e.g. static utilities, `WorldDataImpl`):
   use `BuildSystemPlugin.get().getConfigService().current()` and add the comment
   `// TODO(plan-014): inject` on that line. Plan 014 removes these.
3. Name mapping: `Config.World.Default.*` → `...world().defaults().*`;
   `Config.Settings.Builder.*` → `...settings().builder().*`; everything else keeps
   its name with record-accessor casing.

Then delete `config/Config.java`.

**Verify**:
- `./gradlew :buildsystem-core:compileJava` → exit 0
- `grep -rn "Config\.[A-Z]" buildsystem-core/src/main/java --include='*.java' | grep -v "PluginConfig\|ConfigService\|config/migration"` → no matches
- `ls buildsystem-core/src/main/java/de/eintosti/buildsystem/config/` → `Config.java` gone

### Step 5: Fix the typo in `config.yml` and add tests

- `config.yml` lines 67 and 71: `worth_the_end` → `world_the_end`. (Existing user
  configs keep their own list — only new installs get the fixed default; note this
  in the commit message.)
- `PluginConfigTest`: build a `YamlConfiguration` in-memory
  (`YamlConfiguration.loadFromString`) with a representative config snippet, run it
  through `ConfigService.parse` (make `parse` package-private and accept
  `FileConfiguration` — it already does), assert: values land in the right records;
  missing keys produce the documented defaults; `world.backup.storage.type: s3`
  yields an `S3` settings record; an unknown type yields `Local`. If `parse`
  cannot run without a `BuildSystemPlugin` instance (logger calls), have `parse`
  take a `java.util.logging.Logger` parameter instead of using the plugin field.

**Verify**: `./gradlew :buildsystem-core:test` → exit 0, new tests pass.

## Test plan

- `config/PluginConfigTest.java` as in step 5 (≥ 6 cases: full parse, defaults,
  each backup storage type, gamerule parsing of one boolean + one integer + one
  invalid entry).
- Existing tests from plans 001–002 must stay green.

## Done criteria

- [ ] All three table commands pass (compile, tests, no-stale-readers grep)
- [ ] `config/Config.java` deleted; `PluginConfig.java` + `ConfigService.java` exist
- [ ] `grep -rn "worth_the_end" buildsystem-core/src` → no matches
- [ ] Backup storage is constructed only in `BackupService`
      (`grep -rn "new S3BackupStorage\|new SftpBackupStorage\|new LocalBackupStorage" buildsystem-core/src/main/java` → matches only in `BackupService.java`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `Config.load()` at HEAD contains keys/sections not present in the excerpt range
  442–554 (drift — the record tree would be incomplete).
- A reader uses `Config.getConfig()` for raw `FileConfiguration` access outside
  `config/migration` — list the sites and stop; they need design judgment.
- `BackupService.reload()`'s current semantics do something other than recreate
  storage + restart the auto-backup task — report what you found.
- More than ~70 reader sites turn up (the count was 61 at planning time).

## Maintenance notes

- Reload semantics: code that previously saw mid-operation config changes now keeps
  one consistent snapshot per operation — that is the intended improvement.
  Reviewer should spot-check that no migrated site caches `PluginConfig` in a field.
- The `// TODO(plan-014): inject` markers are intentional debt; plan 014's done
  criteria grep for them.
- Plan 012 (backups) builds on the step-3 constructor shape.
