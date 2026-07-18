<!--
  TEMPLATE — one of these per feature, at specs/<NNN-feature-name>/spec.md.

  Hard rules, enforced by convention, not just convenience:
  - WHAT and WHY only. No tech stack, no API shapes, no code structure —
    that goes in plan.md. A spec that survives a framework swap unchanged
    is a spec that was written correctly.
  - Never silently guess at something the prompt didn't specify. Mark it
    `[NEEDS CLARIFICATION: specific question]` and move on — don't let an
    unstated assumption hide three phases deep in implementation.
  - This is a living document. If a decision changes mid-implementation,
    edit this file — don't let it go stale while the "real" answer lives
    only in chat history.

  Once a section below is filled in, delete its instructional comment along
  with it — including this one, once the spec is ready for review. These
  comments guide drafting; they are not part of the spec itself.
-->

# Feature Specification: [FEATURE NAME]

**Branch**: `[###-feature-name]` | **Created**: [DATE] | **Status**: Draft

**Problem Statement**: [What problem are you solving, for whom, and why does
it matter? One paragraph — this is the part that should survive unchanged
even if every technical decision below it changes.]

## User Stories & Testing *(mandatory)*

<!-- Order by priority. Each story must be independently testable — if you
     implement only the P1 story, you still have a shippable MVP. -->

### User Story 1 — [Brief Title] (Priority: P1)

[Plain-language description of this user journey.]

**Why this priority**: [The value, and why it ranks here.]

**Independent Test**: [How this can be verified on its own — e.g. "fully
testable by doing X, delivers Y."]

**Acceptance Scenarios**:
1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

<!-- Alternative acceptance-criteria format (EARS notation) — use this
     instead of Given/When/Then if your team prefers explicit trigger/
     precondition/response grammar; pick one style per project, not both:

     WHEN <trigger>, the <system> SHALL <response>.
     WHILE <precondition>, the <system> SHALL <response>.
     WHILE <precondition>, WHEN <trigger>, the <system> SHALL <response>.

     e.g. "WHEN the user submits an empty order, the system SHALL return a
     422 with a validation error."
-->

### User Story 2 — [Brief Title] (Priority: P2)

[Same shape as above.]

### Edge Cases

- What happens when [boundary condition]?
- How does the system handle [error scenario]?
- [Anything domain-specific you already know is a gotcha — pour your
  expertise in here, don't make the agent rediscover it.]

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [specific, testable capability].
- **FR-002**: System MUST [specific, testable capability].
- **FR-003**: Users MUST be able to [key interaction].

*Mark anything the prompt didn't specify — don't guess:*
- **FR-004**: System MUST authenticate users via [NEEDS CLARIFICATION: auth
  method not specified — email/password, SSO, OAuth?]

### Non-Functional Requirements

- **Performance**: [e.g., p95 latency target, throughput]
- **Security**: [e.g., auth requirements, data classification]
- **Accessibility**: [if applicable]
- **Scalability**: [expected scale, growth assumptions]

### Key Entities *(if this feature involves data)*

- **[Entity 1]**: [What it represents, key attributes — no implementation
  detail, no ORM annotations, just the concept.]
- **[Entity 2]**: [What it represents, relationship to Entity 1.]

## Formal Verification Obligations *(if applicable)*

<!-- Only for a property/invariant that must be formally guaranteed, not just
     tested — e.g. "this protocol never deadlocks," "this balance can never go
     negative under any sequence of operations." State WHAT must be proven or
     model-checked, exactly and completely; never name a tool here (Lean 4 vs
     TLA+ is a HOW decision — that's plan.md's job). Delete this section if no
     story has such an obligation. -->

- **[US1]**: [The exact property/invariant that must hold, stated precisely
  enough that a proof of the wrong claim would be obviously wrong — not
  "the system must be correct."]

## Success Criteria *(mandatory)*

<!-- Measurable, technology-agnostic. "Fast" is not a success criterion;
     "p95 response time under 200ms at 1,000 concurrent users" is. -->

- **SC-001**: [Measurable outcome.]
- **SC-002**: [Measurable outcome.]
- **SC-003**: [User-facing or business metric.]

## Out of Scope

<!-- The most commonly skipped section, and one of the highest-leverage —
     its absence is what lets scope quietly balloon mid-implementation. -->

- [Explicitly excluded capability #1]
- [Explicitly excluded capability #2]

## Unchanged Behavior *(bug fixes / Track B — regression guard)*

<!-- For a bug fix or any change to existing behavior, state explicitly what
     must KEEP working exactly as before. "Out of Scope" says what you are not
     building; this says what the change must not break — the difference between
     a surgical fix and a regression. Each line should become a regression test
     (write it first, watch it stay green). Delete this section for greenfield
     work with no existing behavior to protect. -->

- [Existing behavior that MUST continue unchanged, e.g. "WHEN a user submits a
  valid form THEN it is processed exactly as before."]
- [Another invariant the change must not disturb.]

## Assumptions

<!-- Reasonable defaults you chose because the prompt didn't specify them.
     Different from NEEDS CLARIFICATION: these are decisions you made and
     are documenting, not open questions. -->

- [e.g., "Mobile support is out of scope for v1."]
- [e.g., "Existing authentication system will be reused as-is."]

## Spec Completeness Checklist

<!-- Self-review before treating this spec as ready for planning. -->

- [ ] No `[NEEDS CLARIFICATION]` markers remain unresolved
- [ ] Every requirement is testable and unambiguous
- [ ] Success criteria are measurable, not subjective
- [ ] Out of Scope section is filled in, not left as a placeholder
- [ ] (Bug fix / Track B) Unchanged-behavior regression guards are listed
- [ ] No speculative "might need" features included
- [ ] Spec contains no tech stack, API shapes, or implementation detail
- [ ] (If applicable) Formal Verification Obligations state the exact
      property/invariant, not just "must be correct" — and name no tool
