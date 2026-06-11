# Plan 015: Verify API compatibility, polish Javadoc, fix the internal event contract

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 67beca7..HEAD -- buildsystem-api`
> This diff is itself the audit subject of step 1 — drift here is expected; the
> step verifies it is *compatible* drift.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: 002–014 (run after the core rebuild settles)
- **Category**: docs / migration
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

`buildsystem-api` is published to Maven Central; third-party plugins compile
against it. The maintainer's rule: **never delete or change public API — only add,
or deprecate-for-removal with a pointer to the replacement.** Plans 002–014 were
written to keep the API untouched; this plan *proves* it, closes the one known
contract gap (the internal `BuildWorldManipulationEvent` lacks its own
`HandlerList`), and codifies the deprecation protocol so future changes follow it.

## Current state

(Verified at `67beca7`.)

- API Javadoc quality is already good (e.g. `api/world/WorldService.java` documents
  every method with `@since 3.0.0` class tags) — the work is *verifying
  completeness*, not mass-writing docs.
- `grep -rn "@Deprecated" buildsystem-api/src/main/java` → no matches (nothing
  deprecated yet; the protocol below is for future use and for anything plans
  002–014 flagged).
- `buildsystem-api/build.gradle.kts` has a configured `Javadoc` task (title,
  links, overview) — `./gradlew :buildsystem-api:javadoc` is the verification
  vehicle.
- Internal events in core: `event/world/BuildWorldManipulationEvent.java` extends
  the API's `BuildWorldEvent` but (reported by audit, **verify by reading**) does
  not declare its own `static HandlerList`/`getHandlerList()`, wrapping a parent
  event for cancellation instead. Bukkit's event contract expects each concrete
  event class to own a `HandlerList`; piggybacking makes
  `BuildWorldManipulationEvent.getHandlerList()` resolve to the parent's, so
  unregistering listeners for one unregisters both.
- `api/world/builder/Builder.java` — `sealed interface Builder permits BuilderImpl`
  (line 33) with static factories `of(UUID, String)` (43), `of(Player)` (54),
  `deserialize` (~77). `BuilderImpl` is **package-private** in the API module
  (`final class BuilderImpl implements Builder`, line 29) — an immutable
  name+uuid pair; a clean record candidate (compatible: not public API surface).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| API javadoc (strict) | `./gradlew :buildsystem-api:javadoc` | exit 0, **zero warnings** |
| API compile | `./gradlew :buildsystem-api:compileJava` | exit 0 |
| Core compile | `./gradlew :buildsystem-core:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |

## Scope

**In scope**:
- Step-1 compatibility audit of `git diff 67beca7..HEAD -- buildsystem-api`
- Javadoc completeness pass on `buildsystem-api` (warnings → fixes)
- `BuilderImpl` → `record` (package-private; only if `equals`/`hashCode`/accessor
  names stay behaviorally identical — read it first; it has `getUuid()`-style
  accessors on an interface contract, a record must implement those interface
  methods explicitly)
- Core: `event/world/BuildWorldManipulationEvent.java` — add its own
  `private static final HandlerList HANDLERS`, `getHandlers()` override, and
  `public static HandlerList getHandlerList()`
- Add `CONTRIBUTING`-level deprecation protocol text to `plans/000-target-architecture.md`
  §6.10 has the rule; copy it into `buildsystem-api/src/main/java/de/eintosti/buildsystem/api/package-info.java`
  as package documentation if not already stated

**Out of scope** (do NOT touch):
- Any public API signature (adding `@Deprecated(forRemoval = true, since = "…")` +
  `@deprecated` Javadoc to genuinely superseded members is allowed ONLY if an
  earlier plan's completion report named the member and its replacement — otherwise
  deprecate nothing)
- Core internals beyond the named event class

## Git workflow

- Conventional commits: `docs: ...` for javadoc, `fix: give BuildWorldManipulationEvent its own HandlerList`, `refactor: make BuilderImpl a record`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Compatibility audit

Run `git diff 67beca7..HEAD -- buildsystem-api`. For every changed hunk, classify:
- **Comment/Javadoc-only** → fine.
- **Addition** (new method/type) → fine; must have Javadoc + `@since` next release
  version (read `gradle.properties` `version=` and use it without `-SNAPSHOT`).
- **Signature change or removal** → VIOLATION: STOP and report (an earlier plan
  broke the rule; it must be reverted/wrapped, which needs the operator).

Write the classification table into your completion report.

### Step 2: Strict Javadoc

Temporarily run with doclint to surface gaps:
`./gradlew :buildsystem-api:javadoc -PjavadocStrict` is not wired — instead add to
`buildsystem-api/build.gradle.kts` inside the existing `tasks.withType<Javadoc>`:
`opt.addBooleanOption("Xdoclint:all,-missing", true)` first; fix what it reports;
then switch to `Xdoclint:all` and fix `missing` warnings (every public
method/param/return in the API gets a doc — *useful* text, per the maintainer:
document behavior and contracts, not restatements of the name). Keep the doclint
option in the build so regressions fail the build.

**Verify**: `./gradlew :buildsystem-api:javadoc` → exit 0, zero warnings.

### Step 3: `BuilderImpl` record + event fix

- Read `BuilderImpl`; convert to `record` only if its interface methods map
  cleanly; serialization format (`deserialize` at `Builder.java:77` splits on a
  separator) must round-trip identically — keep `toString`/serialize methods
  explicit if the record's defaults would differ.
- `BuildWorldManipulationEvent`: add the standard Bukkit boilerplate (own
  `HandlerList`). Confirm `WorldManipulateListener` and `EventDispatcher`
  (its producer, `event/EventDispatcher.java:71`) still compile/behave.

**Verify**: compile + tests → exit 0.

## Test plan

- `BuilderSerializationTest` in core tests (API classes are on core's test
  classpath): `Builder.of(uuid, "name")` → serialize → `deserialize` → equals;
  plus `equals`/`hashCode` consistency before/after the record change (write the
  test BEFORE converting, against current behavior).

## Done criteria

- [ ] Step-1 classification table written; zero violations (or STOP'd)
- [ ] `:buildsystem-api:javadoc` exits 0 with doclint all enabled
- [ ] `BuildWorldManipulationEvent` owns its `HandlerList`
- [ ] `BuilderSerializationTest` green
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Step 1 finds any signature change or removal in the API diff.
- `BuilderImpl`'s equality/serialization cannot be preserved exactly as a record.
- Doclint reveals > ~50 missing-doc warnings (volume says the effort estimate is
  wrong; report the count first).

## Maintenance notes

- The deprecation protocol (API §6.10 of plan 000): mark
  `@Deprecated(forRemoval = true, since = "<version>")` + `@deprecated` Javadoc
  naming the replacement; remove nothing until a major version the maintainer
  calls explicitly.
- Doclint in the build is the regression guard for API docs — do not remove it to
  silence a warning.
