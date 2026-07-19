# Feature Specification: order lifecycle management with formally verified status state machine

**Branch**: `001-order-lifecycle-management-with-formally` | **Created**: 2026-07-18 | **Status**: Approved — sapb2004@gmail.com, 2026-07-18

**Problem Statement**: The service needs to manage orders through a fixed,
irreversible lifecycle where correctness is safety-critical: an order must
never skip, repeat, or reverse a lifecycle stage, and its recorded audit
history must never disagree with its current state — even when the systems
that drive those state changes (warehouse, provisioning, courier) deliver
their updates duplicated, delayed, out of order, or simultaneously. A wrong
transition (e.g. dispatching a cancelled order, or losing an inventory release
on cancellation) has real operational and financial cost, and such faults hide
in rare timing interleavings that ordinary testing cannot exhaust. This feature
delivers that ordered, auditable, concurrency-safe lifecycle and uses it as the
showcase for the project's formal-verification harness, formally guaranteeing
the properties that matter most rather than merely sampling them with tests.

## User Stories & Testing *(mandatory)*

### User Story 1 — Drive an order through its valid lifecycle (Priority: P1)

An operator (or upstream system) creates an order and advances it through the
permitted sequence NEW → INVENTORY_RESERVED → PROVISIONED → DISPATCHED →
DELIVERED → CLOSED, one legal step at a time, and can read the order's current
state at any point. Any attempt to move to a state that is not the legal next
state for the order's current state is rejected, and the order's state is left
unchanged.

**Why this priority**: This is the core of the feature — the ordered state
machine and the guarantee that out-of-order transitions never occur. Without
it there is nothing to audit, cancel, or verify. It is the shippable MVP.

**Independent Test**: Create an order, apply each valid update in order, and
confirm the current state advances correctly; then attempt an illegal jump
(e.g. NEW → DISPATCHED) and confirm it is rejected and the state is unchanged.

**Acceptance Scenarios**:
1. **Given** a newly created order in state NEW, **When** an INVENTORY_RESERVED
   update is applied, **Then** the current state becomes INVENTORY_RESERVED.
2. **Given** an order in state NEW, **When** a DISPATCHED update is applied,
   **Then** the update is rejected as an illegal transition and the state
   remains NEW.
3. **Given** an order in state DELIVERED, **When** a CLOSED update is applied,
   **Then** the current state becomes CLOSED.
4. **Given** an order in a terminal state (CLOSED or CANCELLED), **When** a
   status update **to a different target state** is applied, **Then** the
   update is rejected and the state remains terminal. (A self-target duplicate
   on a terminal order — e.g. CLOSED→CLOSED — is not a transition and is
   governed by FR-011's idempotent no-op rule, not this scenario.)
5. **Given** any existing order, **When** its current state is requested,
   **Then** the service returns the order's single current lifecycle state.

### User Story 2 — Cancel an order with inventory release (Priority: P1)

A customer or operator cancels an order. Cancellation is permitted at any point
strictly before DISPATCHED and moves the order to the terminal CANCELLED state.
If inventory had already been reserved (the order had reached
INVENTORY_RESERVED or beyond, but not yet DISPATCHED), the reserved inventory
is released as part of the cancellation. Cancellation at or after DISPATCHED is
rejected.

**Why this priority**: The cancel branch is an explicit, safety-relevant part
of the fixed lifecycle (inventory must not stay reserved on a cancelled order),
and it is one side of the concurrency race the feature must handle. It is
required for a correct first release.

**Independent Test**: Cancel an order in NEW (expect CANCELLED, no release);
cancel an order in INVENTORY_RESERVED (expect CANCELLED and exactly one
inventory release); attempt to cancel an order in DISPATCHED (expect rejection,
state unchanged).

**Acceptance Scenarios**:
1. **Given** an order in state NEW, **When** it is cancelled, **Then** the
   current state becomes CANCELLED and no inventory release occurs.
2. **Given** an order in state INVENTORY_RESERVED (or PROVISIONED), **When** it
   is cancelled, **Then** the current state becomes CANCELLED and the reserved
   inventory is released exactly once.
3. **Given** an order in state DISPATCHED, **When** cancellation is requested,
   **Then** the request is rejected and the state remains DISPATCHED.
4. **Given** an order already in state CANCELLED, **When** cancellation is
   requested again, **Then** the state remains CANCELLED and no additional
   inventory release occurs.

### User Story 3 — Append-only audit history that always agrees with current state (Priority: P1)

Every accepted status change is recorded as an entry in an append-only history
for that order, and the history can be read back in chronological order. The
current state of the order is always consistent with the most recent history
entry, and a rejected update produces no history entry. This consistency holds
even if the service crashes or a request is retried mid-change.

**Why this priority**: Auditability that can silently diverge from reality is
worse than none. The "history never disagrees with current status, even across
a crash or retry" guarantee is a headline requirement of the prompt and a P1
correctness property.

**Independent Test**: Apply a sequence of valid updates and confirm each
produces exactly one history entry whose latest matches the current state;
apply a rejected update and confirm no entry is added; simulate a
retry/duplicate of a just-applied update and confirm history and current state
remain consistent (no divergence, no duplicate effect).

**Acceptance Scenarios**:
1. **Given** an order, **When** a valid status update is accepted, **Then** a
   new history entry is appended and the current state equals that entry's
   state.
2. **Given** an order, **When** a status update is rejected as illegal, **Then**
   no history entry is appended and the prior current state is retained.
3. **Given** a status update that was already applied, **When** the same update
   is re-delivered (retry/duplicate), **Then** the current state and history
   remain exactly as after the first application — no second entry, no second
   effect.
4. **Given** an order's history is requested, **When** the request is served,
   **Then** entries are returned in chronological order and the last entry's
   state equals the order's current state.

### User Story 4 — Correct behavior under at-least-once, out-of-order, and concurrent delivery (Priority: P2)

Status updates originate from upstream systems (warehouse, provisioning,
courier) with at-least-once delivery. The service behaves correctly when
updates arrive duplicated, delayed, or out of order, and when two updates for
the same order are processed concurrently by different service instances (for
example a courier DISPATCHED callback racing a customer cancel). At most one of
two conflicting concurrent updates is accepted; the other is rejected or
absorbed without corrupting state or history. Upstream delivery semantics are
exercised through the status-update API directly, not through any message
broker or queue.

**Why this priority**: This is the hardest correctness surface and the primary
justification for formal verification, but it builds on the state machine,
cancel branch, and audit history established in US1–US3, so it is sequenced
after them.

**Independent Test**: Replay a duplicate update and confirm no double effect;
deliver updates out of order and confirm illegal ones are rejected and the
final state is correct; issue two conflicting updates concurrently (e.g.
DISPATCHED vs. cancel) and confirm exactly one is accepted, with history and
current state consistent afterward.

**Acceptance Scenarios**:
1. **Given** an order in INVENTORY_RESERVED, **When** a duplicate
   INVENTORY_RESERVED update is delivered, **Then** it produces no additional
   state change and no additional history entry.
2. **Given** an order in NEW, **When** a PROVISIONED update arrives before the
   INVENTORY_RESERVED update it depends on, **Then** the premature update is
   rejected and the order stays in NEW until the legal next update arrives.
3. **Given** an order in PROVISIONED, **When** a DISPATCHED update and a cancel
   request are processed concurrently by different service instances, **Then**
   exactly one is accepted, the other is rejected or absorbed, and the
   resulting current state and history are mutually consistent and reflect a
   single legal outcome.

### Edge Cases

- A status update naming a target state equal to the current state (no-op
  duplicate) — treated as an already-applied duplicate, not an error advance.
  This holds even when the order is in a terminal state (e.g. CLOSED→CLOSED,
  CANCELLED→CANCELLED): a self-target duplicate never moves the order, so it is
  accepted as an idempotent no-op under FR-011, which takes precedence over US1
  Scenario 4 for this specific case. Scenario 4's rejection continues to govern
  a genuine transition attempt to a *different* target on a terminal order.
- A cancel arriving after the order is already CANCELLED, or after it has
  reached CLOSED — rejected or absorbed, never re-releasing inventory.
- A status update or cancel referencing an order that does not exist — rejected
  with a not-found outcome, no state created.
- A crash between recording a history entry and updating current state (or vice
  versa) — on recovery the two must not disagree; a partially applied change is
  never observable.
- Cancellation calls a separate API on an external inventory system to release
  the reserved inventory; that inventory system's own reliability, retries, or
  compensation on failure of that call are out of scope for this feature (see
  Out of Scope) — this feature's obligation is to invoke the release and
  record that it happened, once, for a qualifying cancellation.
- A malformed or out-of-range status value in an update payload — rejected at
  the boundary as invalid input (see FR-013).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow creating an order, which starts in state NEW.
- **FR-002**: System MUST model each order's lifecycle as the fixed ordered
  sequence NEW → INVENTORY_RESERVED → PROVISIONED → DISPATCHED → DELIVERED →
  CLOSED, plus a CANCELLED branch.
- **FR-003**: System MUST accept a status update only when the requested target
  state is the legal next state for the order's current state, and MUST reject
  any other transition, leaving the order's current state unchanged.
- **FR-004**: System MUST treat CLOSED and CANCELLED as terminal — no status
  update or cancel may move an order out of either state.
- **FR-005**: Users (and upstream systems) MUST be able to cancel an order at
  any point strictly before DISPATCHED, moving it to CANCELLED; a cancel
  requested at or after DISPATCHED MUST be rejected.
- **FR-006**: System MUST invoke a separate inventory-system release API
  exactly once when an order that has reached INVENTORY_RESERVED (or a later
  pre-DISPATCHED state) is cancelled, and MUST NOT invoke it for an order
  cancelled while still in NEW. The inventory system's own handling of that
  call (retries, compensation on its own failure) is out of scope for this
  feature — see Out of Scope.
- **FR-007**: System MUST record every accepted status change as an entry in an
  append-only history for that order; a rejected update MUST NOT create a
  history entry.
- **FR-008**: System MUST guarantee that an order's current state always equals
  the state of the most recent entry in its history, and that this agreement
  holds across a crash or a retried request (no observable partial change).
- **FR-009**: Users MUST be able to read an order's current state.
- **FR-010**: Users MUST be able to read an order's status history in
  chronological order.
- **FR-011**: System MUST be idempotent with respect to duplicate/re-delivered
  status updates — re-applying an update that has already taken effect MUST
  produce no additional state change, no additional history entry, and no
  repeated side effect (e.g. no second inventory release).
- **FR-012**: System MUST behave correctly when updates for an order arrive out
  of order or delayed — a premature update (whose precondition state has not
  been reached) MUST be rejected, and later legal updates MUST still be
  accepted.
- **FR-013**: System MUST reject two conflicting status updates for the same
  order processed concurrently by accepting at most one, so that the order
  never ends in a state produced by an illegal or interleaved transition, and
  its history and current state remain mutually consistent afterward.
- **FR-014**: System MUST validate every status update, cancel, and create
  request at the trust boundary — target status against the allow-list of
  lifecycle states, and order identifier for well-formedness — and reject
  invalid input with a defined rejection outcome before it reaches lifecycle
  logic (SEC-01).
- **FR-015**: System MUST reject a status update or cancel that references a
  non-existent order, without creating any order or history entry.
- **FR-016**: Authentication and authorization are explicitly **out of scope**
  for this feature (see Out of Scope) — every create, status-update, cancel,
  read-state, and read-history action is reachable without a caller identity
  check. This is a deliberate, human-confirmed deviation from the opted-in
  Security Baseline pack's SEC-02 (deny-by-default authorization); it is
  recorded here rather than silently dropped so Plan/Review do not treat it as
  an oversight.
- **FR-017**: On any error or rejected request, System MUST fail closed (deny,
  never fall through to a state change) and return a generic client-facing
  error that does not leak internal details, while recording enough server-side
  context to investigate (SEC-07).
- **FR-018**: System MUST log security- and lifecycle-relevant events —
  accepted transitions, rejected/illegal transitions, input-validation
  rejections, cancellations, and authorization denials — with actor, action,
  target order, and outcome, and without recording secrets or sensitive data
  (SEC-07).

### Non-Functional Requirements

- **Performance**: Every create, status-update, cancel, read-state, and
  read-history operation MUST complete within 1 second.
- **Security**: Authentication and authorization are out of scope for this
  feature (see FR-016 and Out of Scope) — SEC-02 is deliberately not satisfied
  here. All untrusted input is validated at the boundary (SEC-01). Order data
  is non-sensitive operational data with no encryption-at-rest requirement;
  transport security (TLS) and other infrastructure-level protections are an
  operational/deployment concern outside this feature's scope, not a
  requirement it implements (SEC-04 not applicable at this layer). Configuration
  secrets (e.g. database credentials) are supplied externally at runtime, never
  committed or logged (SEC-03). Client-facing errors are generic; diagnostic
  detail and audit events go to server-side logs without secrets or unmasked
  sensitive data (SEC-05, SEC-07).
- **Scalability**: No specific order volume or concurrent-request-rate target
  is required for this feature — the service must remain correct when the
  same order is acted on concurrently by more than one service instance;
  correctness under concurrency, not scale, is the requirement.

### Key Entities *(if this feature involves data)*

- **Order**: A single unit progressing through the lifecycle. Conceptually has
  a stable identity, a single current lifecycle state, and an indication of
  whether inventory is currently reserved on its behalf.
- **Status History Entry**: An append-only record of one accepted change to an
  order's state — conceptually the order identifier, the resulting status, and
  the timestamp of the change; no other fields are required. Entries are never
  modified or deleted.
- **Inventory Reservation**: The conceptual fact that stock is held for an
  order between INVENTORY_RESERVED and either fulfillment or cancellation.
  Ownership of the reservation itself is external to this service — a separate
  inventory system — and this service's obligation is to invoke that system's
  release API exactly once on a qualifying cancellation (see Out of Scope).

## Formal Verification Obligations *(if applicable)*

- **[US1]** — Sequential transition-function correctness: Over every possible
  sequence of status updates applied to a single order, the order can only ever
  occupy a state reachable from its start state (NEW) by a chain of *permitted*
  transitions. Precisely: (a) the transition relation admits exactly the edges
  NEW→INVENTORY_RESERVED→PROVISIONED→DISPATCHED→DELIVERED→CLOSED and the cancel
  edges from {NEW, INVENTORY_RESERVED, PROVISIONED} to CANCELLED, and no others;
  (b) CLOSED and CANCELLED have no outgoing transitions; (c) no cancel edge
  exists from DISPATCHED, DELIVERED, CLOSED, or CANCELLED. An out-of-order
  transition can therefore never be produced by the transition function, for
  any input sequence — this must be established as a property of the transition
  function itself, exhaustively over all states and inputs, not by sampled
  examples.

- **[US2/US3/US4]** — Concurrent/interleaving safety and liveness: Under every
  interleaving of concurrent, duplicated, delayed, and out-of-order status
  updates and cancellations for a given order (including two conflicting
  updates processed simultaneously), all of the following invariants hold in
  every reachable state: (a) the order's current state always equals the state
  of the most recent appended history entry (no divergence, ever); (b) the
  history is append-only and no illegal transition is ever committed to it;
  (c) each accepted transition appends exactly one history entry, and a
  re-delivered duplicate appends none (exactly-once effect per logical
  transition); (d) inventory is released exactly once for a qualifying
  cancellation and never for a non-qualifying one; (e) of two conflicting
  concurrent updates, at most one is accepted. These must be shown to hold over
  all interleavings of the concurrent protocol, not for a sampled subset.

- **[US4]** — Liveness: Every order, from any reachable non-terminal state,
  eventually reaches a terminal state (CLOSED or CANCELLED) under fair
  delivery of legal status updates — the protocol admits no reachable
  non-terminal state from which no terminal state is reachable (no deadlock,
  no permanent stall).

## Success Criteria *(mandatory)*

- **SC-001**: For every one of the enumerated illegal transitions (any target
  that is not the legal next state, any transition out of a terminal state, any
  cancel at or after DISPATCHED), the update is rejected and the order's state
  is unchanged — 100% of illegal transitions rejected, zero illegal
  transitions committed.
- **SC-002**: In an audit of all orders and their histories, the current state
  equals the latest history entry's state for 100% of orders, with zero
  divergences observed across normal operation, retries, and simulated
  crash/recovery.
- **SC-003**: Re-delivering any already-applied update produces zero additional
  history entries and zero additional side effects (e.g. inventory is released
  at most once per cancelled-with-reservation order across any number of
  re-deliveries).
- **SC-004**: For conflicting concurrent updates to the same order, exactly one
  is accepted in 100% of trials, with no observed state/history inconsistency.
- **SC-005**: The [US1] sequential transition property, the [US2/US3/US4]
  concurrency invariants, and the [US4] liveness property are each discharged
  by the formal harness (proof and model-check respectively completing
  successfully), not merely by example-based tests.

## Out of Scope

- Any real message broker, queue, or streaming infrastructure — upstream
  at-least-once delivery is simulated at the API boundary only.
- Standing up multiple physical service instances — concurrent multi-instance
  behavior is simulated, not deployed.
- The lifecycle content beyond status management — e.g. payment, pricing,
  shipping-label generation, customer notifications, or the actual mechanics of
  the warehouse/provisioning/courier systems.
- Editing or deleting history entries, or any lifecycle transition not in the
  fixed sequence and cancel branch (no reopen, no un-cancel, no back-transition).
- Reporting, analytics, or dashboards over order data beyond reading a single
  order's current state and its history.
- The external inventory system itself, and any retry/compensation logic
  around its release API — this feature only invokes that API exactly once on
  a qualifying cancellation; the inventory system's own reliability is a
  dependency, not something this feature builds or guarantees.
- Authentication and authorization — no caller-identity check, role, or
  permission model is implemented for any operation in this feature (see
  FR-016). A deliberate, human-confirmed deviation from the opted-in Security
  Baseline pack's SEC-02.
- Encryption at rest for order/history data — order data is treated as
  non-sensitive operational data; no encryption-at-rest mechanism is
  implemented by this feature.

## Assumptions

- Each order progresses independently; there is no cross-order transition
  dependency, and the lifecycle applies uniformly to every order.
- The lifecycle states and their order are fixed exactly as given in the prompt
  and are not runtime-configurable in this feature.
- "Get status history" returns the full history for one order; pagination and
  filtering are assumed out of scope for v1 unless clarified. [Recorded as an
  assumption; if history can grow large enough to need paging, revisit via
  FR-010.]
- Inventory release is triggered by this service as part of cancellation by
  calling a separate API on an external inventory system exactly once; that
  system's own handling of the call is out of scope (see Out of Scope).
- Delivery semantics (duplicate/delayed/out-of-order/concurrent) are reproduced
  by how the status-update API is called in tests and verification models, not
  by any additional infrastructure the service depends on.

## Spec Completeness Checklist

- [x] No `[NEEDS CLARIFICATION]` markers remain unresolved
- [x] Every requirement is testable and unambiguous
- [x] Success criteria are measurable, not subjective
- [x] Out of Scope section is filled in, not left as a placeholder
- [x] (Bug fix / Track B) Unchanged-behavior regression guards are listed — N/A, greenfield feature
- [x] No speculative "might need" features included
- [x] Spec contains no tech stack, API shapes, or implementation detail
- [x] (If applicable) Formal Verification Obligations state the exact
      property/invariant, not just "must be correct" — and name no tool
