# Research: order lifecycle management with formally verified status state machine

**Feature**: `specs/001-order-lifecycle-management-with-formally/` | **Created**: 2026-07-18

## Open Questions Resolved

### How to faithfully simulate "two updates processed concurrently by different service instances" without a multi-instance deployment

- **Why it mattered**: FR-013 / US4 require that of two conflicting concurrent
  updates to the same order, at most one is accepted, with history and current
  state consistent afterward. The plan needs a concrete arbitration mechanism,
  and there is no real multi-instance deployment in this repo (Out of Scope).
- **Finding**: In a real multi-instance deployment the instances would share
  one PostgreSQL database, and the single point that arbitrates two racing
  writers to the same order row is the database itself. JPA optimistic locking
  via a `@Version` column reproduces exactly that arbitration: two concurrent
  transactions read the same version, both attempt to commit, and PostgreSQL +
  Hibernate let exactly one succeed while the other fails with
  `ObjectOptimisticLockingFailureException`. Which JVM/instance the losing
  transaction ran in is irrelevant — the row's version column is the arbiter,
  so N threads against one instance and N requests across N instances are the
  same race against the same DB row.
- **Source**: Spring Data JPA optimistic-locking semantics (`@Version`);
  Hibernate ORM 6 (Spring Boot 3.2.5) version-check-on-flush behavior.
- **Decision**: Order carries a `@Version` column. Each lifecycle mutation runs
  in its own `@Transactional` unit; a lost optimistic-lock race surfaces as
  `ObjectOptimisticLockingFailureException`, which `OrderService` maps to a
  rejection (HTTP 409). Concurrency is exercised in automated tests at the
  repository-mock boundary (the mock throws the optimistic-lock exception to
  drive the rejection path — no real DB, per constitution Article IV); the
  full interleaving property is discharged by the TLA+ model, and real-DB
  arbitration is confirmed by a manual local run against PostgreSQL.

### Split of formal-verification obligations between Lean 4 and TLA+

- **Why it mattered**: The feature carries dual obligations and the plan must
  name exactly one tool per obligation with a concrete artifact target.
- **Finding**: spec.md's Formal Verification Obligations section already fixes
  the split — [US1] sequential transition-function correctness is a pure-
  function property (Lean 4's strength), and [US2/US3/US4] concurrent-
  interleaving safety plus [US4] liveness are temporal properties over all
  interleavings of a concurrent protocol (TLA+/TLC's strength). This confirms,
  rather than re-derives, the candidate split carried in from kickoff.
- **Source**: `spec.md` Formal Verification Obligations; `AGENTS.md` Formal
  Verification Tooling (Lean 4 for pure/data properties, TLA+ for concurrent/
  stateful protocols).
- **Decision**: Lean 4 proves [US1] against `com.formalmethods.domain`'s
  transition table (`Proofs/OrderTransition.lean`); TLA+ model-checks
  [US2/US3/US4] safety + [US4] liveness against the `OrderService` mutation
  protocol (`Specs/OrderLifecycle.tla` + `Specs/OrderLifecycle.cfg`).

## Alternatives Investigated

| Option | Considered for | Rejected because |
|---|---|---|
| Real message broker / queue (Kafka, RabbitMQ, SQS) | Simulating upstream at-least-once delivery of status updates | Triggering prompt explicitly scopes this out: "Upstream delivery semantics are simulated at the API boundary — no message broker or queue infrastructure in this feature." Duplicated/delayed/out-of-order/concurrent delivery must instead be exercised through the synchronous `apply status update` API itself (e.g. repeat calls, out-of-order calls, concurrent calls from test threads/instances), not through real broker infrastructure. |

## Still Open (carried into implementation)

- (Both prior items — concurrency simulation mechanism and the TLA+/Lean 4
  split — are now resolved above under Open Questions Resolved.)

## Implementation Notes (from triggering prompt)

- SQL scripts (schema DDL for orders/status-history tables) go under
  `resources/scripts` (i.e. `src/main/resources/scripts/`, following this
  project's existing `src/main/resources/` convention — no schema-migration
  tool is in use per `AGENTS.md`, so these are the manually-applied scripts a
  developer runs against their local PostgreSQL instance).

## Pending Amendments

(none)
