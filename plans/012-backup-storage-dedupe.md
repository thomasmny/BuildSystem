# Plan 012: Deduplicate the backup storage implementations behind a template base

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/world/backup`
> Expected drift: plan 002 (SFTP null-safety), plan 003 (settings-record
> constructors). Structural drift beyond that: STOP.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW-MED (backups are user data, but the subsystem is well isolated
  behind the `BackupStorage` interface)
- **Depends on**: 002, 003
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

Three `BackupStorage` implementations (`LocalBackupStorage` 136 lines,
`S3BackupStorage` 198, `SftpBackupStorage` 339) share the same skeleton per
operation — zip-world-to-memory, name backups by timestamp, sort newest-first,
run on `BackupService.BACKUP_EXECUTOR`, wrap errors in `RuntimeException` with
slightly different messages — each implementing it independently. A fix to (e.g.)
the zip step or executor handling must be made three times. The async/executor
scaffolding belongs in one base class; subclasses should contain only remote-medium
operations.

## Current state

(Verified at `67beca7`.)

- `api/world/backup/BackupStorage.java` — the API interface:
  `listBackups(BuildWorld) → CF<List<Backup>>`, `storeBackup(BuildWorld) → CF<Backup>`,
  `downloadBackup(Backup) → CF<File>`, `deleteBackup(Backup) → CF<Void>`, `close()`.
- `world/backup/BackupService.java` — `BACKUP_EXECUTOR`
  (`Executors.newCachedThreadPool()`, line 41 — a **static** pool, never shut
  down), backup profiles cache, auto-backup task, `reload()`, `getStorage()`.
- `world/backup/storage/SftpBackupStorage.java` — e.g. `listBackups` (161–195):
  `CompletableFuture.supplyAsync(() -> { ... sftp ops ... sort desc ... }, BackupService.BACKUP_EXECUTOR)`
  with `catch (IOException e) { disconnectAll(); throw new RuntimeException(...) }`;
  `storeBackup` (198–229): timestamp name → `FileUtils.zipWorldToMemory(buildWorld)`
  → upload → log "Backed up world '%s'. Took %sms".
- `S3BackupStorage` / `LocalBackupStorage` — same operation shapes against S3/disk
  (method line map: S3 89/119/148/167/181; Local 61/84/103/108/120).
- All three name backups via a `getBackupName(timestamp)`-style helper and resolve
  a per-world directory (`getBackupDirectory`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.5, §7.

## Scope

**In scope**:
- Create `world/backup/storage/AbstractBackupStorage.java`
- Refit the three implementations onto it
- Move `BACKUP_EXECUTOR` ownership into `BackupService` as an instance field with
  shutdown in `close()`/plugin disable (today: static, leaked on reload)
- Unit tests for `LocalBackupStorage` (the only implementation testable without
  network)

**Out of scope** (do NOT touch):
- The API `BackupStorage` interface
- Backup file naming/locations (existing backups must stay listable)
- Retry/backoff logic — considered and rejected for now (would change user-facing
  timing; revisit if operators report flaky-network failures)
- `BackupsInventory`/subcommands (plan 008/006 territory)

## Git workflow

- Conventional commits: base class; one refit per implementation; executor
  lifecycle; tests.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: `AbstractBackupStorage`

```java
@NullMarked
public abstract class AbstractBackupStorage implements BackupStorage {

    protected final BuildSystemPlugin plugin;
    private final Executor executor;

    protected AbstractBackupStorage(BuildSystemPlugin plugin, Executor executor) { ... }

    // template: async + uniform error wrapping, identical for all media
    @Override
    public final CompletableFuture<List<Backup>> listBackups(BuildWorld buildWorld) {
        return supply("list backups", () -> sortNewestFirst(doListBackups(buildWorld)));
    }
    @Override
    public final CompletableFuture<Backup> storeBackup(BuildWorld buildWorld) {
        return supply("store backup", () -> {
            long timestamp = System.currentTimeMillis();
            byte[] zip = FileUtils.zipWorldToMemory(buildWorld);
            Backup backup = doStoreBackup(buildWorld, timestamp, zip);
            logDuration(buildWorld, timestamp);
            return backup;
        });
    }
    // downloadBackup/deleteBackup analogous

    protected abstract List<Backup> doListBackups(BuildWorld buildWorld) throws IOException;
    protected abstract Backup doStoreBackup(BuildWorld buildWorld, long timestamp, byte[] zip) throws IOException;
    protected abstract File doDownloadBackup(Backup backup) throws IOException;
    protected abstract void doDeleteBackup(Backup backup) throws IOException;
    protected void onIoFailure() {}   // SFTP overrides with disconnectAll()

    protected final String backupName(long timestamp) { ... }   // today's shared naming
}
```

Before writing it, diff the three implementations operation-by-operation and adjust
the hooks so each subclass's *current* behavior is reproducible exactly (e.g. SFTP's
`disconnectAll()` on failure → `onIoFailure`; S3's temp-download directory; Local's
download being a near-no-op since files are already local). The base class must not
force behavior changes — if an operation genuinely diverges (read the bodies!),
leave that operation abstract-only and skip templating it.

### Step 2: Refit Local → S3 → SFTP (in that order, simplest first)

Per implementation: extend the base, convert each `@Override` into the
corresponding `doX` body (dropping the `supplyAsync`/sort/zip/log scaffolding the
base now owns). After each refit: compile + run tests.

### Step 3: Executor lifecycle

Move `BACKUP_EXECUTOR` from a static field on `BackupService` to an instance
`ExecutorService` created in the constructor, passed to storages, and
`shutdown()` in a new `BackupService.close()` called from `BuildSystemPlugin.onDisable`
(next to today's `backupService.getStorage().close()`, line 195 at `67beca7`).
`reload()` reuses the executor (only the storage is rebuilt).
Update the storages' references (they currently reach into
`BackupService.BACKUP_EXECUTOR` statically — constructor parameter now).

### Step 4: Tests

`world/backup/storage/LocalBackupStorageTest.java` with `@TempDir`:
- store (use a fake minimal "world" directory + a stubbed
  `FileUtils.zipWorldToMemory` if it requires Bukkit — check; if it needs a live
  `World`, test `doListBackups`/`doDeleteBackup` against pre-created zip fixtures
  instead and note the gap)
- list returns newest-first; ignores non-`.zip` files
- delete removes exactly the named file

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 4. Operator smoke: `/worlds backup` create + list + restore + delete on a
local-storage config; if SFTP/S3 credentials are available in a test environment,
one create+list each.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] All three storages extend `AbstractBackupStorage`; none contains
      `supplyAsync` (`grep -rn "supplyAsync" .../world/backup/storage/` → only in the base class)
- [ ] `grep -n "static" .../world/backup/BackupService.java` → no executor field
- [ ] Existing backup files from before the change still list correctly (naming
      untouched — verify by grep that `backupName` logic moved verbatim)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The three implementations' operations diverge beyond what the hook methods
  express (per step 1's diff) — report the divergence table instead of forcing it.
- `FileUtils.zipWorldToMemory` performs main-thread-only Bukkit calls (it would
  have been broken all along off-thread — report, don't fix here).
- Changing `BACKUP_EXECUTOR` to instance scope breaks a static reference outside
  the backup package.

## Maintenance notes

- A future storage backend (e.g. WebDAV) = one subclass with four `doX` methods.
- Retry/backoff was deliberately rejected (see Out of scope) — record renewed
  demand in an issue rather than ad-hoc adding it to one storage.
- Reviewer focus: SFTP refit (it has the most state: connection caching,
  `onIoFailure` semantics).
