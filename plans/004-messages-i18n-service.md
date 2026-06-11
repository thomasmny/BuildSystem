# Plan 004: Move message defaults into a bundled messages.yml and make Messages an injected service

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/src/main/java/de/eintosti/buildsystem/Messages.java buildsystem-core/src/main/resources`
> Plan 002 changed `Messages.replacePlaceholders` (regex → literal `String.replace`)
> — that change is expected and must be preserved. Any other drift: STOP.

## Status

- **Priority**: P1
- **Effort**: L
- **Risk**: MED (touches 85 files mechanically; message-file generation moves from
  code to a bundled resource — a wrong transcription silently changes server text)
- **Depends on**: 002, 003
- **Category**: tech-debt
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`Messages.java` is 1102 lines, ~900 of which are 483 `setMessage(...)` calls that
write the default `messages.yml` through a `StringBuilder` — defaults-as-code that
drowns the actual logic (load, placeholder substitution, send). It is fully static,
grabs the plugin singleton in a static initializer (`Messages.java:53`), and caches
PlaceholderAPI presence at class-load time (`Messages.java:55` — wrong if PAPI loads
later, never re-checked by `reloadMessages()`). Target (`plans/000-target-architecture.md`
§6.2): defaults live in a `messages.yml` **resource**, `Messages` becomes a small
injected instance service (~150 lines), and the 439 call sites switch receiver.

## Current state

- `buildsystem-core/src/main/java/de/eintosti/buildsystem/Messages.java`:
  - statics: `PLUGIN` (line 53), `MESSAGES` map (54), `PLACEHOLDER_API_ENABLED` (55),
    `config` (58).
  - `createMessageFile()` (60–921): creates/loads the user file, then 483
    `setMessage(config, sb, key, default)` calls interleaved with `addSpacer(sb, "# …")`
    comment lines, and finally writes the assembled text. Read lines 900–960 to see
    the exact write mechanism and the `setMessage`/`addSpacer` helpers (923–942)
    before transcribing anything.
  - `loadMessages()` (944), `reloadMessages()` (963), `checkIfKeyPresent` (968),
    `getPrefix` (977).
  - Public sending/reading API (the surface the 439 call sites use):
    `sendPermissionError(CommandSender)` (981), `sendMessage(CommandSender, String, Entry...)`
    (986), `getString(String, CommandSender, Entry...)` (1002),
    `getStringList(String, Player, Entry...)` (1024),
    `getStringList(String, Player, Function<String, Entry[]>)` (1037),
    `getMessageKey(BuildWorldStatus)` (1054), `getMessageKey(BuildWorldType)` (1071),
    `formatDate(long)` (1098) and (post-plan-002) the literal placeholder helper.
- Call sites: 439 across 85 files —
  `grep -rn "Messages\." buildsystem-core/src/main/java --include='*.java'`.
- `BuildSystemPlugin.onLoad` calls `Messages.createMessageFile()` (line 148).
- There is currently **no** `messages.yml` under
  `buildsystem-core/src/main/resources/` (only `config.yml`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |
| No static receivers left | `grep -rn "Messages\.\(sendMessage\|getString\|getStringList\|sendPermissionError\|reloadMessages\|createMessageFile\|formatDate\)" buildsystem-core/src/main/java --include='*.java'` | no matches |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §6.2 (design), §8 (conventions).

## Scope

**In scope**:
- Create `buildsystem-core/src/main/resources/messages.yml` (the transcribed defaults)
- Create `buildsystem-core/src/main/java/de/eintosti/buildsystem/i18n/Messages.java`
  (instance service; the old top-level `Messages.java` is deleted at the end)
- Modify `BuildSystemPlugin.java` (construct in `onLoad`, getter `getMessages()`)
- Modify the ~85 caller files (receiver swap only)
- New tests: `i18n/MessagesFileTest.java`
- A throwaway generator test used once in step 1 (deleted before finishing)

**Out of scope** (do NOT touch):
- Message *content* — every key and default string must survive byte-identical
  (except YAML quoting differences that parse to the same string).
- `getMessageKey(BuildWorldStatus/BuildWorldType)` semantics — they move as-is
  (they stay `static`: pure enum→key mappings).
- Adventure/Component migration — explicitly rejected (000 §2.5).
- `util/color/` (ColorAPI) — consumed unchanged.

## Git workflow

- Conventional commits, suggested split: (1) resource generation, (2) new service +
  plugin wiring, (3) call-site migration + old class deletion, (4) tests.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Generate `messages.yml` from the existing code (one-off)

The transcription must be generated, not hand-typed. Write a **temporary** JUnit
test (e.g. `MessagesYamlGenerator.java` in the test tree) that:

1. Copies the bodies of `addSpacer` (line 923) and both `setMessage` overloads
   (927, 932) into the test (they are private statics with no plugin dependency —
   verify that when copying).
2. Copies the *call sequence* from `createMessageFile` (every `addSpacer`/`setMessage`
   line between lines ~71 and ~920, unmodified) — but passes a fresh
   `YamlConfiguration` (empty, simulating a first install) instead of the loaded
   user file.
3. Writes the resulting `StringBuilder` content to
   `buildsystem-core/src/main/resources/messages.yml` exactly as `createMessageFile`
   would write it for a fresh install (replicate its final write step — read lines
   900–960 first to mirror it).

Run the test once, keep the produced resource, then **delete the generator test**.

**Verify**:
- `grep -c "^[a-z_]*:" buildsystem-core/src/main/resources/messages.yml` → 483
  (one top-level key per `setMessage` call; adjust the pattern if list values span
  lines — the count of distinct keys must equal the count of `setMessage` calls).
- `grep -n "prefix:" buildsystem-core/src/main/resources/messages.yml` → 1 match
  with default `&8▎ &bBuildSystem &8»`.
- File contains the `# Messages` / `# Scoreboard` / per-command `# /back`-style
  comment sections.

### Step 2: Create the `i18n/Messages` service

`de/eintosti/buildsystem/i18n/Messages.java`:

```java
@NullMarked
public final class Messages {
    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private volatile Map<String, String> messages = Map.of();

    public Messages(BuildSystemPlugin plugin, ConfigService configService) { ... }

    public void load() { ... }       // = old createMessageFile + loadMessages, see below
    public void reload() { load(); }

    public void sendMessage(CommandSender sender, String key, Map.Entry<String, Object>... placeholders) { ... }
    public void sendPermissionError(CommandSender sender) { ... }
    public String getString(String key, CommandSender sender, Map.Entry<String, Object>... placeholders) { ... }
    public List<String> getStringList(...) { ... }                    // both overloads
    public String formatDate(long millis) { ... }

    public static String getMessageKey(BuildWorldStatus status) { ... }   // moved verbatim
    public static String getMessageKey(BuildWorldType type) { ... }
}
```

`load()` behavior (replaces the 900-line generator):
1. If `<dataFolder>/messages.yml` does not exist → `plugin.saveResource("messages.yml", false)`.
2. Load the user file AND the bundled resource
   (`YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8))`).
3. For each key in the resource missing from the user file: append it to the user
   file (set + save once at the end) — this preserves today's behavior where new
   plugin versions add new keys to existing files. User-edited values always win.
4. Build the immutable `messages` map from the merged user file (string lists join
   with `\n` exactly as today's `loadMessages` does — read it at line 944 and
   replicate, including `checkIfKeyPresent`'s missing-key logging).

Other ports: `getPrefix`, the placeholder logic (post-plan-002 literal version),
and the PlaceholderAPI hook — replace the static `PLACEHOLDER_API_ENABLED` constant
with a per-call `Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null`
check (it's a map lookup; correctness over micro-caching).

In `BuildSystemPlugin.onLoad`: replace `Messages.createMessageFile()` with
constructing the service and calling `load()`; add getter `getMessages()`.
Find and update the existing `Messages.reloadMessages()` caller(s):
`grep -rn "reloadMessages" buildsystem-core/src/main/java`.

**Verify**: compile (old + new coexist; old class still referenced) → exit 0.

### Step 3: Migrate the 439 call sites

Receiver-only swap, method names unchanged:

1. Class has a `plugin` field → `Messages.sendMessage(` becomes
   `plugin.getMessages().sendMessage(` (same for `getString`, `getStringList`,
   `sendPermissionError`, `formatDate`).
2. Class has no plugin reference → `BuildSystemPlugin.get().getMessages().…` plus
   `// TODO(plan-014): inject` (same transitional rule as plan 003).
3. `Messages.getMessageKey(...)` → `de.eintosti.buildsystem.i18n.Messages.getMessageKey(...)`
   (static, just retarget the import).
4. Update imports: `de.eintosti.buildsystem.Messages` → `de.eintosti.buildsystem.i18n.Messages`
   only where statics remain; otherwise drop the import.

Then delete `de/eintosti/buildsystem/Messages.java`.

**Verify**: compile → exit 0; the "no static receivers left" grep from the table →
no matches; `ls buildsystem-core/src/main/java/de/eintosti/buildsystem/Messages.java` → gone.

### Step 4: Tests

`buildsystem-core/src/test/java/de/eintosti/buildsystem/i18n/MessagesFileTest.java`:

- Parse `src/main/resources/messages.yml` with `YamlConfiguration.loadFromString`
  (plain file read — no server needed): assert key count (483), assert 5 known
  key/value pairs verbatim (`prefix`, `sender_not_player`, `no_permissions`,
  `back_usage`, `update_available` as a 2-element list).
- Test the merge logic of step 2.3 if it is extractable as a pure function
  (resource map + user map → merged map); if it is interwoven with file IO, test
  via `@TempDir` with real files.

**Verify**: `./gradlew :buildsystem-core:test` → exit 0.

## Test plan

As step 4. Plus: all pre-existing tests stay green (`MessagesPlaceholderTest` from
plan 002 — if it referenced the old class, retarget it to wherever the placeholder
helper now lives).

## Done criteria

- [ ] `./gradlew :buildsystem-core:compileJava` exits 0
- [ ] `./gradlew :buildsystem-core:test` exits 0 (incl. new `MessagesFileTest`)
- [ ] `buildsystem-core/src/main/resources/messages.yml` exists with 483 keys
- [ ] Old `de/eintosti/buildsystem/Messages.java` deleted; new class < 300 lines
- [ ] "No static receivers" grep → no matches
- [ ] Throwaway generator test deleted
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `setMessage`/`addSpacer` (lines 923–942) turn out to reference plugin state —
  the step-1 copy approach then doesn't work as described.
- The user-file merge behavior in today's `createMessageFile` is something other
  than "user values win, missing keys get defaults appended" — report what the code
  actually does before building `load()`.
- The generated resource's key count ≠ 483 after accounting for list-valued keys.
- You find call sites passing dynamic/computed keys that a receiver swap would break.

## Maintenance notes

- Adding a message now means: add the key to `src/main/resources/messages.yml`
  (with its section comment) and use it — no Java change. State this in the PR
  description; it's the developer-facing win.
- Reviewer focus: the step-1 transcription (diff a server-generated messages.yml
  from the old jar against the new resource if possible) and the merge logic.
- The `i18n/Placeholders` extraction from plan 002 (if it happened) should end up in
  `i18n/` next to this class.
