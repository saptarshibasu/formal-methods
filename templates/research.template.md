<!--
  TEMPLATE — optional, one per feature at specs/<NNN-feature-name>/research.md.
  Created during Plan (Phase 2) to record open questions you had to RESOLVE
  before the plan could be finalised — version-sensitive library facts,
  unknowns about an existing system, trade-offs you investigated. It is the
  scratchpad that keeps that investigation out of plan.md so the plan stays
  clean.

  Delete this file entirely if the plan had no open questions worth recording —
  don't manufacture content. Once filled in, delete this comment block.

  This file gets revisited, not just written once — a resumed session or a
  later planner pass may turn up something that contradicts or corrects an
  entry already here. Unlike decision-log.md, this is NOT an append-only audit
  trail: it's a scratchpad that gets read wholesale into a fresh context every
  time a phase consults it, so it needs to reflect current understanding, not
  preserve history a reader has to sort through. In Open Questions Resolved
  and Still Open, a correction means editing the existing entry in place —
  don't leave a stale Finding sitting next to a corrected one. Alternatives
  Investigated is the one exception: rejected alternatives normally stay
  listed cumulatively, on purpose, so they aren't re-proposed later — only
  edit or remove a row there if the rejection itself gets reversed (the plan
  later adopts what this file said was rejected), since a "rejected because X"
  row sitting next to a plan that does X anyway is actively misleading, not
  useful history. If provenance matters, that's what version control is for.
-->

# Research: [FEATURE NAME]

**Feature**: `specs/[###-feature-name]/` | **Created**: [DATE]

## Open Questions Resolved

<!-- One entry per question that genuinely blocked planning. State what you
     needed to know, what you found, and the source — so a later reader (or
     agent) doesn't redo the investigation or distrust the answer. -->

### [Question 1 — e.g. "Which version of <library> ships <capability>?"]

- **Why it mattered**: [what in the plan depended on this]
- **Finding**: [the answer, specific and dated if version-sensitive]
- **Source**: [docs link, code reference, experiment, person]
- **Decision**: [what the plan now does as a result]

### [Question 2]

[Same shape.]

## Alternatives Investigated

<!-- Approaches you seriously considered and rejected. Recording these stops
     the team (and agents) from re-proposing them later. A rejected option with
     no stated reason invites a relitigation. -->

| Option | Considered for | Rejected because |
|---|---|---|
| [approach] | [what it would solve] | [concrete reason] |

## Still Open (carried into implementation)

<!-- Questions that did NOT block planning but remain genuinely unresolved.
     If something here would change the design, it belongs back in the spec as
     [NEEDS CLARIFICATION], not buried here. -->

- [e.g., "Exact cache TTL — start at 60s, tune against real traffic."]

## Pending Amendments

<!-- Transient hand-off slot — normally EMPTY. When a new human instruction
     reopens an Approved document (see develop-feature's "Reopening a
     completed feature"), the orchestrator records the requested change here
     verbatim BEFORE flipping that document's Status to Draft, so an
     interrupted session can recover the reason for the flip from disk
     instead of from a conversation that no longer exists. The entry is
     deleted at the moment the re-run gate is approved. An entry here plus a
     Draft status = a reopen in flight; a Draft status with a prior approval
     row in decision-log.md but NO entry here means the context is lost —
     ask the human what changed. -->

- [YYYY-MM-DD — `spec.md`/`plan.md`/`tasks.md` — the requested change,
  verbatim enough to act on without the original conversation]
