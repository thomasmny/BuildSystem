# Plan 006: Registry-based /worlds subcommand dispatch with colocated completion

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/command`
> Plan 005 must have landed (WorldsCommand extends CommandBase; the body of the old
> `WorldsTabCompleter.onTabComplete` now lives in `WorldsCommand.complete()`). If
> that is not the case, STOP — this plan builds directly on it.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED (24 subcommands; permission-gated completion must not drift)
- **Depends on**: 005
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`/worlds` dispatches through a 24-case switch that instantiates a new subcommand
object per invocation (`WorldsCommand.java:95-121` at `67beca7`), and its tab
completion is a separate 301-line class whose nested switch re-implements each
subcommand's permission logic. Adding a subcommand today means editing three places
that can drift. Target (`plans/000-target-architecture.md` §6.3): subcommands are
registered once in a map, each owning its `execute` AND `complete`, and the
dispatcher is generic.

## Current state

- `command/WorldsCommand.java` — after plan 005: extends `CommandBase`; `run()`
  still contains (verbatim from `67beca7` lines 85–123):
  - `WorldsTabCompleter.WorldsArgument.matchArgument(args[0])` lookup,
  - the `String worldName = args.length >= 2 ? args[1] : player.getWorld().getName();`
    convention (line 93, with an explanatory comment),
  - the 24-case `switch (argument)` creating one `SubCommand` per case, e.g.
    `case DELETE -> new DeleteSubCommand(plugin, worldName);`, then
    `subCommand.execute(player, args);`.
- `command/subcommand/SubCommand.java` (verified, 34 lines):
  ```java
  public interface SubCommand {
      void execute(Player player, String[] args);
      Argument getArgument();
      default boolean hasPermission(Player player) {
          String permission = getArgument().getPermission();
          return permission == null || player.hasPermission(permission);
      }
  }
  ```
- `command/subcommand/Argument.java` (verified): interface with `getName()` and
  `@Nullable getPermission()`.
- `command/subcommand/worlds/` — 25 classes (24 subcommands + their shared imports):
  `AddBuilderSubCommand, ArchiveSubCommand, BackupsSubCommand, BuildersSubCommand,
  DeleteSubCommand, EditSubCommand, FolderSubCommand, HelpSubCommand,
  ImportAllSubCommand, ImportSubCommand, InfoSubCommand, ItemSubCommand,
  PrivateSubCommand, PublicSubCommand, RemoveBuilderSubCommand,
  RemoveSpawnSubCommand, RenameSubCommand, SetCreatorSubCommand, SetItemSubCommand,
  SetPermissionSubCommand, SetProjectSubCommand, SetSpawnSubCommand,
  SetStatusSubCommand, TeleportSubCommand, UnimportSubCommand`.
  Most take `(plugin, worldName)` in the constructor — **state that belongs to the
  invocation, not the object**.
- The completion logic (moved into `WorldsCommand.complete()` by plan 005;
  originally `tabcomplete/WorldsTabCompleter.java`): `case 1` lists subcommand
  names filtered by `argument.getPermission()`; `case 2` has a giant inner switch —
  11 subcommands share a "list worlds the player may act on" block (verified at
  original lines 82–113), `backup` checks `BACKUP.getPermission() + ".create"`,
  `builders/removebuilder` list builders, `folder` has its own branch, `import`
  lists importable directories, etc.; `default` handles arg ≥ 3 for `import`
  (generator flags) and `folder`.
- The `WorldsArgument` enum (today nested in `WorldsTabCompleter`) implements
  `Argument` with name + permission per subcommand.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.3, §8.

## Scope

**In scope**:
- `command/SubCommandDispatcher.java` (create — generic, reusable)
- `command/subcommand/SubCommand.java` (extend the contract)
- `command/subcommand/worlds/**` (all 24 subcommands: constructor shape + their
  completion moved in)
- `command/WorldsCommand.java` (shrinks to dispatcher wiring)
- Move `WorldsArgument` to `command/subcommand/worlds/WorldsArgument.java` if plan
  005 left it elsewhere

**Out of scope** (do NOT touch):
- The *behavior* of any subcommand's `execute` (only its constructor/parameter shape
  changes: invocation state moves from constructor to method parameters)
- Inventory classes the subcommands open
- `CommandBase`/`CommandRegistrar` (plan 005's contract is fixed)

## Git workflow

- Conventional commits: contract first, then subcommand batches (e.g. 6 per commit),
  then `WorldsCommand` shrink.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Extend the `SubCommand` contract

```java
public interface SubCommand {
    Argument getArgument();
    void execute(Player player, String worldName, String[] args);
    default List<String> complete(Player player, String[] args) { return List.of(); }
    default boolean hasPermission(Player player) { ... unchanged ... }
}
```

`worldName` becomes a parameter (today it is constructor state). Subcommands that
never used `worldName` ignore it. `args` stays the full original array (indices
unchanged — subcommand bodies must not be re-indexed).

### Step 2: Convert the 24 subcommands

For each class in `command/subcommand/worlds/`:
- Constructor keeps only real dependencies (`plugin`, services, messages) — remove
  `worldName` (and any other per-invocation values) from fields; thread them through
  `execute` parameters instead.
- `AddBuilderSubCommand`/`RemoveBuilderSubCommand` receive
  `player.getWorld().getName()` as `worldName` today (WorldsCommand line 97/110
  passes the *current* world, not `args[1]`) — preserve that: the dispatcher passes
  the raw resolved name; these two subcommands must compute their own
  `player.getWorld().getName()` inside `execute` to keep behavior identical.
  Check each `case` arm at `67beca7` lines 95–121 for which name each subcommand
  actually received before assuming.
- `HelpSubCommand` keeps its `PagedCommand` relationship as-is.

### Step 3: `SubCommandDispatcher` + registry

```java
public final class SubCommandDispatcher {
    private final Map<String, SubCommand> byName;   // LinkedHashMap, lowercase keys
    // register all 24 once (constructor of WorldsCommand builds the map)
    public boolean dispatch(Player player, String[] args) { ... }   // match args[0], permission default-impl is NOT bypassed: preserve today's behavior — execute() does its own checks exactly as before
    public List<String> complete(Player player, String[] args) {
        // args.length == 1: names whose getArgument().getPermission() the player has
        // args.length >= 2: byName.get(args[0].toLowerCase())?.complete(player, args)
    }
}
```

Dispatch must replicate today's flow exactly: unknown `args[0]` →
`worlds_unknown_command` message; `worldName` resolution rule from line 93;
**no new permission gate in the dispatcher** (subcommands enforce their own — adding
a central check would change behavior for subcommands whose `execute` deliberately
checks late or differently).

### Step 4: Distribute the completion switch

Move each branch of the (post-005) `WorldsCommand.complete()` inner switch into the
matching subcommand's `complete()`:
- The 11-subcommand shared "list permitted worlds" block becomes a static helper
  `WorldsCompletions.permittedWorldNames(Player, WorldStorage, String commandPermissionSuffix)`
  in `command/subcommand/worlds/` — called by those 11 `complete()` impls (do not
  copy it 11 times).
- `backup`'s `.create` sub-permission check, `builders`' builder listing, `import`'s
  directory + flag completion (the `default:` branch handling args ≥ 3), and
  `folder`'s branch each move into their subcommand verbatim.
- `WorldsCommand.complete()` becomes a single `dispatcher.complete(player, args)` call;
  `WorldsCommand.run()` becomes guard + `dispatcher.dispatch(player, args)`.

**Verify**: compile → exit 0;
`grep -c "case " buildsystem-core/src/main/java/de/eintosti/buildsystem/command/WorldsCommand.java` → 0;
`wc -l buildsystem-core/src/main/java/de/eintosti/buildsystem/command/WorldsCommand.java` → < 80 lines.

### Step 5: Tests

`command/subcommand/SubCommandDispatcherTest.java` (Mockito):
- unknown subcommand name → no dispatch (assert via a recording fake `SubCommand`).
- known name, lowercase/uppercase input → dispatched with the resolved world name
  per the line-93 rule (`args.length >= 2 ? args[1] : currentWorld`).
- `complete` with `args.length == 1` filters by permission (two fake subcommands,
  one permitted).
- `complete` with `args.length == 2` delegates to the named subcommand.

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 5 (≥ 4 dispatcher tests). Operator smoke list: `/worlds`, `/worlds tp <tab>`,
`/worlds import <tab>` (directory listing), `/worlds addbuilder` from inside a world,
`/worlds backup` with/without `.create` permission.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `WorldsCommand` < 80 lines, no `switch` over subcommands, no completion logic
- [ ] Every subcommand's completion lives in its own `complete()`
      (`grep -n "case \"" .../command/WorldsCommand.java` → no matches)
- [ ] No subcommand stores `worldName` as a field:
      `grep -rn "private final String worldName" buildsystem-core/src/main/java/de/eintosti/buildsystem/command/subcommand` → no matches
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Any `case` arm at `67beca7` `WorldsCommand.java:95-121` passes constructor
  arguments beyond `(plugin, worldName)` that don't map onto the new contract.
- A subcommand's `execute` reads `args` at indices that assumed the old constructor
  state — report instead of re-indexing.
- The completion switch contains a branch with no matching subcommand class.

## Maintenance notes

- Adding a `/worlds` subcommand now = one class + one registry line; completion and
  permission live in the same file. Reviewers should enforce this.
- The dispatcher is generic on purpose — if another command grows subcommands
  (e.g. `/folder`), reuse it rather than forking.
- Plan 015 documents `SubCommand` as internal (not API).
