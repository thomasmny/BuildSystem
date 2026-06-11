# Plan 001: Establish a test infrastructure and a verification baseline

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-core/build.gradle.kts gradle/libs.versions.toml buildsystem-core/src/test`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

This repository has **zero tests** (the directories `buildsystem-core/src/test/java` and `buildsystem-core/src/test/resources` exist but are empty). Plans 002–016 perform a full architectural refactor of ~29,500 lines of plugin code; without any executable verification beyond `compileJava`, every refactor step is blind. This plan wires up JUnit 5 + Mockito in Gradle and writes characterization tests for the pure utility classes that later plans will touch. It deliberately does NOT attempt to test Bukkit-server-dependent code (no MockBukkit) — that would be high-risk against spigot-api 26.1.2 and is not needed as a baseline.

## Current state

- `buildsystem-core/build.gradle.kts` — module build file; has NO test dependencies and no test configuration. Dependencies block currently ends with:
  ```kotlin
  implementation(libs.sftp)
  implementation(libs.xseries)
  implementation(libs.zip4j)
  ```
- `gradle/libs.versions.toml` — version catalog; has `[versions]`, `[libraries]`, `[bundles]` sections, no test libraries.
- `buildsystem-core/src/test/java` — exists, empty.
- Pure utility classes to characterize (all in `buildsystem-core/src/main/java/de/eintosti/buildsystem/util/`):
  - `StringCleaner.java` — `public static final String INVALID_NAME_CHARACTERS = "[^A-Za-z\\d/_-]"` (line 28), `hasInvalidNameCharacters(String)` (line 39), `firstInvalidChar(String)` (line 50), `sanitize(String)` (line 63).
  - `NumberUtils.java` — `toInt(String)` (line 50), `toInt(String, int)` (line 71).
  - `ArgumentParser.java` — constructor `ArgumentParser(String[])` (line 37), `isArgument(String)` (line 50), `getFlag(String)` (line 64), `getValue(String)` (line 77).
  - `util/inventory/PaginatedInventory.java` — `calculateNumPages(int, int)` (line 101) is pure math: `totalNumObjects == 0 ? 1 : ceil(total / perPage)`.
- Repo conventions: Java 25 toolchain (set centrally in `buildSrc/src/main/kotlin/CommonConfig.kt`), 4-space indent, license header at the top of every `.java` file (copy the exact 17-line header from any existing file, e.g. `StringCleaner.java` lines 1–17), `@NullMarked` on classes.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile main | `./gradlew :buildsystem-core:compileJava` | exit 0 (deprecation notes are OK) |
| Compile tests | `./gradlew :buildsystem-core:compileTestJava` | exit 0 |
| Run tests | `./gradlew :buildsystem-core:test` | exit 0, all tests pass |

## Scope

**In scope** (the only files you should modify/create):
- `gradle/libs.versions.toml` (modify)
- `buildsystem-core/build.gradle.kts` (modify)
- `buildsystem-core/src/test/java/de/eintosti/buildsystem/util/StringCleanerTest.java` (create)
- `buildsystem-core/src/test/java/de/eintosti/buildsystem/util/NumberUtilsTest.java` (create)
- `buildsystem-core/src/test/java/de/eintosti/buildsystem/util/ArgumentParserTest.java` (create)
- `buildsystem-core/src/test/java/de/eintosti/buildsystem/util/inventory/PaginatedInventoryTest.java` (create)

**Out of scope** (do NOT touch):
- Any file under `buildsystem-core/src/main/java` — this plan adds tests only; if a test reveals a bug, record it in the test as a `// NOTE: documents current (possibly buggy) behavior` comment and report it, do not fix main code.
- `buildsystem-api` — no tests needed there yet (interfaces only).
- Do not add MockBukkit, Paper test fixtures, or any server-mocking library.

## Git workflow

- Branch: work directly on the current branch unless the operator says otherwise (repo convention: feature branches like `dev/26.1`).
- Commit style (from `git log`): conventional commits, e.g. `chore: add JUnit 5 test infrastructure and utility characterization tests`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add test libraries to the version catalog

In `gradle/libs.versions.toml`:
- Under `[versions]` add:
  ```toml
  junit = "5.13.4"
  mockito = "5.20.0"
  ```
  (If dependency resolution fails because these exact versions are unavailable, use the newest available 5.x of each.)
- Under `[libraries]` add:
  ```toml
  junit-bom = { group = "org.junit", name = "junit-bom", version.ref = "junit" }
  junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
  junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
  mockito = { group = "org.mockito", name = "mockito-core", version.ref = "mockito" }
  ```

**Verify**: `./gradlew :buildsystem-core:compileJava` → exit 0 (catalog parses).

### Step 2: Wire test dependencies and the test task into buildsystem-core

In `buildsystem-core/build.gradle.kts`, append to the `dependencies { }` block:

```kotlin
testImplementation(platform(libs.junit.bom))
testImplementation(libs.junit.jupiter)
testImplementation(libs.mockito)
testImplementation(libs.spigot)
testRuntimeOnly(libs.junit.platform.launcher)
```

(`libs.spigot` is needed on the test classpath because main classes reference Bukkit types; it is `compileOnly` for main.)

Add at top level of the file:

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
}
```

**Verify**: `./gradlew :buildsystem-core:test` → exit 0 (no tests yet, task succeeds; if it reports "no tests found" as a failure, add `failOnNoDiscoveredTests = false` is NOT the fix — proceed to step 3 and re-run, Gradle 9 skips empty test sets by default).

### Step 3: Write StringCleanerTest

Create `buildsystem-core/src/test/java/de/eintosti/buildsystem/util/StringCleanerTest.java`. Include the standard 17-line license header. Cover at minimum:
- `hasInvalidNameCharacters`: `"valid_World-1/x"` → false; `"bad name!"` → true; `""` → false.
- `firstInvalidChar`: returns the first offending character for `"abc def"` (a space).
- `sanitize`: strips invalid characters (e.g. `"my world!"` → `"myworld"`), returns input unchanged when already clean. **First read `StringCleaner.java` fully** and assert what the code actually does (e.g. whether it trims, lowercases, or replaces) — these tests characterize current behavior, they must not encode guesses.

### Step 4: Write NumberUtilsTest

Cover: `toInt("42")` → 42; `toInt("abc")` → document actual behavior (read the method first — likely 0); `toInt(null, 7)` → 7; `toInt("13", 7)` → 13; negative numbers; leading/trailing whitespace behavior as implemented.

### Step 5: Write ArgumentParserTest

Read `ArgumentParser.java` fully first. Cover: parsing `new String[]{"-g", "Generator", "-t"}` style input — `isArgument`, `getFlag`, `getValue` for present/absent names; behavior with an empty array; case sensitivity exactly as implemented.

### Step 6: Write PaginatedInventoryTest

`PaginatedInventory` is abstract; instantiate via an anonymous subclass (its only abstract surface comes from `InventoryHandler` — implement the required methods as no-ops). Test `calculateNumPages` only (do NOT call methods that touch `XSound`/`Player`):
- `(0, 9)` → 1; `(9, 9)` → 1; `(10, 9)` → 2; `(27, 9)` → 3; `(1, 9)` → 1.

If class initialization of `PaginatedInventory` fails in tests because the `XSound` static initializer requires a running server, STOP — report it; do not work around with mocking frameworks.

**Verify (steps 3–6)**: `./gradlew :buildsystem-core:test` → exit 0, ≥ 4 test classes, all green.

## Test plan

This plan IS the test plan. Expected result: ≥ 20 individual test methods across 4 test classes, all passing, runnable via one command.

## Done criteria

- [ ] `./gradlew :buildsystem-core:test` exits 0 with at least 4 test classes discovered and passing
- [ ] `./gradlew :buildsystem-core:compileJava` exits 0
- [ ] No files under `buildsystem-core/src/main/java` modified (`git status`)
- [ ] All new test files carry the repo license header
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `libs.spigot` cannot be resolved for `testImplementation` (repository/credential issue).
- `PaginatedInventory` cannot be instantiated in a plain JVM (static init touches the server).
- Any characterization test reveals behavior so surprising it looks like a bug you'd be tempted to fix — record it and report instead.
- Gradle configuration-cache errors appear that require changes outside the two in-scope build files.

## Maintenance notes

- Later plans (003, 004, 006, 008) add tests for the components they refactor; this plan only establishes the harness and the pure-utility baseline.
- Reviewer should check that tests assert *current* behavior (characterization), not idealized behavior.
- MockBukkit/integration testing was deliberately deferred — revisit only if a specific plan needs server-level verification.
