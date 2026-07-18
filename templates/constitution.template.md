# [PROJECT NAME] Constitution

<!--
Sync Impact Report (fill in on every amendment):
- Version: [OLD] -> [NEW]
- Modified principles: [list]
- Added sections: [list]
- Removed sections: [list]
- Templates requiring updates: [list, or "none"]
-->

## Core Principles

### Article I — [Principle name, e.g. "Library-First"]

[Statement of the principle in 1-3 sentences. Should be a testable rule, not
an aspiration.]

**Rationale**: [Why this principle exists — what failure mode it prevents.]

### Article II — [Principle name]

[Statement of the principle.]

**Rationale**: [Why.]

### Article III — Test-First Development

Tests are written before implementation and must fail for the right reason
before any implementation code is written. Every test carries evidence of
its own intent.

Corollaries:

- No implementation code is written before a failing test exists for it.
- Tests are not modified to make them pass; implementation changes to satisfy
  tests, never the reverse, without an explicit, logged exception.
- Every test documents its own intent: a docstring or comment naming the
  acceptance-criterion ID it verifies and a one-line rationale for why the
  test proves that criterion.

**Rationale**: A test written after the implementation tends to describe what
the code does, not what it should do — it inherits the implementation's
blind spots.

### Article IV — [Principle name]

[Statement of the principle.]

**Rationale**: [Why.]

## Additional Constraints

[Technology stack requirements, compliance obligations, deployment policies,
or other constraints that apply across all features. Optional section —
delete if not needed.]

## Development Workflow

[Code review requirements, testing gates, deployment approval process.
Optional section — delete if not needed.]

## Governance

This constitution supersedes all other project practices and guidelines.
Amendments require documentation of the change, a migration or compatibility
review, and explicit approval before the new version takes effect.

All PRs and reviews must verify compliance with this constitution. Complexity
introduced beyond these principles must be justified in `decision-log.md`.
Use `AGENTS.md` for day-to-day runtime development guidance; this document
is the constitutional layer it must remain consistent with.

### Amendments

To amend this constitution:

1. Propose the change (what's changing, and why) and reach agreement with the
   project's maintainers.
2. Update this file, filling in the Sync Impact Report at the top.
3. Bump the version per the rules below.
4. Propagate any resulting changes to `AGENTS.md`, templates, and skill
   files so they stay consistent with the new text.

Versioning policy (semantic):

- **MAJOR**: backward-incompatible governance/principle removals or
  redefinitions.
- **MINOR**: a new principle or materially expanded guidance added.
- **PATCH**: clarifications, wording fixes, typo corrections, non-semantic
  refinements.

**Version**: [X.Y.Z] | **Ratified**: [DATE] | **Last Amended**: [DATE]
