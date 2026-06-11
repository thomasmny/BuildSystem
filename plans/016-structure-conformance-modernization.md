# Plan 016: Final structure conformance, Java 25 sweep, and developer docs

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: confirm plans 002–015 are marked DONE in
> `plans/README.md`. This plan is the closing pass; running it early produces
> conflicts with everything else. If any predecessor is not DONE, STOP.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW (moves + mechanical modernization + docs; behavior frozen)
- **Depends on**: ALL of 002–015
- **Category**: tech-debt / dx / docs
- **Planned at**: commit `67beca7`, 2026-06-11

## Why this matters

The strangler rebuild leaves stragglers: classes in legacy locations, files the
subsystem plans never touched (and therefore never modernized), and no
developer-facing document explaining the new architecture. This plan makes the
package tree match `plans/000-target-architecture.md` §4 exactly, finishes the
Java 25 modernization in untouched files, and writes the two documents that keep
the structure enforceable: `ARCHITECTURE.md` and `CLAUDE.md`.

## Current state

Cannot be known precisely at planning time (it depends on 002–015 execution).
Authoritative inputs:

- `plans/000-target-architecture.md` §4 — the target tree.
- Expected residual moves at `67beca7` coordinates (verify each still exists):
  - `expansion/` (luckperms, placeholderapi) → `integration/`
    (plan 009 created `integration/worldedit/`; the rest moves here)
  - `world/modification/*Menu`-style classes → `world/menu/`,
    `player/settings/*Menu` → `player/menu/` (if plan 008 left them in place)
  - `world/navigator/inventory/*` → `world/menu/` or `navigator/` per 000 §4
  - `world/util/` lifecycle classes → `world/lifecycle/` (loader/unloader/teleporter)
  - `command/subcommand/worlds/` stays (it matches the tree)
- Modernization targets — sweep whatever 002–015 didn't touch:
  - `util/color/**` (ColorAPI + patterns), `util/UUIDFetcher`, `util/DirectionUtil`,
    `expansion/**` (pre-move), remaining listeners, `player/` data classes.
- Docs: no `CLAUDE.md`, no `ARCHITECTURE.md` in the repo (verified at `67beca7`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./gradlew :buildsystem-core:compileJava :buildsystem-api:compileJava` | exit 0 |
| Tests | `./gradlew :buildsystem-core:test` | exit 0 |
| Full build | `./gradlew build` | exit 0 (shadowJar produced) |

## Suggested executor toolkit

- `plans/000-target-architecture.md` §4 (tree — the conformance spec), §8
  (conventions, comment policy).

## Scope

**In scope**: package moves (`git mv` only), import fixes, the modernization sweep
(rules below), `ARCHITECTURE.md`, `CLAUDE.md`, a final dead-code check.

**Out of scope** (do NOT touch):
- Behavior. This plan must produce a diff reviewable as "moves + mechanical
  rewrites + docs". Anything that needs a judgment call about behavior goes into
  the completion report as a follow-up, not into the diff.
- `buildsystem-api` packages (published coordinates are frozen forever).
- plugin.yml `main` class coordinate: `de.eintosti.buildsystem.BuildSystemPlugin`
  does not move.

## Git workflow

- Conventional commits: one commit per package move-group; one per modernization
  file-batch; one for docs.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Package conformance

Diff the real tree against 000 §4:
`find buildsystem-core/src/main/java -type d | sort` vs the spec. Produce a move
list, execute with `git mv`, fix imports/package statements
(compile after each group). Where the spec and reality diverge because an earlier
plan made a *better* call, prefer reality and record the delta in
`ARCHITECTURE.md` (the architecture doc describes what IS).

**Verify**: compile → exit 0; `find … -type d` matches the documented tree.

### Step 2: Modernization sweep

For every `.java` file in core not modified by plans 002–015
(`git log --since="<plan-002 start date>" --name-only` to compute the untouched
set), apply mechanically:

- `switch` statements on enums/strings with per-case `break` → switch expressions
  (arrow form) where the result is a value or every branch is a single statement.
- `instanceof X` + cast → pattern `instanceof X x`.
- Immutable data carriers (private final fields + ctor + getters + no identity
  semantics) → `record` — ONLY for internal classes; check each class for
  subclassing/mutation first.
- `new ArrayList<>()` + add-loop building constants → `List.of(...)`;
  string concatenation chains → `formatted(...)` where already half-converted
  style exists.
- Delete comments that restate code (per 000 §8 / the maintainer's comment
  policy); keep license headers and decision-explaining comments.
- NO renaming of fields/methods during the sweep (keeps the diff reviewable).

**Verify**: compile + tests after every ~10-file batch.

### Step 3: Dead-code check

`grep`-level pass: unused private methods (IDE-style detection is unavailable —
use `grep -rn "methodName(" ` per suspect), classes with zero inbound references
(`grep -rln "ClassName" --include='*.java' | wc -l` == 1 → its own file). Delete
what is provably dead; list anything uncertain in the report instead of deleting.

### Step 4: `ARCHITECTURE.md` (repo root)

~150 lines, written from 000 but describing the **as-built** system: module roles,
the package tree with one line per package, the composition-root construction
order, the threading rules (000 §7 verbatim), the menu/command/protection
extension recipes ("to add a command: …", "to add a menu: …", "protection rules
live ONLY in WorldProtectionPolicy"), and the API compatibility policy.

### Step 5: `CLAUDE.md` (repo root)

~40 lines for future agent sessions:
- Build/test/verify commands (the table from this plan).
- Pointer to `ARCHITECTURE.md` + the three iron rules: API frozen
  (deprecate-only), Bukkit-main-thread rules, no static mutable state.
- Comment policy (no restating-the-code comments; API Javadoc mandatory).
- Conventional-commit style; `plans/` directory explanation.

**Verify**: `./gradlew build` → exit 0; both docs exist and name only
classes/packages that exist (`grep`-check every code reference in the docs).

## Test plan

Existing suite green throughout; no new tests (mechanical pass). Operator smoke:
full build, start server, click through one menu, one command, one world create.

## Done criteria

- [ ] `./gradlew build` exits 0
- [ ] Package tree matches `ARCHITECTURE.md` (which matches reality)
- [ ] `ARCHITECTURE.md` + `CLAUDE.md` exist, references verified
- [ ] No file > 400 lines in core except generated/data-heavy ones — list any
      exceptions in the report with one-line justifications:
      `find buildsystem-core/src/main/java -name '*.java' | xargs wc -l | sort -rn | head`
- [ ] `plans/README.md`: this row DONE + a closing note listing all deferred
      follow-ups collected from plans 002–015 completion reports

## STOP conditions

Stop and report back (do not improvise) if:

- A "mechanical" modernization changes observable behavior (e.g. a record's
  `equals` now differs and something relied on identity) — revert that file and
  report.
- The untouched-file set exceeds ~60 files (sweep is bigger than estimated —
  report and split).
- Step 3 dead-code candidates include anything referenced by reflection/Bukkit
  (event handlers, serialization) — never delete those; report.

## Maintenance notes

- `ARCHITECTURE.md` is now the contract; PRs that violate the tree or the iron
  rules should be rejected in review with a pointer to it.
- The `plans/` directory can be archived (e.g. `plans/done/`) once the maintainer
  signs off the series; keep `000` next to `ARCHITECTURE.md` history for the "why".
