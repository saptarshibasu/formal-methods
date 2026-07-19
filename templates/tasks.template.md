<!--
  TEMPLATE — generated FROM plan.md + spec.md, not written from scratch.
  One per feature, at specs/<NNN-feature-name>/tasks.md. Tasks are grouped
  by user story so each story is independently implementable, testable,
  and shippable as its own increment.

  Format: [ID] [P?] [Story] Description, with exact file paths.
  [P] = can run in parallel (different files, no dependency on other [P] tasks
  in the same group). [Story] = which user story (US1, US2...) this task
  belongs to, for traceability.

  Once the task list below is filled in, delete this instructional comment —
  it guided drafting, it is not part of the task list itself.
-->

# Tasks: [FEATURE NAME]

**Input**: `spec.md` (required), `plan.md` (required), `data-model.md` /
`contracts/` if present. | **Status**: Draft

## Phase 1: Setup

- [ ] T001 [Project init / scaffolding per plan.md]
- [ ] T002 [P] [Dependency installation]
- [ ] T003 [P] [Lint/format config]

## Phase 2: Foundational (blocks all user stories)

**⚠️ No user-story work starts until this phase is complete.**

- [ ] T004 [Shared schema/migration]
- [ ] T005 [P] [Shared auth/middleware, if needed]
- [ ] T006 [Base models all stories depend on]

**Checkpoint**: foundation ready — user stories can now proceed, in parallel
if staffed.

## Phase 3: User Story 1 — [Title] (Priority: P1) 🎯 MVP

**Goal**: [what this story delivers, one line]
**Independent Test**: [how to verify this story on its own]

### Tests for User Story 1 — write first, confirm they fail

- [ ] T007 [P] [US1] Contract test for [endpoint] in `tests/contract/...`

### Implementation for User Story 1

- [ ] T008 [P] [US1] [Model] in `src/models/...`
- [ ] T009 [US1] [Service] in `src/services/...` (depends on T008)
- [ ] T010 [US1] [Endpoint/feature] in `src/...`
- [ ] T011 [US1] Validation + error handling
- [ ] T012 [US1] Logging for this story's operations

### Formal Verification for User Story 1 *(only if spec.md's Formal
Verification Obligations names one for this story)*

- [ ] T012a [US1] Draft [Lean 4 theorem | TLA+ spec] for [the obligation] via
  `lean4-theorem-writer`/`tlaplus-spec-writer` (per plan.md's tool choice) —
  `[exact file path]`
- [ ] T012b [US1] Verify via `lean4-verifier`/`tlaplus-verifier`; on a failing
  read, hand the diagnostic back to the writer task above for one revision
  round before re-verifying (see `develop-feature` Phase 4)

**Checkpoint**: User Story 1 is fully functional and independently testable
(including formally verified, if T012a/T012b apply).

## Phase 4: User Story 2 — [Title] (Priority: P2)

[Same shape as Phase 3.]

**Checkpoint**: User Stories 1 and 2 both work independently.

## Phase N: Polish & Cross-Cutting

- [ ] [Docs updates]
- [ ] [Refactoring/cleanup]
- [ ] [Performance pass across all stories]
- [ ] [Security hardening]

## Review Gate

**⚠️ No feature is complete until this gate clears.**

- [ ] TREVIEW Invoke the `code-reviewer` agent: pass `git diff main` (or equivalent), `specs/<NNN>/spec.md`, and `specs/<NNN>/decision-log.md`. Do not skip — if the user explicitly declines, record the skip in `decision-log.md`.

`code-reviewer` runs four deterministic gates before its qualitative read —
clean build, test coverage floor, static analysis / cognitive complexity,
formal verification obligations (commands from `AGENTS.md`, thresholds from
`memory/constitution.md`'s Quality Gates) — alongside spec/constitution
conformance, security, and — for a Formal Verification Obligation — a
qualitative correspondence check between the formal artifact and the real
implementation (`code-reviewer.md`'s own check, not something the verifier
tool judges). If this feature has a Formal Verification Obligation, the
caller also feeds it a fresh `lean4-verifier`/`tlaplus-verifier` report
before invoking it. A failing gate is a Blocker like any other finding,
tagged with a `Kind`: `defect` (e.g. broken build) routes to `debugger`,
`design` (e.g. a complexity finding) routes to `implementor`, `coverage` (a
gap below the floor) routes to `test-writer`, `formal` (a not-verified
result, *or* a verified result whose correspondence to the real
implementation doesn't hold up) routes to the matching
`lean4-theorem-writer`/`tlaplus-spec-writer` then the matching verifier —
see `develop-feature`'s Phase 5 for the fix-loop.

On approval, append a **Review** row to `decision-log.md` (verdict + model used).

## Dependencies

- Setup → Foundational → (User stories, parallel or priority order) → Polish → Review Gate
- Within a story: tests before implementation; models before services;
  services before endpoints; story complete before moving to the next.

## Implementation Strategy

**MVP first**: Setup → Foundational → User Story 1 only → stop, validate,
deploy/demo if ready, *then* continue to the next story. Don't build all
stories before validating the first one works end-to-end.
