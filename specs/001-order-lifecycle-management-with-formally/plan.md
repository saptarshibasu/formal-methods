# Implementation Plan: order lifecycle management with formally verified status state machine

**Branch**: `001-order-lifecycle-management-with-formally` | **Spec**: `specs/001-order-lifecycle-management-with-formally/spec.md` | **Status**: Approved — sapb2004@gmail.com, 2026-07-18

## Summary

Deliver an ordered, auditable, concurrency-safe order lifecycle (NEW →
INVENTORY_RESERVED → PROVISIONED → DISPATCHED → DELIVERED → CLOSED, plus a
pre-DISPATCHED CANCELLED branch) as a Spring Boot REST feature backed by
PostgreSQL/JPA. Correctness rests on three mechanisms: a single pure transition
table in the domain layer that admits only legal edges (rejecting every other
update while leaving state unchanged); per-operation `@Transactional` units that
append a history entry and update current state atomically (so the two can never
diverge, even across a crash or retry); and a JPA `@Version` optimistic lock on
the order row that arbitrates two conflicting concurrent updates so at most one
commits. The transition table is proved correct in Lean 4 ([US1]); the
concurrent protocol's safety and liveness are model-checked in TLA+
([US2/US3/US4]).

## Technical Context

- **Language/Version**: Java 17
- **Primary Dependencies**: Spring Boot 3.2.5 (Web, Data JPA, Validation,
  Actuator), Lombok, PostgreSQL JDBC driver
- **Storage**: PostgreSQL via Spring Data JPA; schema created manually from DDL
  scripts under `src/main/resources/scripts/` (no migration tool; Hibernate
  `ddl-auto: validate` only checks entity mappings against the live schema)
- **Testing**: JUnit 5 + AssertJ + Mockito, mocking at the Spring Data JPA
  repository boundary — no Testcontainers, no embedded/real DB (constitution
  Article IV). Concurrency-rejection paths are driven by having the repository
  mock throw `ObjectOptimisticLockingFailureException`.
- **Target Platform**: Single deployable JVM service (multi-instance behavior is
  simulated, not deployed — Out of Scope)
- **Performance Goals**: Every create / status-update / cancel / read-state /
  read-history operation completes within 1 second (spec NFR)
- **Constraints**: No message broker/queue (upstream delivery exercised through
  the status-update API directly); no Docker/Testcontainers; constructor
  injection only; package-by-layer; this service NEVER invokes Lean 4 / TLA+
  tooling (constitution Article I)
- **Scale/Scope**: No volume/throughput target; correctness under concurrency on
  a single order is the requirement, not scale. Greenfield feature on a skeleton
  codebase (`Application.java` only).
- **Formal Verification**:
  - **[US1] — Lean 4** (pure sequential transition-function property; Lean's
    strength). Models the transition table in
    `com.formalmethods.domain.OrderLifecycle` (the pure
    `next(current, event)` / legality relation over the `OrderStatus` enum).
    Artifact: `Proofs/OrderTransition.lean`. The Lean model represents each
    lifecycle state as a constructor of an inductive `OrderStatus`
    (`NEW | INVENTORY_RESERVED | PROVISIONED | DISPATCHED | DELIVERED | CLOSED |
    CANCELLED`) and the transition function as a total function
    `step : OrderStatus → Event → Option OrderStatus` (an `Event` being an
    Advance-to-target or a Cancel). The proof obligations: (a) the relation
    admits exactly the five forward edges (the chain
    NEW → INVENTORY_RESERVED → PROVISIONED → DISPATCHED → DELIVERED → CLOSED is
    six states connected by five forward edges) and the three cancel edges
    ({NEW, INVENTORY_RESERVED, PROVISIONED} → CANCELLED) — eight legal edges in
    total — and no others;
    (b) CLOSED and CANCELLED have no outgoing transition (`step` returns `none`);
    (c) no cancel edge from DISPATCHED/DELIVERED/CLOSED/CANCELLED; (d) any state
    reachable from NEW by iterating `step` is one of the seven valid states —
    i.e. an out-of-order transition is unrepresentable, established
    exhaustively over all states and events, not by sampled cases.
  - **[US2/US3/US4] — TLA+** (concurrent-interleaving safety + [US4] liveness;
    TLC's strength). Models the `OrderService` mutation protocol for a single
    order. Artifacts: `Specs/OrderLifecycle.tla` + `Specs/OrderLifecycle.cfg`.
    The spec represents a fixed set of concurrent actor processes issuing
    status-update and cancel actions against state variables `currentStatus`,
    `history` (a sequence of statuses), `inventoryReserved` (bool), and
    `inventoryReleaseCount` (nat). Each accepted mutation is a single atomic
    action (modeling the `@Transactional` unit) that both sets `currentStatus'`
    and appends to `history'`; conflicting concurrent mutations are serialized so
    at most one commits (modeling the `@Version` optimistic-lock arbitration).
    Invariants to check: (a) `currentStatus = Last(history)` in every reachable
    state; (b) `history` is append-only and contains no illegal transition;
    (c) each logical transition appends exactly one entry and a re-delivered
    duplicate appends none; (d) `inventoryReleaseCount ≤ 1` and equals 1 only for
    a qualifying cancellation; (e) of two conflicting concurrent updates at most
    one is accepted. Liveness (temporal, under weak fairness on legal-update
    delivery): from any reachable non-terminal state,
    `<>(currentStatus ∈ {CLOSED, CANCELLED})` — no reachable non-terminal
    deadlock/stall.

  Both artifacts are checked by the framework's `lean4-verifier` /
  `tlaplus-verifier` agents on the machine running the coding assistant — never
  by this service (constitution Article I). No Java code shells out to
  `lean`/`lake`/TLC and no toolchain becomes a Gradle dependency.

## Constitution Check

<!-- GATE: must pass before research/design starts; re-check after. -->

- [x] **Article I (tool-execution boundary)** — PASS. The Lean 4 proof and TLA+
      spec are static artifacts under `Proofs/` and `Specs/`, verified by the
      framework agents outside the service. No Java class invokes a verification
      tool; no toolchain is added to `build.gradle`.
- [x] **Article II (reachable & observable)** — PASS. All five operations are
      REST endpoints; Actuator `health`/`info` already exposed; every accepted/
      rejected/invalid outcome is logged via SLF4J (FR-018).
- [x] **Article III (test-first)** — PASS (obligation on implementation).
      `tasks.md` will order a failing test before each implementation unit; each
      test names its acceptance-criterion ID and rationale. No production class
      is written before its red test.
- [x] **Article IV (isolated tests)** — PASS. Tests mock Spring Data JPA
      repositories via Mockito; the optimistic-lock rejection path is driven by a
      mock throwing `ObjectOptimisticLockingFailureException`. No real/embedded DB
      or Testcontainers.
- [x] **Article V (simplicity / anti-abstraction)** — PASS. ≤ 3 conceptual
      modules (this feature's slice of the existing single Spring module);
      inventory release is a single concrete class with no interface extracted;
      single JPA model per entity, no DTO-per-layer proliferation. See gate
      reasoning in the summary returned to the caller.
- [x] Any violation documented below with justification, not silently ignored —
      no violations; Complexity Tracking left empty.

**Framework gates** (Simplicity / Anti-abstraction / Isolation) — all PASS;
reasoning is returned in the caller summary and mirrored by Articles IV/V above.

## Project Structure

### Documentation (this feature)

```text
specs/001-order-lifecycle-management-with-formally/
├── spec.md          # approved WHAT/WHY
├── plan.md          # this file
├── research.md      # concurrency-simulation + tool-split resolutions
├── tasks.md         # generated from this plan, not by it
├── learnings.md     # append-only scratchpad
└── decision-log.md  # human-approved decisions / deviations
```

### Source Code (repository root)

```text
src/main/java/com/formalmethods/
├── domain/
│   ├── OrderStatus.java            # enum: NEW..CLOSED, CANCELLED
│   ├── OrderLifecycle.java         # pure transition table — Lean 4's target
│   ├── Order.java                  # @Entity: id, status, inventoryReserved, @Version
│   └── StatusHistoryEntry.java     # @Entity: id, orderId, status, changedAt (append-only)
├── dto/
│   ├── StatusUpdateRequest.java    # { targetStatus } — @NotNull, enum-validated
│   ├── OrderResponse.java          # { id, status, inventoryReserved }
│   ├── HistoryEntryResponse.java   # { status, changedAt }
│   └── ErrorResponse.java          # generic client-facing error (SEC-07)
├── repository/
│   ├── OrderRepository.java            # extends JpaRepository<Order, UUID>
│   └── StatusHistoryRepository.java    # findByOrderIdOrderByIdAsc(...)
├── service/
│   ├── OrderService.java           # create/apply/cancel/get/history; @Transactional
│   └── InventoryReleaseClient.java # concrete stub: logs the release, no-op body
├── web/
│   ├── OrderController.java         # the five REST endpoints
│   └── ApiExceptionHandler.java    # @RestControllerAdvice → HTTP codes + generic body
└── config/                         # (only if a bean needs explicit wiring)

src/main/resources/scripts/
├── V1__create_orders.sql               # orders table DDL (incl. version column)
└── V2__create_order_status_history.sql # order_status_history table DDL (FK, append-only)

src/test/java/com/formalmethods/         # package-mirrored unit tests (Mockito)

Proofs/
└── OrderTransition.lean                 # [US1] Lean 4 proof (single file; `lean <file>`)

Specs/
├── OrderLifecycle.tla                   # [US2/US3/US4] TLA+ spec
└── OrderLifecycle.cfg                   # TLC config (constants, invariants, PROPERTY)
```

**Structure Decision**: Package-by-layer under the existing
`com.formalmethods` root (`domain`/`dto`/`repository`/`service`/`web`/`config`)
per `AGENTS.md` — no new top-level package, no second Gradle module. Formal
artifacts live in capitalized `Proofs/` and `Specs/` directories at the repo
root (per the plan template's convention; the lowercase `specs/` tree is
reserved for this feature's spec/plan/tasks). No Lake project is scaffolded yet,
so `OrderTransition.lean` is a single file checked with `lean <file>` (per
`AGENTS.md` Formal Verification Tooling); if a later story needs a proof project
this can migrate to `lake build`. DDL scripts go under
`src/main/resources/scripts/` per the triggering prompt and `research.md`.

Key design points (detail for `task-decomposer`, not new scope):

- **Entities / schema.** `orders`: `id UUID PK`, `status VARCHAR NOT NULL`
  (enum name), `inventory_reserved BOOLEAN NOT NULL`, `version BIGINT NOT NULL`
  (JPA `@Version`), plus `created_at`/`updated_at TIMESTAMP`.
  `order_status_history`: `id BIGINT GENERATED ... AS IDENTITY PK`,
  `order_id UUID NOT NULL REFERENCES orders(id)`, `status VARCHAR NOT NULL`,
  `changed_at TIMESTAMP NOT NULL`. Chronological order is the monotonic
  identity `id` (with `changed_at` for display); the table is append-only — no
  update/delete path is exposed. Order id is a service-generated UUID, giving a
  clean well-formedness check at the boundary (FR-014).
- **Concurrency control (FR-013).** Optimistic locking via the `@Version`
  column. Each mutation runs in its own `@Transactional` method; two concurrent
  transactions racing on the same order read the same version, and the DB lets
  exactly one commit — the loser fails with
  `ObjectOptimisticLockingFailureException`, which `OrderService` catches and
  maps to a rejection (HTTP 409). This is the faithful stand-in for
  "different service instances" (see `research.md`): the shared row's version
  column is the single arbiter regardless of which JVM the writer ran in.
- **Atomic history/state (FR-008, crash consistency).** Within one
  `@Transactional` unit the service both appends the history entry and updates
  `orders.status`; a crash mid-change rolls back both, so current state and the
  latest history entry can never be observed in disagreement.
- **Idempotency (FR-011).** In `applyStatusUpdate`, if the requested target
  equals the current status, the operation is an accepted no-op — returns the
  current state, appends no history entry, triggers no side effect. A repeated
  cancel on an already-CANCELLED order is likewise an idempotent no-op (no
  second inventory release). Only genuinely illegal transitions (target that is
  not the legal next state, or any move out of a terminal state) are rejected.
- **Inventory release seam (FR-006, Out of Scope).** A single concrete
  `InventoryReleaseClient` with one method `release(UUID orderId)` whose body
  logs the release and is otherwise a no-op stub for this feature. Deliberately
  **no** interface is extracted (constitution Article V — a second concrete
  implementation does not yet exist) and **no** retry/compensation layer is
  built (explicitly out of scope). The real external HTTP call replaces this
  method body later. `OrderService.cancel` invokes it exactly once, only when
  the order had reached INVENTORY_RESERVED or a later pre-DISPATCHED state.
- **REST surface (FR-009/010/014/015/017).**
  - `POST /api/orders` → create; `201 Created`, body `OrderResponse` (status
    NEW, inventoryReserved false).
  - `POST /api/orders/{orderId}/status` (body `{ "targetStatus": "..." }`) →
    `200 OK` `OrderResponse` when accepted (including an idempotent no-op);
    `400` malformed/out-of-range status or malformed id; `404` unknown order;
    `409` illegal transition, terminal-state update, or lost optimistic-lock
    race.
  - `POST /api/orders/{orderId}/cancel` → `200 OK` when cancelled (or an
    idempotent repeat on an already-CANCELLED order); `404` unknown order;
    `409` cancel at/after DISPATCHED or on CLOSED.
  - `GET /api/orders/{orderId}` → `200 OK` `OrderResponse`; `404` unknown.
  - `GET /api/orders/{orderId}/history` → `200 OK`
    `List<HistoryEntryResponse>` in chronological order; `404` unknown.
  - `ApiExceptionHandler` (`@RestControllerAdvice`) maps domain rejections to
    these codes and returns a generic `ErrorResponse` with no internal detail
    (SEC-07 / FR-017), while the service logs full context server-side.

## Complexity Tracking

> Fill ONLY if the Constitution Check above has a violation that needs
> justifying — leave empty otherwise, don't manufacture content here.

(No constitution violations — this section intentionally empty.)
