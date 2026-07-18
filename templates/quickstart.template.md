<!--
  TEMPLATE — optional, one per feature at specs/<NNN-feature-name>/quickstart.md.
  A short, concrete list of validation scenarios for fast smoke-testing this
  feature DURING implementation — the "is the happy path alive yet?" check you
  run by hand at each checkpoint. It is not the test suite (that's automated,
  per tasks.md) and not the full acceptance criteria (those are in spec.md);
  it's the fast manual sanity pass.

  Each scenario should map to a real user story in spec.md. Delete this file if
  the feature is too small to warrant one. Once filled in, delete this comment.
-->

# Quickstart: [FEATURE NAME]

**Feature**: `specs/[###-feature-name]/` | **Created**: [DATE]

## Prerequisites

<!-- The minimum to exercise the feature locally. Reference AGENTS.md's Run
     command rather than repeating it if it's already documented there. -->

- [e.g., "App running locally — see AGENTS.md 'Run locally'."]
- [e.g., "Seed data loaded: `[command]`."]

## Smoke Scenarios

<!-- Numbered, in the order you'd actually run them. Each: what to do, what you
     should see. Keep them fast — this is a 2-minute confidence check. -->

### 1. [Happy path for User Story 1] (US1)

1. [Action — e.g. "POST /orders with a valid payload."]
2. **Expect**: [observable result — e.g. "201 with an order id; order visible in GET /orders."]

### 2. [Key path for User Story 2] (US2)

1. [Action]
2. **Expect**: [observable result]

### 3. [One important failure path]

1. [Action — e.g. "POST /orders with an empty cart."]
2. **Expect**: [graceful, specified error — e.g. "422 with a validation message, no order created."]

## Done when

<!-- The bar for "this feature is demonstrably working," in plain terms. Should
     line up with the spec's Success Criteria. -->

- [ ] All smoke scenarios above pass by hand.
- [ ] [Any feature-specific signal — e.g. "logs show the expected events."]
