# Tasks: order lifecycle management with formally verified status state machine

**Input**: `spec.md` (required), `plan.md` (required), `research.md`. | **Status**: Approved — sapb2004@gmail.com, 2026-07-18

## Phase 1: Setup

- [x] T001 [P] Write `src/main/resources/scripts/V1__create_orders.sql` — `orders` table DDL: `id UUID PK`, `status VARCHAR NOT NULL`, `inventory_reserved BOOLEAN NOT NULL`, `version BIGINT NOT NULL` (JPA `@Version`), `created_at`/`updated_at TIMESTAMP`.
- [x] T002 [P] Write `src/main/resources/scripts/V2__create_order_status_history.sql` — `order_status_history` table DDL: `id BIGINT GENERATED ... AS IDENTITY PK`, `order_id UUID NOT NULL REFERENCES orders(id)`, `status VARCHAR NOT NULL`, `changed_at TIMESTAMP NOT NULL`; no update/delete path (append-only).

## Phase 2: Foundational (blocks all user stories)

**⚠️ No user-story work starts until this phase is complete.**

- [ ] T003 [P] Write failing unit tests for the pure transition table in `src/test/java/com/formalmethods/domain/OrderLifecycleTest.java` — assert the relation admits exactly the 5 forward edges (NEW→INVENTORY_RESERVED→PROVISIONED→DISPATCHED→DELIVERED→CLOSED) and the 3 cancel edges ({NEW, INVENTORY_RESERVED, PROVISIONED}→CANCELLED) — 8 legal edges total, and no others, and that CLOSED/CANCELLED have no outgoing transition (FR-002/003/004). Confirm they fail before T004/T005 exist.
- [ ] T004 Implement `OrderStatus` enum (`NEW, INVENTORY_RESERVED, PROVISIONED, DISPATCHED, DELIVERED, CLOSED, CANCELLED`) in `src/main/java/com/formalmethods/domain/OrderStatus.java` (depends on T003 being red).
- [ ] T005 Implement the pure `OrderLifecycle` transition table (legality relation over `OrderStatus`) in `src/main/java/com/formalmethods/domain/OrderLifecycle.java`, making T003 pass (depends on T004).
- [x] T006 [P] Create `Order` `@Entity` (`id`, `status`, `inventoryReserved`, `@Version version`, `createdAt`/`updatedAt`) in `src/main/java/com/formalmethods/domain/Order.java`.
- [x] T007 [P] Create `StatusHistoryEntry` `@Entity` (`id`, `orderId`, `status`, `changedAt`, append-only, no setters exposed beyond construction) in `src/main/java/com/formalmethods/domain/StatusHistoryEntry.java`.
- [x] T008 [P] Create `OrderRepository extends JpaRepository<Order, UUID>` in `src/main/java/com/formalmethods/repository/OrderRepository.java`.
- [x] T009 [P] Create `StatusHistoryRepository` (with `findByOrderIdOrderByIdAsc(...)`) in `src/main/java/com/formalmethods/repository/StatusHistoryRepository.java`.
- [x] T010 [P] Create DTOs in `src/main/java/com/formalmethods/dto/`: `StatusUpdateRequest.java` (`{ targetStatus }`, `@NotNull` + enum allow-list validation per SEC-01/FR-014), `OrderResponse.java`, `HistoryEntryResponse.java`, `ErrorResponse.java` (generic client-facing shape, SEC-07/FR-017).
- [x] T011 Create `ApiExceptionHandler` (`@RestControllerAdvice`) skeleton in `src/main/java/com/formalmethods/web/ApiExceptionHandler.java` mapping unhandled/domain exceptions to generic `ErrorResponse` bodies with appropriate HTTP codes, fail-closed by default (SEC-07/FR-017).

**Checkpoint**: foundation ready — user stories can now proceed in priority order.

## Phase 3: User Story 1 — Drive an order through its valid lifecycle (Priority: P1) 🎯 MVP

**Goal**: An order can be created and advanced one legal step at a time through NEW → INVENTORY_RESERVED → PROVISIONED → DISPATCHED → DELIVERED → CLOSED; any illegal jump or any update on a terminal order is rejected with state unchanged; current state is readable at any point.
**Independent Test**: Create an order, apply each valid update in order and confirm the current state advances correctly; attempt an illegal jump (e.g. NEW → DISPATCHED) and confirm rejection with unchanged state.

### Tests for User Story 1 — write first, confirm they fail

- [x] T012 [P] [US1] Write failing service tests in `src/test/java/com/formalmethods/service/OrderServiceTest.java` for `create` (starts NEW) and `applyStatusUpdate` covering Acceptance Scenarios 1–5 (valid advance, illegal jump rejected/state unchanged, terminal-state rejection, read current state).
- [x] T013 [P] [US1] Write failing controller tests in `src/test/java/com/formalmethods/web/OrderControllerTest.java` for `POST /api/orders`, `POST /api/orders/{orderId}/status`, `GET /api/orders/{orderId}`.

### Implementation for User Story 1

- [x] T014 [US1] Implement `OrderService.create` and `applyStatusUpdate` in `src/main/java/com/formalmethods/service/OrderService.java`, using `OrderLifecycle` to admit only legal transitions (depends on T012/T013 red, and Foundational T005/T006/T008).
- [x] T015 [US1] Implement `OrderController` endpoints `POST /api/orders`, `POST /api/orders/{orderId}/status`, `GET /api/orders/{orderId}` in `src/main/java/com/formalmethods/web/OrderController.java` (depends on T014).
- [x] T016 [US1] Wire boundary validation for target-status allow-list and order-id well-formedness (FR-014, SEC-01) into `StatusUpdateRequest`/`OrderController`, with a defined rejection outcome (depends on T015).
- [x] T017 [US1] Add SLF4J logging for accepted and rejected transitions (actor, action, target order, outcome) in `OrderService` (FR-018, SEC-07) (depends on T014).

### Formal Verification for User Story 1

- [ ] T017a [US1] Draft the Lean 4 theorem for the sequential transition-function property (spec.md's [US1] obligation: the transition relation admits exactly the 5 forward edges and 3 cancel edges (8 legal edges total) and no others; CLOSED/CANCELLED have no outgoing transition; no cancel edge from DISPATCHED/DELIVERED/CLOSED/CANCELLED) via `lean4-theorem-writer`, modeling `OrderLifecycle` — `Proofs/OrderTransition.lean`.
- [ ] T017b [US1] Verify via `lean4-verifier`; on a failing read, hand the diagnostic back to T017a for one revision round before re-verifying (see `develop-feature` Phase 4).

**Checkpoint**: User Story 1 is fully functional, independently testable, and formally verified ([US1] Lean 4 proof green).

## Phase 4: User Story 2 — Cancel an order with inventory release (Priority: P1)

**Goal**: An order can be cancelled at any point strictly before DISPATCHED, moving it to CANCELLED; inventory reserved at INVENTORY_RESERVED or later (pre-DISPATCHED) is released exactly once; cancellation at/after DISPATCHED is rejected.
**Independent Test**: Cancel an order in NEW (expect CANCELLED, no release); cancel an order in INVENTORY_RESERVED (expect CANCELLED + exactly one release); attempt to cancel an order in DISPATCHED (expect rejection, state unchanged).

### Tests for User Story 2 — write first, confirm they fail

- [ ] T018 [P] [US2] Write failing service tests in `src/test/java/com/formalmethods/service/OrderServiceTest.java` for `cancel` covering Acceptance Scenarios 1–4 (NEW: no release; INVENTORY_RESERVED/PROVISIONED: exactly one release; DISPATCHED: rejected; already-CANCELLED: idempotent, no second release).
- [ ] T019 [P] [US2] Write failing controller tests in `src/test/java/com/formalmethods/web/OrderControllerTest.java` for `POST /api/orders/{orderId}/cancel`.

### Implementation for User Story 2

- [x] T020 [P] [US2] Create concrete `InventoryReleaseClient` (single method `release(UUID orderId)`, logs the release, no-op body; no interface per constitution Article V) in `src/main/java/com/formalmethods/service/InventoryReleaseClient.java`.
- [x] T021 [US2] Implement `OrderService.cancel` in `OrderService.java`, invoking `InventoryReleaseClient.release` exactly once only for a qualifying cancellation (FR-005/006) (depends on T018/T019 red, T020).
- [x] T022 [US2] Implement `POST /api/orders/{orderId}/cancel` endpoint in `OrderController.java` (200 on accept/idempotent repeat, 409 on illegal cancel) (depends on T021).
- [x] T023 [US2] Extend SLF4J logging to cover cancellations and inventory-release invocations (FR-018, SEC-07) (depends on T021).

**Checkpoint**: User Stories 1 and 2 both work independently.

## Phase 5: User Story 3 — Append-only audit history that always agrees with current state (Priority: P1)

**Goal**: Every accepted status change appends exactly one history entry; current state always equals the latest history entry, even across a crash or retry; rejected updates append nothing; history reads back chronologically.
**Independent Test**: Apply a sequence of valid updates and confirm each produces exactly one history entry matching current state; apply a rejected update and confirm no entry is added; replay a just-applied update and confirm no divergence/duplicate effect.

### Tests for User Story 3 — write first, confirm they fail

- [x] T024 [P] [US3] Write failing service tests in `src/test/java/com/formalmethods/service/OrderServiceTest.java` covering Acceptance Scenarios 1–4 (entry appended on accept and matches current state; no entry on reject; retry/duplicate produces no second entry/effect; chronological order with last entry matching current state).
- [x] T025 [P] [US3] Write failing controller test in `src/test/java/com/formalmethods/web/OrderControllerTest.java` for `GET /api/orders/{orderId}/history`.

### Implementation for User Story 3

- [x] T026 [US3] Implement the atomic `@Transactional` append-history-and-update-state unit in `OrderService.applyStatusUpdate`/`cancel` so the two can never diverge, including across a crash or retry (FR-007/008) (depends on T024/T025 red).
- [x] T027 [US3] Implement idempotent no-op handling in `OrderService` — a target equal to current status (or a repeat cancel on an already-CANCELLED order) is an accepted no-op: no new history entry, no repeated side effect (FR-011) (depends on T026).
- [x] T028 [US3] Implement `GET /api/orders/{orderId}/history` endpoint in `OrderController.java` returning `List<HistoryEntryResponse>` in chronological order (FR-010) (depends on T026).
- [x] T029 [US3] Implement not-found handling (404, no order/history created) for status-update, cancel, read-state, and read-history on a non-existent order id across `OrderService`/`ApiExceptionHandler` (FR-015) (depends on T015, T022, T028).

**Checkpoint**: User Stories 1, 2, and 3 all work independently; history/state consistency holds under retry.

## Phase 6: User Story 4 — Correct behavior under at-least-once, out-of-order, and concurrent delivery (Priority: P2)

**Goal**: Duplicate, delayed, out-of-order, and concurrent status updates never corrupt state or history; of two conflicting concurrent updates, at most one is accepted.
**Independent Test**: Replay a duplicate update and confirm no double effect; deliver a premature update and confirm rejection with correct final state; issue two conflicting concurrent updates and confirm exactly one is accepted with consistent history/state afterward.

### Tests for User Story 4 — write first, confirm they fail

- [x] T030 [P] [US4] Write failing tests in `src/test/java/com/formalmethods/service/OrderServiceTest.java` for duplicate delivery (Scenario 1: duplicate INVENTORY_RESERVED produces no additional state change/entry) and premature/out-of-order delivery (Scenario 2: PROVISIONED before INVENTORY_RESERVED is rejected, order stays NEW).
- [x] T031 [P] [US4] Write a failing concurrency test in `OrderServiceTest.java` that drives the mocked `OrderRepository` to throw `ObjectOptimisticLockingFailureException` on the losing write, asserting exactly one of two conflicting concurrent updates (e.g. DISPATCHED vs. cancel) is accepted and history/state remain consistent (Scenario 3, FR-013).

### Implementation for User Story 4

- [x] T032 [US4] Implement optimistic-lock conflict handling in `OrderService` — catch `ObjectOptimisticLockingFailureException` from the `@Version`-guarded write and map it to a rejection (depends on T030/T031 red, Foundational T006's `@Version`).
- [x] T033 [US4] Map the optimistic-lock rejection to HTTP 409 in `ApiExceptionHandler`/`OrderController` for the status-update and cancel endpoints (depends on T032).
- [x] T034 [US4] Confirm and extend the idempotency (T027) and out-of-order-rejection paths across `applyStatusUpdate`/`cancel` so duplicate and premature deliveries are absorbed without corrupting state or history (FR-012) (depends on T027, T032).

### Formal Verification for User Story 4 (covers spec.md's [US2/US3/US4] safety and [US4] liveness obligations)

- [x] T034a [US2/US3/US4] Draft the TLA+ spec and config modeling the concurrent mutation protocol via `tlaplus-spec-writer` — safety invariants (a) `currentStatus = Last(history)` always, (b) history append-only with no illegal transition, (c) exactly-once effect per logical transition, (d) `inventoryReleaseCount ≤ 1` and only for a qualifying cancellation, (e) at most one of two conflicting concurrent updates accepted; plus the [US4] liveness property (every reachable non-terminal state eventually reaches CLOSED or CANCELLED under fair delivery) — `tla/OrderLifecycle.tla` and `tla/OrderLifecycle.cfg` (path corrected from plan.md's original `Specs/` — see decision-log's Plan (amendment) row: `Specs/`/`specs/` collide on a case-insensitive filesystem).
- [x] T034b [US2/US3/US4] Verify via `tlaplus-verifier`; on a failing read, hand the diagnostic back to T034a for one revision round before re-verifying (see `develop-feature` Phase 4).

**Checkpoint**: All four user stories work independently and together; concurrent/duplicate/out-of-order behavior is formally model-checked.

## Phase 7: Polish & Cross-Cutting

- [ ] T035 [P] Extend boundary-validation test coverage (SEC-01, FR-014) across all five endpoints — malformed/out-of-range target status and malformed order id on create/status/cancel/read/history — in `src/test/java/com/formalmethods/web/OrderControllerTest.java`.
- [ ] T036 [P] Verify SEC-07/FR-017 compliance — client-facing errors are generic across every rejection path, and server-side logs carry full context (actor, action, target order, outcome) without secrets — via tests in `src/test/java/com/formalmethods/web/ApiExceptionHandlerTest.java` and `OrderServiceTest.java`.
- [ ] T037 Manually verify `V1__create_orders.sql`/`V2__create_order_status_history.sql` against a local PostgreSQL instance and confirm Hibernate `ddl-auto: validate` accepts the entity mappings (per `research.md`'s decision; this is the manual local-DB check, not part of the automated suite — constitution Article IV). During this same manual local-PostgreSQL run, spot-check that each of the five operations (create order, apply status update, cancel, get current state, get status history) completes comfortably within the spec's 1-second performance target — a manual timing observation, not an automated integration test; no Spring-context-loading test is added for this, consistent with constitution Article IV's mock-only test-isolation rule.
- [ ] T038 [P] Run the full suite and coverage gate — `scripts/quiet.sh ./gradlew test` and `./gradlew jacocoTestCoverageVerification` — confirming all four stories pass together and the 90% coverage floor holds.

## Review Gate

**⚠️ No feature is complete until this gate clears.**

- [ ] TREVIEW Invoke the `code-reviewer` agent: pass `git diff main` (or equivalent), `specs/001-order-lifecycle-management-with-formally/spec.md`, and `specs/001-order-lifecycle-management-with-formally/decision-log.md`. Do not skip — if the user explicitly declines, record the skip in `decision-log.md`.

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

- Setup → Foundational → User Story 1 → User Story 2 → User Story 3 → User Story 4 (priority order; US2/US3 could proceed in parallel once Foundational is done since they touch largely disjoint concerns, but US4 and the joint [US2/US3/US4] TLA+ obligation depend on US2's cancel path and US3's history/idempotency mechanisms being in place) → Polish → Review Gate.
- Within a story: tests before implementation; models/entities before services; services before endpoints; formal-verification draft before its verify task.
- T004/T005 (OrderLifecycle) block all per-story service implementation tasks (T014, T021, T026, T032).
- T027 (US3 idempotency) is a direct dependency of T034 (US4 out-of-order/duplicate handling) — US4 builds on, not duplicates, US3's idempotency work.

## Implementation Strategy

**MVP first**: Setup → Foundational → User Story 1 only → stop, validate (including the [US1] Lean 4 proof), then continue to User Story 2, User Story 3, and finally User Story 4 with its joint TLA+ obligation. Don't build all stories before validating the first one works end-to-end.
