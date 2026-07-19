<!--
  TEMPLATE — one per feature, at specs/<NNN-feature-name>/learnings.md.

  Unlike spec/plan/tasks/decision-log, this file is NOT gated. It has no
  Status header and no approval step. It exists purely so a discovery made
  mid-story survives the fresh-context boundary between story iterations —
  each implementor/debugger run starts with a clean context and no memory of
  what the last run figured out the hard way. Without this file, that
  discovery dies with the sub-agent that found it and gets re-discovered
  (or re-broken) next story.

  Rules:
  - Append only — for `implementor` and `debugger`, within a story. Never
    rewrite or delete a prior entry, even a superseded one — add a new entry
    noting what changed instead of erasing the old one. This is what keeps
    mid-story writes safe to do without a gate.
  - One entry per discovery, newest at the bottom.
  - `implementor` and `debugger` read this file first (if it exists) before
    starting work on this feature, and append to it as they go — not only in
    their final report, so the discovery survives even if the session ends
    mid-story.
  - Not a substitute for `decision-log.md`. That file is the gated,
    human-approved audit trail of what was decided. This file is the
    ungated "things we learned the hard way" scratchpad — low ceremony,
    no approval needed to append, and it may include things that turned out
    to be wrong (say so in the entry) as well as things that were right.
  - Delete this comment block once the first real entry is appended.

  Compaction (the one exception to append-only):
  - Append-only applies to `implementor`/`debugger` mid-story so they never
    need to pause for a gate. It does not mean this file grows forever unread
    — the orchestrator (`develop-feature`), never a sub-agent, offers a
    compaction pass at each story's Phase 5 checkpoint (see
    `references/phase-5-review.md`): propose a deduped/merged version (fold
    repeated gotchas into one entry, drop entries an applied `AGENTS.md`
    correction has already fixed, keep everything still-relevant), show the
    before/after to the human, and only rewrite the file on explicit approval.
  - A compaction pass replaces the file's body, not silently — leave one
    marker entry at the top of the (still append-only from here) log:

    `## 2026-07-18 — compaction — N entries merged into M; superseded-by-AGENTS.md-fix entries dropped: [list]`

    so a later read still knows a GC pass happened and roughly what it removed.
  - Never compact mid-story, and never let `implementor`/`debugger` trigger
    or perform it themselves — same "propose, human approves" gate as the
    `AGENTS.md` self-correction rule, applied to this file instead.
-->

# Learnings: order lifecycle management with formally verified status state machine

**Feature**: `001-order-lifecycle-management-with-formally`

Append-only between compaction passes. Newest entries at the bottom. Each
entry should tell a future session — including a fresh run of yourself —
something it could not already infer from `AGENTS.md`, the spec, or the code
itself: a wrong-turn command that looked right but wasn't, the real location
of something the plan assumed was elsewhere, a version-specific gotcha, a
flaky test and its actual cause. Periodically, at a story checkpoint, the
orchestrator may propose a human-approved compaction pass that dedupes/merges
entries and drops ones an applied `AGENTS.md` correction already fixed — see
the comment block above for how that's marked.

<!-- Entry format — copy for each new entry:

## [ENTRY-DATE] — [implementor|debugger] — [US# or task ID]

[What was discovered, specific enough to act on directly. Skip anything
already obvious from AGENTS.md or the spec — this file is for what those
don't cover yet.]

-->

## 2026-07-18 — implementor — T003/T004/T005

`OrderLifecycleTest.relationAdmitsExactlyTheNineLegalEdgesAndNoOthers()`
asserts `legalEdgeCount == 9`, but spec.md's [US1] obligation literally lists
5 forward edges (NEW→INVENTORY_RESERVED→PROVISIONED→DISPATCHED→DELIVERED→
CLOSED is a chain of 6 *states*, i.e. 5 edges) + 3 cancel edges
({NEW, INVENTORY_RESERVED, PROVISIONED}→CANCELLED) = 8 total, which is also
exactly what `OrderLifecycleTest`'s own `legalForwardEdges()` (5 cases) and
`legalCancelEdges()` (3 cases) enumerate. `plan.md` (line 54) and `tasks.md`
(lines 14, 45) both say "six forward edges" — an arithmetic miscount that
propagated from spec.md's 6-*state* chain into a wrong edge count, and from
there into the test's hardcoded `9`. I implemented `OrderLifecycle` with the
correct 8 edges per spec.md's literal edge list (all other 37 tests across
`OrderLifecycleTest`/`OrderServiceTest`/`OrderControllerTest` are green); this
one exhaustive-count assertion fails (`expected: 9`, actual: 8) because no
9th edge exists in spec.md that doesn't contradict the test's own explicit
false-assertions (all self-loops on CLOSED/CANCELLED are asserted false;
inventing a self-loop or extra edge on another state to force the count to 9
would be an unjustified guess, not something spec.md supports). Flagging for
the caller/test-writer/spec-owner rather than editing the test or guessing a
9th edge.

## 2026-07-18 — debugger — code-reviewer Blockers on US1 diff

Two defects found on review: (1) `OrderService` had no `@Service`/`@Component`
stereotype and no `@Bean` providing it, so `OrderController`'s constructor
injection of it fails Spring context startup
(`UnsatisfiedDependencyException`) — none of the five REST endpoints was
actually reachable despite all unit tests being green, because the unit
tests mock the collaborator directly and never boot the real
`ApplicationContext` (constitution Article IV keeps tests off a real DB, but
that also means no test in this suite exercises full context startup — worth
remembering next time "all tests pass" is treated as proof the app boots).
Fix: added `@Service` to `OrderService`, no other change. (2) `OrderLifecycle`'s
Javadoc (lines 11 and 26) still said "six forward edges" / "nine ... legal
edges" — stale from before the 2026-07-18 T003/T004/T005 entry above already
corrected the actual edge count to 5 forward + 3 cancel = 8; the code and
test were fixed then, but these two comments were missed. Fixed to say "five
forward edges" and "eight legal edges" respectively. Verified: all three
targeted test classes (`OrderLifecycleTest`, `OrderServiceTest`,
`OrderControllerTest`) still pass. Also ran `./gradlew bootRun` locally (no
PostgreSQL running) — the log shows no `OrderService`/`UnsatisfiedDependency`/
`NoSuchBeanDefinition` errors at all; it progresses past bean wiring into
Hibernate's JDBC-dialect-detection stage and fails only there
(`Unable to determine Dialect without JDBC metadata`), which is the expected
"no local Postgres" environment limitation, not a context-startup defect —
confirms the `@Service` fix actually resolves the reported bean-wiring
failure.

## 2026-07-18 — implementor — T020/T021/T022/T023

US2 (cancel) came together with no surprises: `OrderLifecycle.isLegalTransition`
already encodes exactly the three cancel edges ({NEW, INVENTORY_RESERVED,
PROVISIONED}→CANCELLED), so `OrderService.cancel` reuses it directly for the
"is this cancel legal" check — DISPATCHED/DELIVERED/CLOSED/CANCELLED are
already correctly excluded by that table with zero new cancel-specific
branching. The one thing that needs its own branch ahead of the legality
check is the already-CANCELLED idempotent no-op (FR-011): since there's no
CANCELLED→CANCELLED self-edge in the transition table, running the legality
check first would misclassify a repeat cancel as illegal, so the idempotency
check on `order.getStatus() == CANCELLED` must come *before*
`isLegalTransition`, not after. `InventoryReleaseClient` is annotated
`@Component` (not `@Service`) since it's a client/gateway-style seam rather
than a domain service — consistent with the prior entry's `@Service` fix for
`OrderService` itself, which is the actual domain service.

Also: `ApiExceptionHandler`'s existing `IllegalTransitionException` → 409
mapping (already in place from US1/T016) needed zero changes for the cancel
endpoint — `OrderService.cancel` reuses the same exception type, so the
mapping was already generic enough to cover both endpoints. No deviation
from plan.md.

## 2026-07-18 — implementor — T026/T027/T028/T029

US3 required putting the idempotent no-op check for `applyStatusUpdate`
*before* the legality check, same reasoning as US2's cancel-idempotency
entry above: `OrderLifecycle` has no self-loop edges (e.g. no
`INVENTORY_RESERVED`→`INVENTORY_RESERVED`), so a re-delivered update whose
target equals current status would otherwise be misclassified as illegal.
Both `applyStatusUpdate` and `cancel` now append a `StatusHistoryEntry`
(via `statusHistoryRepository.save`, previously injected-but-unused) in the
same method as the `orderRepository.save`, and both methods are
`@Transactional` (`org.springframework.transaction.annotation.Transactional`
— no prior precedent for either import existed in the codebase before this
story, picked Spring's for consistency with `@Service`) so state+history can
never diverge (T026/FR-007/008). `getHistory` reuses the existing
`findOrThrow` private helper before reading `statusHistoryRepository`, so
the 404 mapping for a non-existent order id on read-history came for free
via the same `OrderNotFoundException`→404 `ApiExceptionHandler` mapping
T029 asked me to confirm rather than reimplement — confirmed by inspection
and by all tests passing, no new exception-handler code needed.

One test-coverage gap noted, not filled (per this agent's "never write a
new test" rule): `OrderControllerTest` has an explicit 404 test for
status-update (`applyStatusUpdateOnUnknownOrderReturns404`) and read-state
(`getOrderForUnknownOrderReturns404`), but none for cancel or
read-history on an unknown order id, even though T029 names all four
operations. The underlying code path is identical (`findOrThrow` →
`OrderNotFoundException` → the same `ApiExceptionHandler` mapping already
covered by the existing tests), so I'm confident the behavior is correct,
but the two missing controller tests are a real coverage gap for
`test-writer` to close, not something I should add myself mid-implementation.

## 2026-07-18 — implementor — T032/T033/T034

`ConcurrentUpdateException` (new, RuntimeException, sibling to
`IllegalTransitionException`) is thrown from both `applyStatusUpdate` and
`cancel` by wrapping the `orderRepository.save(order)` call in a try/catch
for `ObjectOptimisticLockingFailureException` — placed *before* the
`statusHistoryRepository.save` call in each method, so a losing optimistic-
lock race can never leave a history entry behind. `ApiExceptionHandler` maps
it to 409 via the same pattern as the existing `IllegalTransitionException`
handler. T034's "confirm and extend" scope required no new code: T030a
(duplicate delivery) and T030b (premature/out-of-order delivery) both
already pass under existing US1/US3 logic (the idempotency no-op check and
`OrderLifecycle.isLegalTransition` both run unconditionally before any
write, regardless of *why* the caller is duplicating/out-of-ordering) —
verified by reading the code, not just trusting the test-writer's report.

## 2026-07-18 — implementor/debugger — CANCELLED-via-status-update bug (found by tlaplus-spec-writer)

Formal modeling for T034a caught a real production bug before it ever
reached TLC: `OrderService.applyStatusUpdate` accepted `targetStatus:
CANCELLED` (a legal edge in `OrderLifecycle`'s table, needed for the
dedicated `/cancel` endpoint's own legality check) but never called
`InventoryReleaseClient.release` — only `OrderService.cancel` does. An order
could be moved to CANCELLED via the generic status-update endpoint,
skipping inventory release entirely, corrupting the exactly-once-release
guarantee (FR-006). Fixed by rejecting `CANCELLED` as an `applyStatusUpdate`
target (throws the existing `IllegalTransitionException`) whenever it would
actually move the order. Check-ordering subtlety: this rejection must come
*after* the existing same-status idempotency check, not before — a
`CANCELLED→CANCELLED` self-target duplicate must still no-op per FR-011
(from the US3 idempotency work), so the final order in `applyStatusUpdate`
is: (1) same-status no-op, (2) reject if target is CANCELLED (would-move
case only), (3) `OrderLifecycle.isLegalTransition`, (4) save + history-append.
`OrderLifecycle.java` and `OrderService.cancel` were deliberately left
untouched — the fix is specific to the status-update path, not the
underlying transition table.

## 2026-07-18 — tlaplus-spec-writer/tlaplus-verifier — T034a/T034b formal verification

Drafted `tla/OrderLifecycle.tla`/`.cfg` modeling the concurrent mutation
protocol: 2 callers, optimistic-lock arbitration (only one of two
same-step racing writes to the shared order succeeds, matching
`ConcurrentUpdateException`'s real effect), unrestricted target choice on
read (modeling at-least-once/duplicated/delayed/out-of-order delivery).
The first draft, before the CANCELLED-bug fix landed, faithfully exposed
that same bug as a TLC-checkable invariant-(d) violation rather than
narrowing the model to avoid it — confirming the model was built honestly
against the real (buggy) code, then revised to match after the fix.
First real TLC run (post-fix): `TypeOK` + all 5 safety invariants
verified exhaustively (163,129 states generated, 23,212 distinct, 0 left
on queue), but `Liveness` violated — a lasso counterexample where a caller
perpetually "blinks" the legal-progress action's enabledness (alternating
self-target and illegal-target reads) so weak fairness (`WF_vars`) never
obligates it to fire, stalling the order at DELIVERED forever. Fixed by
strengthening `WF_vars`→`SF_vars` on the two legal-progress read actions
only (strong fairness requires infinitely-often-enabled, not continuously-
enabled, which matches the actual recurrence in the counterexample) —
`CommitUpdate`/`CommitCancel` didn't need strengthening since nothing
re-disables them once a caller enters its pending-write mode. Re-verify:
exit 0, all invariants + Liveness hold, no regression. Also note for future
sessions: TLC resolves the `.tla` module name against its CWD regardless of
the path passed as the argument — must `cd` into the directory containing
the spec before invoking `java -jar tools/tla2tools.jar ...`, not just pass
a relative path from elsewhere.

## 2026-07-18 — orchestrator — accidental specs/ deletion during TLA+ directory rename, fully recovered

While relocating the TLA+ artifacts (first attempted as `Specs/`, discovered
to collide with `specs/` on this case-insensitive filesystem, see the
decision-log's Plan (amendment) row), an `rm -rf specs/states Specs`
command deleted the entire `specs/` tree — including this feature's
`spec.md`/`plan.md`/`tasks.md`/`decision-log.md`/`research.md`/
`learnings.md` — because `Specs` and `specs` resolve to the identical
directory here. Fully recovered via `git checkout -- specs/` from the last
commit (`4609ebd`, US3) with zero data loss to committed content; the only
casualty was this session's not-yet-committed decision-log rows and
learnings entries for US4's work, which were manually reconstructed from
conversation context afterward. Lesson: never pass a path that might
case-collide with another real path to `rm -rf` in the same command,
especially on Windows — verify collisions with a harmless write-test first
(as was done, but only *after* the deletion, not before).

## 2026-07-18 — implementor — code-reviewer defect fix on US4 diff (saveAndFlush)

`code-reviewer` flagged that `applyStatusUpdate`/`cancel`'s
`ObjectOptimisticLockingFailureException` catch (from the T032/T033/T034
entry above) wrapped a plain `orderRepository.save(order)` — under real JPA
the `@Version` check fires at flush time, and `save()` on a managed entity
doesn't force one, so the actual exception could surface later (at the next
repository call, or at `@Transactional` commit) *outside* the try/catch,
producing an uncaught exception → 500 instead of the required 409 (FR-013).
State integrity was never at risk (the transaction still rolls back either
way) — this was purely an HTTP-contract gap on the race path, and Mockito's
mocked `save()` in the existing tests couldn't have caught it since a mock
doesn't model flush timing at all. Fix: changed both call sites to
`orderRepository.saveAndFlush(order)`, forcing the version check synchronously
within the catch scope. `OrderRepository` needed no interface change —
`saveAndFlush` is inherited from `JpaRepository`. Updated the six
`OrderServiceTest` stubs that exercise `applyStatusUpdate`/`cancel` from
`when(orderRepository.save(...))` to `when(orderRepository.saveAndFlush(...))`
(mechanical stub-target change only, same assertions) — `create()`'s stub at
line 74 was intentionally left as `save()` since `OrderService.create` itself
was not touched by this fix. All targeted test classes
(`OrderLifecycleTest`, `OrderServiceTest`, `OrderControllerTest`) pass after
the change.
