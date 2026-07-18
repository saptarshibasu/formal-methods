/-
  Proofs/OrderTransition.lean

  [US1] Formal Verification Obligation (specs/001-order-lifecycle-management-with-formally/spec.md):
  the order-lifecycle transition relation admits EXACTLY the 8 legal edges
  (5 forward: NEW -> INVENTORY_RESERVED -> PROVISIONED -> DISPATCHED ->
  DELIVERED -> CLOSED, plus 3 cancel edges {NEW, INVENTORY_RESERVED,
  PROVISIONED} -> CANCELLED) and no others, established exhaustively over
  all 7x7 state pairs rather than sampled; CLOSED and CANCELLED have no
  outgoing transitions; no cancel edge exists from DISPATCHED, DELIVERED,
  CLOSED, or CANCELLED.

  Model target (per plan.md): com.formalmethods.domain.OrderLifecycle,
  specifically its static method
  `OrderLifecycle.isLegalTransition(OrderStatus from, OrderStatus to)`.

  This file uses only Lean 4's core Prelude (no Mathlib, no Lake project)
  per AGENTS.md's Formal Verification Tooling section — check with
  `lean Proofs/OrderTransition.lean`. All theorems below are closed,
  fully-decidable propositions over a 7-constructor finite inductive type
  and are discharged by the `decide` tactic, which performs the exhaustive
  case analysis itself rather than sampling representative cases.
-/

/-- Mirrors `com.formalmethods.domain.OrderStatus` — the same 7 constructors,
    in the same declared order, as the Java enum. -/
inductive OrderStatus
  | NEW
  | INVENTORY_RESERVED
  | PROVISIONED
  | DISPATCHED
  | DELIVERED
  | CLOSED
  | CANCELLED
  deriving DecidableEq, Repr

open OrderStatus

/-- Every state, in the same order as `OrderStatus.values()` would return
    (declaration order of the Java enum / this inductive's constructors). -/
def allStates : List OrderStatus :=
  [NEW, INVENTORY_RESERVED, PROVISIONED, DISPATCHED, DELIVERED, CLOSED, CANCELLED]

/--
  Mirrors `OrderLifecycle.isLegalTransition` / the `EnumMap<OrderStatus,
  EnumSet<OrderStatus>>` built by `buildTransitionTable()`. Each `true`
  case below corresponds one-for-one to one `.add(...)` call in the Java
  method; every pair not listed falls to the catch-all `false`, mirroring
  every `OrderStatus` being initialized to `EnumSet.noneOf(OrderStatus.class)`
  (i.e. "no transitions" is the default, and only the 8 explicit `.add`
  calls carve out exceptions to that default). See the correspondence
  mapping in the accompanying report for the full line-by-line table.
-/
def isLegal : OrderStatus → OrderStatus → Bool
  | NEW, INVENTORY_RESERVED => true
  | NEW, CANCELLED => true
  | INVENTORY_RESERVED, PROVISIONED => true
  | INVENTORY_RESERVED, CANCELLED => true
  | PROVISIONED, DISPATCHED => true
  | PROVISIONED, CANCELLED => true
  | DISPATCHED, DELIVERED => true
  | DELIVERED, CLOSED => true
  | _, _ => false

/-- All 49 ordered state pairs, outer loop over `from`, inner loop over `to` —
    mirrors the nested `for (OrderStatus from : allStates) for (OrderStatus
    to : allStates)` double loop in `OrderLifecycleTest
    .relationAdmitsExactlyTheEightLegalEdgesAndNoOthers`. -/
def allPairs : List (OrderStatus × OrderStatus) :=
  allStates.flatMap (fun f => allStates.map (fun t => (f, t)))

/-- **Main [US1] theorem**: filtering all 49 state pairs down to the ones
    `isLegal` admits yields exactly these 8 pairs, in exactly this order —
    established over the full 7x7 space (`allPairs` has 49 elements), not
    sampled. This single equality simultaneously proves both directions of
    "exactly these 8 edges and no others": every pair in the right-hand list
    is legal (soundness) and no pair outside it is (completeness), since
    `List.filter` over the complete 49-pair enumeration cannot omit or admit
    an edge without the equality failing. -/
theorem legal_edges_eq_exactly_eight :
    allPairs.filter (fun p => isLegal p.1 p.2) =
      [ (NEW, INVENTORY_RESERVED), (NEW, CANCELLED)
      , (INVENTORY_RESERVED, PROVISIONED), (INVENTORY_RESERVED, CANCELLED)
      , (PROVISIONED, DISPATCHED), (PROVISIONED, CANCELLED)
      , (DISPATCHED, DELIVERED)
      , (DELIVERED, CLOSED)
      ] := by decide

/-- Corollary: the legal relation has exactly 8 edges (the cardinality half
    of the [US1] obligation), read off `legal_edges_eq_exactly_eight`. -/
theorem legal_edge_count_is_eight :
    (allPairs.filter (fun p => isLegal p.1 p.2)).length = 8 := by
  rw [legal_edges_eq_exactly_eight]
  rfl

/-- [US1] clause (b): CLOSED is terminal — no outgoing edge of any kind,
    checked against every one of the 7 states. -/
theorem closed_has_no_outgoing_transitions :
    allStates.filter (fun t => isLegal CLOSED t) = [] := by decide

/-- [US1] clause (b): CANCELLED is terminal — no outgoing edge of any kind,
    checked against every one of the 7 states. -/
theorem cancelled_has_no_outgoing_transitions :
    allStates.filter (fun t => isLegal CANCELLED t) = [] := by decide

/-- [US1]: no cancel edge exists from DISPATCHED, DELIVERED, CLOSED, or
    CANCELLED — cancellation is legal only strictly before DISPATCHED. -/
theorem no_cancel_edge_from_dispatched_or_later_or_terminal :
    [DISPATCHED, DELIVERED, CLOSED, CANCELLED].filter (fun f => isLegal f CANCELLED) = [] := by
  decide
