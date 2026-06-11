# Plan 002: Fix verified bugs before any restructuring

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/BuildSystemPlugin.java buildsystem-core/src/main/java/de/eintosti/buildsystem/Messages.java buildsystem-core/src/main/java/de/eintosti/buildsystem/util/inventory buildsystem-core/src/main/java/de/eintosti/buildsystem/player/settings/NoClipManager.java buildsystem-core/src/main/java/de/eintosti/buildsystem/world/WorldServiceImpl.java buildsystem-core/src/main/java/de/eintosti/buildsystem/world/backup/storage/SftpBackupStorage.java`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED (behavior changes — each fix is small and independently verifiable)
- **Depends on**: 001 (test harness must exist; this plan adds tests where possible)
- **Category**: bug
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

Plans 003–016 rebuild the internals while preserving behavior. These seven bugs are
*current* behavior; fixing them first means the rebuild ports correct behavior, and
each fix lands as an isolated, reviewable change instead of being buried inside a
refactor diff. All seven were verified by reading the code at commit `67beca7`.

## Current state — the seven bugs

### Bug A — CustomBlockManager registered twice (duplicate event handling)

`BuildSystemPlugin.java:231` (in `initClasses()`):
```java
this.customBlockManager = new CustomBlockManager(this);
```
`BuildSystemPlugin.java:276` (in `registerListeners()`):
```java
new CustomBlockManager(this);
```
`CustomBlockManager.java:51-54` — the constructor self-registers as a listener:
```java
public CustomBlockManager(BuildSystemPlugin plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
}
```
Two instances ⇒ `onCustomBlockPlace` (a `BlockPlaceEvent` handler) runs twice per
block placement.

### Bug B — placeholder substitution treats values as regex replacements

`Messages.java:1086-1096`:
```java
return Arrays.stream(placeholders)
        .map(entry -> (Function<String, String>) data -> data.replaceAll(entry.getKey(), String.valueOf(entry.getValue())))
        .reduce(Function.identity(), Function::andThen)
        .apply(query);
```
`String.replaceAll` interprets the key as a regex and the value as a replacement
template: a world/project/player value containing `$` or `\` corrupts the message or
throws `IllegalArgumentException`/`IndexOutOfBoundsException`.

### Bug C — inventory mutated off the main thread after async skull fetch

`InventoryUtils.java:190-205` (`addWorldItem`):
```java
XSkull.createItem()
        .profile(...)
        .fallback(buildWorld.asProfilable())
        .lenient()
        .applyAsync()
        .thenAcceptAsync(itemStack -> {
            ...
            inventory.setItem(slot, itemStack);   // runs on ForkJoinPool — illegal
        });
```
Bukkit inventories must only be mutated on the main thread.

### Bug D — NoClipManager: async world reads + unsynchronized shared collections

`NoClipManager.java:52-54`:
```java
private void runBlockCheckTask() {
    Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkForBlocks, 0L, 4L);
}
```
`checkForBlocks()` (lines 56-95) iterates `noClipPlayers` (a plain `HashSet`
mutated from the main thread by `startNoClip`/`stopNoClip`) and calls
`player.getLocation()` / `block.getType()` (via `checkNoClip`, lines 97-114) from
the async thread. Both are thread-rule violations; the set iteration can throw
`ConcurrentModificationException`.

### Bug E — world rename: registry mutated off-main + spawn read after unload + no-op

`WorldServiceImpl.java:321-343` (inside `renameWorld`):
```java
Bukkit.unloadWorld(oldWorld, true);
Bukkit.getWorlds().remove(oldWorld);          // (E3) no-op: getWorlds() returns a copy
...
CompletableFuture.runAsync(() -> { /* file copy */ }).thenRunAsync(() -> {
    buildWorld.setName(sanitizedNewName);     // (E1) registry + domain mutated on
    worldStorage.addBuildWorld(buildWorld);   //      a ForkJoinPool thread
    worldStorage.save(buildWorld);
}).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
    World newWorld = new BuildWorldCreatorImpl(plugin, buildWorld).generateBukkitWorld(false);
    Location spawnLocation = oldWorld.getSpawnLocation();   // (E2) world already unloaded
    ...
```

### Bug F — SFTP client used without null check

`SftpBackupStorage.java:144-154` — `getSftpClient()` is `@Nullable` (returns the
field even when `establishConnection()` failed to open it). Callers at lines 167,
213, 277, 299 dereference the result directly:
```java
SftpClient sftp = getSftpClient();
createDirectoryIfNotExists(sftp, backupDirectory);   // NPE if connection failed
```

### Bug G — InventoryManager.handleClose removes twice

`InventoryManager.java:95-102`:
```java
public void handleClose(InventoryCloseEvent event) {
    Inventory inventory = event.getInventory();
    InventoryHandler handler = this.activeInventories.remove(inventory);
    if (handler != null) {
        handler.onClose(event);
        unregisterInventoryHandler(inventory);   // second remove of the same key — dead call
    }
}
```
Harmless today, but it ships confusion into the menu-framework port (plan 007).
Also `InventoryManager.java:36` declares `public final Map<Inventory, InventoryHandler> activeInventories;` —
should be `private`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0, all pass |

## Suggested executor toolkit

- Read `plans/000-target-architecture.md` §7 (threading model) — bugs C, D, E are
  instances of those rules being violated.

## Scope

**In scope** (the only main-source files you should modify):
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/BuildSystemPlugin.java` (bug A)
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/Messages.java` (bug B)
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/util/inventory/InventoryUtils.java` (bug C)
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/player/settings/NoClipManager.java` (bug D)
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/world/WorldServiceImpl.java` (bug E)
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/world/backup/storage/SftpBackupStorage.java` (bug F)
- `buildsystem-core/src/main/java/de/eintosti/buildsystem/util/inventory/InventoryManager.java` (bug G)
- New test files under `buildsystem-core/src/test/java/` (bug B test)

**Out of scope** (do NOT touch):
- The `PaginatedInventory.invIndex` per-player map leak — resolved by design in
  plan 007 (menu instances become per-open, page index becomes a field). Do not add
  a quit-listener cleanup; it would be throwaway work.
- Any restructuring, renaming, or package moves — this plan changes the minimum
  number of lines per fix.
- `S3BackupStorage`/`LocalBackupStorage` — reviewed; the null-client pattern is
  SFTP-specific.

## Git workflow

- One commit per bug, conventional style: `fix: <what>` (e.g.
  `fix: prevent duplicate CustomBlockManager listener registration`).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1 (Bug A): delete the duplicate instantiation

In `BuildSystemPlugin.registerListeners()` delete the line `new CustomBlockManager(this);`
(line 276). Do not touch `initClasses()`.

**Verify**: `grep -n "new CustomBlockManager" buildsystem-core/src/main/java/de/eintosti/buildsystem/BuildSystemPlugin.java` → exactly 1 match, inside `initClasses()`. Compile → exit 0.

### Step 2 (Bug B): literal placeholder replacement

In `Messages.replacePlaceholders` (line ~1087), replace the `data.replaceAll(...)`
lambda body so both key and value are treated literally. Placeholder keys are plain
tokens like `%world%`, so the correct minimal fix is:
```java
data -> data.replace(entry.getKey(), String.valueOf(entry.getValue()))
```
(`String.replace` is literal substring replacement — no regex semantics.)

Add `buildsystem-core/src/test/java/de/eintosti/buildsystem/MessagesPlaceholderTest.java`
testing the substitution logic. `replacePlaceholders` is `private static`; make it
package-private (drop `private`) with no other signature change so the test can call
it. Cases: simple `%world%` → name; value containing `$1`; value containing `\`;
multiple placeholders; placeholder absent from the template.
Note: `Messages`'s static initializer calls `BuildSystemPlugin.get()` (line 53) —
if class-loading `Messages` in a plain JUnit test throws, extract the method into a
new final class `de/eintosti/buildsystem/util/Placeholders.java` (same package
convention, static method, called from `Messages`) and test that class instead.

**Verify**: `./gradlew :buildsystem-core:test` → new tests pass.

### Step 3 (Bug C): apply skull textures on the main thread

In `InventoryUtils.addWorldItem`, replace the `thenAcceptAsync(...)` consumer so the
mutation is scheduled back onto the main thread:
```java
.thenAcceptAsync(itemStack -> {
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(displayName);
    itemMeta.setLore(lore);
    itemStack.setItemMeta(itemMeta);
    storeWorldInformation(itemStack, buildWorld);
    Bukkit.getScheduler().runTask(PLUGIN, () -> inventory.setItem(slot, itemStack));
});
```
(`PLUGIN` is the existing static field in `InventoryUtils`; meta manipulation on the
async thread is fine — only `inventory.setItem` must hop.)

**Verify**: compile → exit 0; `grep -n "runTask" buildsystem-core/src/main/java/de/eintosti/buildsystem/util/inventory/InventoryUtils.java` → 1 match inside `addWorldItem`.

### Step 4 (Bug D): make the no-clip check synchronous

In `NoClipManager.runBlockCheckTask()` change
`runTaskTimerAsynchronously(plugin, this::checkForBlocks, 0L, 4L)` to
`runTaskTimer(plugin, this::checkForBlocks, 0L, 4L)`.
Then simplify `checkForBlocks()`: the inner
`Bukkit.getScheduler().runTask(plugin, () -> { player.setGameMode(...); ... })`
wrapper (lines 83-91) is no longer needed — inline its body. Collections stay as
they are (everything is main-thread now). The per-tick cost is a handful of block
reads per no-clip player — negligible.

**Verify**: compile → exit 0; `grep -n "Asynchronously" buildsystem-core/src/main/java/de/eintosti/buildsystem/player/settings/NoClipManager.java` → no matches.

### Step 5 (Bug E): fix the rename flow's thread shape

In `WorldServiceImpl.renameWorld`:
1. Delete the no-op `Bukkit.getWorlds().remove(oldWorld);` (line 325).
2. Before `Bukkit.unloadWorld(oldWorld, true)` (line 324), capture
   `Location oldSpawnLocation = oldWorld.getSpawnLocation();` and use that captured
   variable in the final sync block instead of `oldWorld.getSpawnLocation()`
   (line 342). Keep using `oldWorld` for the `spawnManager.getSpawnWorld()` equality
   check — that comparison is by reference/name and does not read world state.
3. Move the three statements currently in `thenRunAsync` (`buildWorld.setName`,
   `worldStorage.addBuildWorld`, `worldStorage.save`) into the **beginning** of the
   existing `Bukkit.getScheduler().runTask(plugin, ...)` sync block, and delete the
   now-empty `thenRunAsync` stage. Resulting shape:
   `runAsync(file copy/delete) → thenRun(runTask(sync: rename registry, regenerate world, teleport, spawn, message))`.

**Verify**: compile → exit 0. Read the final method and confirm: no registry/domain
mutation outside the `runTask` block; `getSpawnLocation()` called only before unload.

### Step 6 (Bug F): fail fast when the SFTP connection cannot be established

Change `getSftpClient()` to throw instead of returning null:
```java
private SftpClient getSftpClient() throws IOException {
    if (sftpClient == null || !sftpClient.isOpen()) {
        synchronized (this) {
            if (sftpClient == null || !sftpClient.isOpen()) {
                establishConnection();
            }
        }
    }
    if (sftpClient == null || !sftpClient.isOpen()) {
        throw new IOException("SFTP connection could not be established");
    }
    return sftpClient;
}
```
Remove the `@Nullable` annotation. All four call sites (lines 167, 213, 277, 299)
already sit inside `try { ... } catch (IOException e)` blocks — confirm each still
compiles; the existing catch blocks now handle connection failure with their proper
error paths instead of an NPE.

**Verify**: compile → exit 0; `grep -n "@Nullable" buildsystem-core/src/main/java/de/eintosti/buildsystem/world/backup/storage/SftpBackupStorage.java` → no match on the `getSftpClient` method.

### Step 7 (Bug G): single removal, private map

In `InventoryManager`: delete the `unregisterInventoryHandler(inventory);` call
inside `handleClose` (the map entry was already removed two lines above) and change
the field to `private final Map<Inventory, InventoryHandler> activeInventories;`.
If anything outside `InventoryManager` referenced the field directly, compilation
will fail — route those through the existing methods.

**Verify**: `./gradlew :buildsystem-core:compileJava` → exit 0.

## Test plan

- `MessagesPlaceholderTest` (step 2) — the only unit-testable fix; ≥ 5 cases listed above.
- Bugs A, C, D, E, F, G are server-behavior fixes verified by compile + targeted greps
  (each step lists its grep). Manual server smoke-testing is the operator's call.

## Done criteria

- [ ] `./gradlew :buildsystem-core:compileJava` exits 0
- [ ] `./gradlew :buildsystem-core:test` exits 0, including new placeholder tests
- [ ] All seven step-level greps/checks pass
- [ ] Exactly the in-scope files modified (`git status`)
- [ ] Seven commits, one per bug (or fewer if the operator prefers squashing)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Any excerpt above does not match the live code (drift).
- Step 2: extracting `replacePlaceholders` for testing requires touching more than
  `Messages.java` + the new utility class.
- Step 5: you find additional callers of `renameWorld` relying on the async ordering.
- Step 6: any `getSftpClient()` call site is NOT inside a `try/catch (IOException)` —
  report the site instead of inventing error handling.

## Maintenance notes

- Plans 004 (Messages), 007 (menus), 010 (world flows), 012 (backups) rebuild these
  areas; the fixes here define the *correct* behavior those rebuilds must preserve.
- Reviewer focus: step 5 — the rename flow's failure mode if the async file copy
  throws (the `thenRun` stage is skipped; world stays unloaded under the old name —
  same as today's failure mode, just without the threading hazard).
- `/physics all` colliding with a world literally named "all"
  (`PhysicsCommand.java:68`) was considered and deliberately NOT changed — it's
  defensible behavior, documented in the command's porting notes (plan 005).
