# Plan 005: Build the command framework and migrate all 17 commands onto it

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/command buildsystem-core/src/main/java/de/eintosti/buildsystem/BuildSystemPlugin.java`
> Expected drift from plans 002–004: `BuildSystemPlugin` gained service wiring;
> commands' `Messages.`/`Config.` receivers changed. The command *structure*
> (one class per command, self-registration, separate `tabcomplete/` classes)
> must still match the excerpts below; otherwise STOP.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED (every command is touched; permission and console behavior must
  not change)
- **Depends on**: 003, 004
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

All 17 command classes repeat the same boilerplate: self-registration in the
constructor, a `!(sender instanceof Player player)` guard (16 of 17), permission
checks, and "resolve world from args or current world" logic (4+ commands,
char-identical). Nine separate `tabcomplete/*` classes re-implement the commands'
permission logic, which can silently drift (e.g. `GamemodeTabCompleter` duplicates
`GamemodeCommand`'s permission mapping). Target (`plans/000-target-architecture.md`
§6.3): a `CommandBase` with shared guards, completion colocated with execution, and
one `CommandRegistrar` owning all Bukkit registration.

## Current state

- `buildsystem-core/src/main/java/de/eintosti/buildsystem/command/` — 17 classes:
  `BackCommand, BlocksCommand, BuildCommand, BuildSystemCommand, ConfigCommand,
  ExplosionsCommand, GamemodeCommand, NoAICommand, PhysicsCommand, SettingsCommand,
  SetupCommand, SkullCommand, SpawnCommand, SpeedCommand, TimeCommand, TopCommand,
  WorldsCommand` + `PagedCommand` (shared help-pagination base used by
  `BuildSystemCommand` and the worlds `HelpSubCommand`).
- Representative boilerplate, `PhysicsCommand.java:42-60` (verified):
  ```java
  public PhysicsCommand(BuildSystemPlugin plugin) {
      this.plugin = plugin;
      this.worldStorage = plugin.getWorldService().getWorldStorage();
      plugin.getCommand("physics").setExecutor(this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player player)) {
          plugin.getLogger().warning(Messages.getString("sender_not_player", sender));
          return true;
      }

      String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
      BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
      if (!WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, "buildsystem.physics")) { ... }
  ```
  The same player-guard appears in 16 commands; the same `worldName` line appears in
  `PhysicsCommand:55`, `NoAICommand:55`, `ExplosionsCommand:55`, `TimeCommand:54`.
- `ConfigCommand.java:38-43` (verified) is the **only console-capable command** —
  it checks `sender.hasPermission("buildsystem.config")` without a player guard.
- `buildsystem-core/.../command/tabcomplete/` — 9 classes: `BuildTabCompleter,
  ConfigTabCompleter, EmptyTabCompleter, GamemodeTabCompleter, PhysicsTabCompleter,
  SpawnTabCompleter, SpeedTabCompleter, TimeTabCompleter, WorldsTabCompleter`. Each
  self-registers; `EmptyTabCompleter` registers itself for 6 commands (back, blocks,
  buildsystem, settings, setup, top).
- `BuildSystemPlugin.registerCommands()` (lines 240–258) and
  `registerTabCompleters()` (260–270) instantiate everything.
- Command names/permissions ground truth: `buildsystem-core/build.gradle.kts`
  `bukkit { commands { ... } permissions { ... } }` block.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |
| Old layer gone | `ls buildsystem-core/src/main/java/de/eintosti/buildsystem/command/tabcomplete 2>/dev/null` | "No such file or directory" |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §5, §6.3, §8.

## Scope

**In scope**:
- Create `command/CommandBase.java`, `command/CommandRegistrar.java`
- Migrate the 16 simple commands (all except `WorldsCommand`'s subcommand dispatch —
  `WorldsCommand` itself only changes registration + gets its completion from
  plan 006; leave its `onCommand` switch body alone)
- Dissolve all 9 `tabcomplete/*` classes into the owning commands' `complete()`;
  delete the package
- `BuildSystemPlugin.java`: replace `registerCommands()`/`registerTabCompleters()`
  with one `CommandRegistrar` call
- `PagedCommand` may be adapted (constructor injection) but keeps its rendering logic

**Out of scope** (do NOT touch):
- `command/subcommand/**` (plan 006)
- `WorldsTabCompleter`'s *worlds-specific* completion logic — move it temporarily
  into `WorldsCommand.complete()` as-is; plan 006 dissolves it properly
- Any permission string, message key, or behavioral change (the `/physics all`
  quirk at `PhysicsCommand:68` ports as-is)
- plugin.yml command declarations (`build.gradle.kts`)

## Git workflow

- Conventional commits: framework first, then one commit per migrated command
  (or sensible batches), then the deletion commit.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: `CommandBase`

```java
@NullMarked
public abstract class CommandBase implements CommandExecutor, TabCompleter {

    protected final BuildSystemPlugin plugin;
    protected final Messages messages;
    private final boolean playerOnly;

    protected CommandBase(BuildSystemPlugin plugin, boolean playerOnly) { ... }

    @Override
    public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (playerOnly) {
            if (!(sender instanceof Player player)) {
                plugin.getLogger().warning(messages.getString("sender_not_player", sender));
                return true;
            }
            run(player, args);
        } else {
            run(sender, args);
        }
        return true;
    }

    protected void run(Player player, String[] args) {}        // player-only commands override
    protected void run(CommandSender sender, String[] args) {} // console-capable commands override

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return sender instanceof Player player ? complete(player, args) : List.of();
    }

    protected List<String> complete(Player player, String[] args) { return List.of(); }

    // shared guards — exact behavior of the duplicated blocks:
    protected boolean requirePermission(CommandSender sender, String permission) { ... } // sends sendPermissionError on fail
    protected String worldNameFromArgs(Player player, String[] args, int index) {
        return args.length <= index ? player.getWorld().getName() : args[index];
    }
}
```

Design constraints:
- No Bukkit registration inside `CommandBase` or any command constructor.
- `messages` comes from the constructor (kills the `plugin.getMessages()` chains
  plan 004 introduced inside commands).
- Keep the exact console warning behavior (`logger.warning(messages.getString("sender_not_player", sender))`).

### Step 2: `CommandRegistrar`

```java
public final class CommandRegistrar {
    // ctor: (BuildSystemPlugin plugin, Messages messages, ...services the commands need)
    public void registerAll() {
        register("back", new BackCommand(...));
        ...all 17...
    }
    private void register(String name, CommandBase command) {
        PluginCommand cmd = Objects.requireNonNull(plugin.getCommand(name), name);
        cmd.setExecutor(command);
        cmd.setTabCompleter(command);
    }
}
```

`requireNonNull` with the command name: a missing plugin.yml entry should fail loud
at startup, not NPE later. In `BuildSystemPlugin.onEnable`, replace the
`registerCommands()` + `registerTabCompleters()` calls and delete both methods.

**Verify** (steps 1–2): compile → exit 0 (registrar may register only already-migrated
commands during the transition; finish the migration before the final gate).

### Step 3: Migrate the 16 simple commands

Per command: extend `CommandBase`, move `onCommand` body into `run(Player, …)`
(or `run(CommandSender, …)` for `ConfigCommand` with `playerOnly = false`), delete
the self-registration constructor line and the player guard (the base does it),
replace duplicated world-resolution lines with `worldNameFromArgs(player, args, 0)`.
**Behavior must be diff-identical**: same messages, same permission strings, same
order of checks. `TimeCommand` handles three registered commands (`day`, `night`,
`gamemode`-style label switching — check its constructor: it registers `day` and
`night`); commands that serve multiple labels register once per label in the
registrar with the same instance, and `run` keeps using the label — note:
`CommandBase.onCommand` must pass `label` through to `run` if any migrated command
reads it (check `TimeCommand` and `SpeedCommand` first; add the parameter to `run`
if needed).

### Step 4: Fold tab completion into the commands

For each of the 9 completer classes: move its `onTabComplete` logic into the owning
command's `complete(Player, args)`, replacing duplicated permission checks with the
same constants the command's `run` uses (define a `private static final String
PERMISSION = "buildsystem.x"` per command where both paths need it).
`EmptyTabCompleter` disappears (its 6 commands simply inherit `complete` → empty list).
`WorldsTabCompleter`: move the whole body into `WorldsCommand.complete()` unchanged
(plan 006 splits it). Then delete the `tabcomplete/` package.

**Verify**: compile → exit 0; `ls .../command/tabcomplete` → gone;
`grep -rn "setExecutor\|setTabCompleter" buildsystem-core/src/main/java --include='*.java' | grep -v CommandRegistrar` → no matches.

### Step 5: Tests

`command/CommandBaseTest.java` with Mockito:
- console sender + playerOnly command → `run(Player…)` not invoked, warning logged.
- console sender + `playerOnly=false` → `run(CommandSender…)` invoked.
- `worldNameFromArgs`: `args=[]` → current world name; `args=["x"]`, index 0 → `"x"`.
- `requirePermission` true/false paths (mock `Player#hasPermission`).
Mock `BuildSystemPlugin`/`Messages` (Mockito can mock final classes if configured;
if mocking `Messages` fails, introduce a thin interface or relax `final`).

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 5 (≥ 6 cases). Manual smoke list for the operator: `/back`, `/config reload`
from console, `/physics` with 0/1/2 args, tab-complete `/gamemode ` with and
without permission.

## Done criteria

- [ ] Compile + tests exit 0
- [ ] `command/tabcomplete/` deleted
- [ ] All Bukkit command registration flows through `CommandRegistrar`
      (grep in step 4 verifies)
- [ ] All 17 command classes extend `CommandBase`; no constructor self-registration
- [ ] No `instanceof Player` guard remains in any command subclass:
      `grep -rn "instanceof Player" buildsystem-core/src/main/java/de/eintosti/buildsystem/command --include='*.java'` → matches only in `CommandBase.java`
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Any command's `onCommand` does something before the player guard (ordering would
  change) — report the command.
- A completer contains logic with **no** owning command equivalent (orphaned
  feature) — report rather than guessing where it belongs.
- `TimeCommand`/`SpeedCommand` label handling cannot be expressed with the
  base-class shape without changing user-visible behavior.
- Mockito cannot construct the step-5 mocks after one reasonable attempt
  (report; do not add new mocking libraries).

## Maintenance notes

- Adding a command now = one class extending `CommandBase` + one registrar line +
  plugin.yml entry. Completion lives in the same file as execution — reviewers
  should reject any future PR re-separating them.
- Plan 006 builds `SubCommandDispatcher` on top of this for `/worlds`.
- Plan 014 revisits constructor parameters when the singleton dies.
