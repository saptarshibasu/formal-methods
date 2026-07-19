<!--
  TEMPLATE — optional, one per feature at specs/<NNN-feature-name>/data-model.md.
  Created during Plan (Phase 2) when a feature has non-trivial data. It is the
  CONCEPTUAL model: entities, their meaningful fields, relationships, and the
  rules that govern them. NOT an ORM mapping, not DDL, not migration SQL — those
  are implementation and belong in code. If you find yourself writing
  `@Column` or `CREATE TABLE`, you've gone too low.

  The conceptual entities here should trace back to the Key Entities section of
  spec.md. Delete this file if the feature has no real data model. Once filled
  in, delete this comment block.
-->

# Data Model: [FEATURE NAME]

**Feature**: `specs/[###-feature-name]/` | **Created**: [DATE]

## Entities

### [Entity 1 — e.g. "Order"]

[One line: what it represents in the domain.]

| Field | Meaning | Rules / constraints |
|---|---|---|
| [name] | [what it holds] | [required? unique? allowed values? — conceptual, not column types] |
| [name] | [what it holds] | [e.g. "must be ≥ 0", "set once, never changes"] |

**Identity**: [what makes one instance distinct from another, conceptually.]
**Lifecycle**: [the states it moves through, if any — e.g. Draft → Placed →
Fulfilled → Cancelled. Note which transitions are allowed.]

### [Entity 2]

[Same shape.]

## Relationships

<!-- State cardinality and meaning, not foreign keys. -->

- [Entity A] has [one / many] [Entity B] — [what the relationship means;
  what happens to B when A is deleted, conceptually.]
- [Entity B] belongs to exactly one [Entity A].

## Invariants

<!-- Rules that must ALWAYS hold across the model, regardless of how it's
     stored or which operation runs. These are prime candidates for tests. -->

- [e.g., "An Order's total always equals the sum of its line items."]
- [e.g., "A Cancelled order can never transition back to Placed."]

## Notes for the implementer

<!-- Anything conceptual that constrains implementation without dictating it —
     e.g. "expected to grow unbounded; design for pagination." Keep storage
     choices in plan.md, not here. -->

- [optional]
