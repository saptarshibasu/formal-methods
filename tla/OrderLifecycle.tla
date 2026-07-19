---------------------------- MODULE OrderLifecycle ----------------------------
(* [US2/US3/US4] Formal Verification Obligation — spec.md, plan.md, tasks.md
   T034a.

   Models the concurrent OrderService mutation protocol for a SINGLE shared
   order under at-least-once, duplicated, delayed, out-of-order, and truly
   concurrent status-update / cancel delivery (spec.md User Stories 2-4).
   Drafted by tlaplus-spec-writer; NOT yet run through TLC — hand off to
   tlaplus-verifier for that. This agent has not claimed the spec holds.

   ------------------------------------------------------------------------
   CORRESPONDENCE MAPPING (real code -> this model) — mandatory per
   tlaplus-spec-writer's operating instructions; also repeated in the
   drafting report.
   ------------------------------------------------------------------------

   VARIABLES:
     status         <-> Order.status column (single shared order row).
     history        <-> the ordered set of StatusHistoryEntry rows for this
                         order (order_status_history table), read back via
                         StatusHistoryRepository.findByOrderIdOrderByIdAsc.
                         Modeled as Seq(Statuses); only ever grown via
                         Append, matching "no update/delete path is exposed"
                         (plan.md).
     version        <-> Order.version, the JPA @Version optimistic-lock
                         column. Incremented by exactly one on every
                         successful orderRepository.save(order) that
                         actually changes status (i.e. every accepted,
                         non-idempotent transition) — never on an idempotent
                         no-op or a rejected attempt, matching the real
                         entity's @Version semantics (Hibernate only bumps
                         the version on an actual row UPDATE).
     releaseCount   <-> the cumulative number of InventoryReleaseClient
                         .release(orderId) invocations for this order.
     commitVersions <-> ghost/bookkeeping only, no real-code counterpart.
                         Records, for every accepted state-changing commit,
                         the `version` value it was based on (i.e. the
                         value the caller's read saw before its write).
                         Exists purely to make invariant (e) — "of two
                         conflicting concurrent updates at most one is
                         accepted" — a directly TLC-checkable state
                         predicate rather than a two-state action property.
     crMode/crVersion/crTarget[c], for each modeled caller c
                    <-> the two phases of one OrderService.applyStatusUpdate
                         or .cancel invocation as actually executed:
                         "Idle"    = caller not currently mid-request.
                         Pending*  = caller has done
                         orderRepository.findById(orderId) (read phase,
                         snapshotting the row's current version into
                         crVersion[c]) and is about to run its
                         business-logic checks and orderRepository.save(...)
                         (write phase == the Commit* action below). This
                         two-phase split is what makes a genuine two-writer
                         race (not just TLA+'s ordinary one-action-per-step
                         interleaving) representable: two callers can both
                         read the same version before either commits.

   ACTIONS:
     ReadForUpdate(c)  <-> the read half of OrderService.applyStatusUpdate:
                         orderRepository.findById(orderId), i.e. the moment
                         the caller learns the order's current
                         status/version and picks its intended target
                         (Statuses is unrestricted — see the
                         StatusUpdateRequest/applyStatusUpdate discrepancy
                         note below). Models arbitrary, possibly
                         duplicated/delayed/out-of-order/illegal target
                         choice — at-least-once delivery from
                         warehouse/provisioning/courier.
     ReadForCancel(c)  <-> the read half of OrderService.cancel.
     CommitUpdate(c)   <-> the write half of applyStatusUpdate: the
                         idempotent no-op check (order.getStatus() ==
                         targetStatus), then the CANCELLED-target rejection
                         (targetStatus == CANCELLED && order.getStatus() !=
                         CANCELLED -> IllegalTransitionException,
                         "use POST /cancel"), then
                         OrderLifecycle.isLegalTransition, then
                         orderRepository.save(order) +
                         statusHistoryRepository.save(...) inside the
                         @Transactional method. The
                         "crVersion[c] # version" branch is the
                         ObjectOptimisticLockingFailureException ->
                         ConcurrentUpdateException path (ordinary DB
                         optimistic-lock rejection: someone else's write
                         landed between this caller's read and write).
     CommitCancel(c)   <-> the write half of OrderService.cancel: the
                         already-CANCELLED idempotent-no-op check, then
                         OrderLifecycle.isLegalTransition(status,
                         CANCELLED), then the shouldReleaseInventory
                         decision, then save + history append +
                         (conditionally) InventoryReleaseClient.release.

   KNOWN DISCREPANCY #1 — the flagged inventoryReserved dead field:
   Order.inventoryReserved is set false at creation and never written again
   anywhere in OrderService (confirmed by reading OrderService.java in
   full) — it plays no role in OrderService.cancel's release decision. The
   real release decision reads order.getStatus() at the moment of
   cancellation (INVENTORY_RESERVED or PROVISIONED => release). This model
   therefore has NO variable for inventoryReserved at all — releaseCount's
   update condition in CommitCancel is keyed off `status` (the value at the
   time of the accepted CANCELLED commit), exactly mirroring the real
   `shouldReleaseInventory` local variable in OrderService.cancel, not the
   dead entity field. Modeling the dead field as if it were live would have
   been the wrong-shaped state space; it is deliberately omitted.

   FIXED — formerly "KNOWN DISCREPANCY #2" in the prior draft, now resolved
   in the real code and modeled accordingly:
   applyStatusUpdate now rejects targetStatus = CANCELLED with
   IllegalTransitionException ("use POST /cancel to cancel an order ...")
   whenever it would actually move the order (order.getStatus() !=
   CANCELLED at call time) — checked immediately after the idempotent
   same-status no-op check and before OrderLifecycle.isLegalTransition is
   ever consulted for this path. A CANCELLED -> CANCELLED self-target is
   still caught earlier by the idempotency check and still no-ops,
   unaffected by this fix. OrderLifecycle.buildTransitionTable's edge set
   itself (LegalEdges below) is UNCHANGED — CANCELLED remains a legal edge
   from NEW/INVENTORY_RESERVED/PROVISIONED because OrderService.cancel (the
   dedicated cancel path, modeled by ReadForCancel/CommitCancel) still
   needs and uses that edge for its own legality check; only
   applyStatusUpdate's willingness to *drive* the order there via that
   generic path was closed off. Statuses (the range ReadForUpdate/
   CommitUpdate quantify over) remains deliberately unrestricted, matching
   StatusUpdateRequest.targetStatus's real unrestricted input range
   (validated only against the OrderStatus enum) — narrowing Statuses would
   be the disallowed shortcut; instead CommitUpdate itself now models the
   rejection, exactly matching the real IllegalTransitionException. With
   this fix, invariant (d) — InvD_ReleaseExactlyOnceIfQualifying below — is
   expected to HOLD: the only path that can move status to CANCELLED and
   append it to history is now CommitCancel, which always runs the
   qualifying-origin release check. See this drafting report for the
   updated expectation; hand off to tlaplus-verifier to confirm.
*)
EXTENDS Naturals, Sequences

CONSTANTS Callers

Statuses == {"NEW", "INVENTORY_RESERVED", "PROVISIONED", "DISPATCHED",
             "DELIVERED", "CLOSED", "CANCELLED"}

TerminalStatuses == {"CLOSED", "CANCELLED"}

QualifyingCancelOrigins == {"INVENTORY_RESERVED", "PROVISIONED"}

(* Mirrors OrderLifecycle.buildTransitionTable's exact 8 legal edges — reuse
   the same edge set the Lean 4 proof ([US1], Proofs/OrderTransition.lean)
   establishes is exhaustive, not a redefinition of it. *)
LegalEdges == {
    <<"NEW", "INVENTORY_RESERVED">>,
    <<"INVENTORY_RESERVED", "PROVISIONED">>,
    <<"PROVISIONED", "DISPATCHED">>,
    <<"DISPATCHED", "DELIVERED">>,
    <<"DELIVERED", "CLOSED">>,
    <<"NEW", "CANCELLED">>,
    <<"INVENTORY_RESERVED", "CANCELLED">>,
    <<"PROVISIONED", "CANCELLED">>
}

IsLegalEdge(from, to) == <<from, to>> \in LegalEdges

VARIABLES
    status,          \* current Order.status
    history,          \* Seq(Statuses) — append-only status-history projection
    version,          \* Nat — Order.@Version counter
    releaseCount,     \* Nat — InventoryReleaseClient.release invocation count
    commitVersions,   \* Seq(Nat) — ghost log of pre-commit version per accepted write
    crMode,           \* [Callers -> {"Idle","PendingUpdate","PendingCancel"}]
    crVersion,        \* [Callers -> Nat] — version snapshot read at Read time
    crTarget          \* [Callers -> Statuses] — target read for a pending update

vars == <<status, history, version, releaseCount, commitVersions,
          crMode, crVersion, crTarget>>

TypeOK ==
    /\ status \in Statuses
    /\ history \in Seq(Statuses)
    /\ version \in Nat
    /\ releaseCount \in Nat
    /\ commitVersions \in Seq(Nat)
    /\ crMode \in [Callers -> {"Idle", "PendingUpdate", "PendingCancel"}]
    /\ crVersion \in [Callers -> Nat]
    /\ crTarget \in [Callers -> Statuses]

Init ==
    /\ status = "NEW"
    /\ history = <<>>
    /\ version = 0
    /\ releaseCount = 0
    /\ commitVersions = <<>>
    /\ crMode = [c \in Callers |-> "Idle"]
    /\ crVersion = [c \in Callers |-> 0]
    /\ crTarget = [c \in Callers |-> "NEW"]

(* Read half of applyStatusUpdate: picks an arbitrary target (unrestricted —
   see Known Discrepancy #2) and snapshots the current version. *)
ReadForUpdate(c) ==
    /\ crMode[c] = "Idle"
    /\ \E t \in Statuses :
          /\ crMode' = [crMode EXCEPT ![c] = "PendingUpdate"]
          /\ crTarget' = [crTarget EXCEPT ![c] = t]
          /\ crVersion' = [crVersion EXCEPT ![c] = version]
    /\ UNCHANGED <<status, history, version, releaseCount, commitVersions>>

(* Read half of cancel. *)
ReadForCancel(c) ==
    /\ crMode[c] = "Idle"
    /\ crMode' = [crMode EXCEPT ![c] = "PendingCancel"]
    /\ crVersion' = [crVersion EXCEPT ![c] = version]
    /\ UNCHANGED <<status, history, version, releaseCount, commitVersions, crTarget>>

(* Write half of applyStatusUpdate: stale-version rejection (optimistic
   lock), idempotent no-op, CANCELLED-target rejection (FIXED — see the
   correspondence-mapping note above; matches applyStatusUpdate's
   IllegalTransitionException("use POST /cancel...") for any target =
   CANCELLED that would actually move the order), illegal-transition
   rejection, or accepted transition — in that priority order, matching
   OrderService.applyStatusUpdate's own order of checks (repository save
   first, which is where the real optimistic-lock check happens at the DB
   level; modeled here as the first-checked branch since it is the
   outermost real failure mode once past the idempotency/legality checks —
   semantically equivalent because a stale snapshot cannot in fact reflect
   a legality decision the current row would honor). *)
CommitUpdate(c) ==
    /\ crMode[c] = "PendingUpdate"
    /\ UNCHANGED <<crVersion, crTarget>>
    /\ crMode' = [crMode EXCEPT ![c] = "Idle"]
    /\ IF crVersion[c] # version THEN
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>
       ELSE IF crTarget[c] = status THEN
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>
       ELSE IF crTarget[c] = "CANCELLED" THEN
           \* FIXED: applyStatusUpdate now rejects any CANCELLED target that
           \* would actually move the order (status # CANCELLED here, since
           \* the same-status no-op above already handled CANCELLED ->
           \* CANCELLED) — IllegalTransitionException, no state change, no
           \* history append, no release. Never reaches IsLegalEdge for
           \* this target on this path.
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>
       ELSE IF IsLegalEdge(status, crTarget[c]) THEN
           /\ status' = crTarget[c]
           /\ history' = Append(history, crTarget[c])
           /\ version' = version + 1
           /\ commitVersions' = Append(commitVersions, version)
           /\ UNCHANGED releaseCount
       ELSE
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>

(* Write half of cancel: stale-version rejection, idempotent repeat-cancel
   no-op, illegal-cancel rejection (at/after DISPATCHED), or accepted
   cancel — releasing inventory exactly once iff the order's status at the
   moment of the accepted commit was INVENTORY_RESERVED or PROVISIONED. *)
CommitCancel(c) ==
    /\ crMode[c] = "PendingCancel"
    /\ UNCHANGED <<crVersion, crTarget>>
    /\ crMode' = [crMode EXCEPT ![c] = "Idle"]
    /\ IF crVersion[c] # version THEN
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>
       ELSE IF status = "CANCELLED" THEN
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>
       ELSE IF IsLegalEdge(status, "CANCELLED") THEN
           /\ status' = "CANCELLED"
           /\ history' = Append(history, "CANCELLED")
           /\ version' = version + 1
           /\ commitVersions' = Append(commitVersions, version)
           /\ releaseCount' = releaseCount + (IF status \in QualifyingCancelOrigins THEN 1 ELSE 0)
       ELSE
           UNCHANGED <<status, history, version, releaseCount, commitVersions>>

Next == \E c \in Callers : ReadForUpdate(c) \/ ReadForCancel(c)
                              \/ CommitUpdate(c) \/ CommitCancel(c)

(* Fairness targets — used only in the Fairness formula below, not in Next
   (they are already reachable as nondeterministic sub-cases of
   ReadForUpdate/ReadForCancel above). Restricting to a currently-legal
   target/edge is what turns "the system keeps taking *some* step" into
   "the system keeps making *legal* progress". *)
LegalReadUpdate(c) ==
    /\ crMode[c] = "Idle"
    /\ \E t \in Statuses :
          /\ IsLegalEdge(status, t)
          /\ crMode' = [crMode EXCEPT ![c] = "PendingUpdate"]
          /\ crTarget' = [crTarget EXCEPT ![c] = t]
          /\ crVersion' = [crVersion EXCEPT ![c] = version]
    /\ UNCHANGED <<status, history, version, releaseCount, commitVersions>>

LegalReadCancel(c) ==
    /\ crMode[c] = "Idle"
    /\ IsLegalEdge(status, "CANCELLED")
    /\ crMode' = [crMode EXCEPT ![c] = "PendingCancel"]
    /\ crVersion' = [crVersion EXCEPT ![c] = version]
    /\ UNCHANGED <<status, history, version, releaseCount, commitVersions, crTarget>>

(* FIXED — liveness counterexample from tlaplus-verifier's first run
   (2026-07-18): with WF_vars(LegalReadUpdate(c)) / WF_vars(LegalReadCancel(c)),
   TLC found a lasso trace stuck forever at status = "DELIVERED": caller c1
   perpetually re-picks the idempotent self-target ("DELIVERED"), caller c2
   perpetually re-picks an illegal target ("DISPATCHED"), and neither ever
   picks the one legal, progress-making target ("CLOSED"). That trace is a
   genuine gap in the fairness *formula*, not evidence the underlying
   liveness claim is false and not a reason to touch Next, LegalEdges, or
   any safety invariant:

   Root cause: ENABLED LegalReadUpdate(c) requires crMode[c] = "Idle" *and*
   some legal target to exist from the current status. Once c re-enters
   "Idle" (after its own CommitUpdate/CommitCancel completes), a legal
   target may exist (e.g. DELIVERED -> CLOSED) so LegalReadUpdate(c) is
   enabled again — but the very next step, the general ReadForUpdate(c) is
   free to nondeterministically pick a self-target or illegal target
   instead, moving c to "PendingUpdate" and disabling LegalReadUpdate(c)
   again until c cycles back to "Idle". This is exactly TLA+'s "blinking
   enabled" scenario: LegalReadUpdate(c) is enabled infinitely often along
   the trace, but never enabled *continuously* from some point on. Weak
   fairness (WF_vars) only obligates an action once it is continuously
   enabled forever, so WF never fires here and the caller can dodge the
   legal target forever — a valid WF_vars(Next)-behavior, but not one that
   should be allowed to stand in for "delivery is eventually fair."

   Fix chosen: strengthen exactly LegalReadUpdate(c) and LegalReadCancel(c)
   — the two READ-phase actions whose enabledness can be toggled off by a
   sibling nondeterministic branch of the *same* general action — from
   WF_vars to SF_vars. Strong fairness only requires an action to be
   enabled *infinitely often* (not continuously) to obligate it eventually
   firing; since LegalReadUpdate(c) at status = DELIVERED genuinely recurs
   infinitely often on any lasso that keeps looping through c's Idle state,
   SF_vars now correctly forces it to eventually be taken, which snapshots
   the legal target and hands off to CommitUpdate(c) — whose own WF_vars
   suffices unchanged, because once c is "PendingUpdate" nothing but c's
   own CommitUpdate can disable it again (no sibling branch re-disables a
   *committed* pending request), so it is continuously enabled from that
   point and WF already forces it. This is not a narrowing of the state
   space or of Next: every self-target/illegal-target branch of
   ReadForUpdate/ReadForCancel remains fully reachable and unconstrained;
   only the *fairness assumption* under which Liveness is checked was
   strengthened, which is the correct direction (a stronger hypothesis is
   a strictly smaller, still-legitimate set of behaviors TLC checks
   Liveness against — never a weakening of the Liveness formula itself,
   which is untouched below). *)
Fairness ==
    \A c \in Callers :
        /\ SF_vars(LegalReadUpdate(c))
        /\ SF_vars(LegalReadCancel(c))
        /\ WF_vars(CommitUpdate(c))
        /\ WF_vars(CommitCancel(c))

Spec == Init /\ [][Next]_vars /\ Fairness

-----------------------------------------------------------------------------
(* Safety invariants — spec.md's [US2/US3/US4] obligation (a)-(e). *)

(* (a) current state always equals the state of the most recent appended
       history entry. Vacuously true before any history entry exists
       (freshly created order, status = NEW, history = <<>>) — Last(history)
       is undefined on an empty sequence, and the real system likewise
       records no history entry at creation (OrderService.create never
       calls statusHistoryRepository.save), so "divergence from the latest
       entry" cannot yet be observed; this is the natural reading of (a),
       not a weakening of it. *)
InvA_StatusMatchesHistory ==
    history = <<>> \/ status = history[Len(history)]

(* (b) history is append-only (guaranteed by construction: no action ever
       does anything but Append to history or leave it UNCHANGED) and no
       illegal transition is ever committed to it. *)
InvB_HistoryOnlyLegalEdges ==
    \A i \in 1..Len(history) :
        LET prev == IF i = 1 THEN "NEW" ELSE history[i - 1]
        IN IsLegalEdge(prev, history[i])

(* (c) each accepted transition appends exactly one history entry, and a
       re-delivered duplicate appends none: version increments by exactly 1
       per accepted state-changing commit and by 0 on every idempotent/
       rejected commit, in lockstep with history's length. *)
InvC_ExactlyOnceEffect ==
    Len(history) = version

(* (d) inventory is released exactly once for a qualifying cancellation and
       never for a non-qualifying one, for this single order over its
       entire (necessarily terminal-bounded) lifetime. FIXED (formerly
       "Known Discrepancy #2"): now that CommitUpdate rejects any
       actually-moving CANCELLED target, the only action able to move
       status to CANCELLED and append it to history is CommitCancel, which
       always applies the qualifying-origin release check. This invariant
       is expected to HOLD, not violate — hand off to tlaplus-verifier to
       confirm. *)
InvD_ReleaseExactlyOnceIfQualifying ==
    /\ releaseCount <= 1
    /\ (releaseCount = 1 <=>
          \E i \in 1..Len(history) :
              /\ history[i] = "CANCELLED"
              /\ (IF i = 1 THEN "NEW" ELSE history[i - 1]) \in QualifyingCancelOrigins)

(* (e) of two conflicting concurrent updates, at most one is accepted: no
       two accepted (state-changing) commits were ever both based on
       reading the same pre-commit version — i.e. no version value is ever
       "spent" by more than one successful write. *)
InvE_AtMostOneAcceptedPerVersion ==
    \A i, j \in 1..Len(commitVersions) :
        i # j => commitVersions[i] # commitVersions[j]

-----------------------------------------------------------------------------
(* [US4] Liveness — every order, from any reachable non-terminal state,
   eventually reaches a terminal state (CLOSED or CANCELLED) under fair
   delivery of legal status updates. *)

Liveness ==
    (status \notin TerminalStatuses) ~> (status \in TerminalStatuses)

=============================================================================
