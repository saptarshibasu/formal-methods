<!--
  TEMPLATE — one per feature, at specs/<NNN-feature-name>/decision-log.md.

  This is the feature's DURABLE, COMMITTED audit trail. Live resume state lives
  elsewhere — each document's **Status** header (Draft -> Approved) — so the log
  doesn't track work in flight. It is committed and it outlives the feature, so a
  future agent or human can see WHAT was decided, WHO approved it, and WHY —
  without replaying chat history that no longer exists.

  Append one row per decision as the feature moves through the workflow. Never
  rewrite history: if a later decision reverses an earlier one, add a new row
  that references the one it supersedes. Keep each entry to a line or two —
  rationale, not prose.

  Cross-cutting decisions (a new pattern other features will follow) belong in a
  full ADR under docs/adr/, not here; in that case log a one-line pointer to the
  ADR number. This file is for decisions local to THIS feature.

  Delete this comment block once the first real entry is in.
-->

# Decision Log: [FEATURE NAME]

**Feature**: `[###-feature-name]` | **Created**: [DATE]

This log records the human-approved decisions for this feature: the chosen
workflow track, any opted-in rule extensions, each phase-gate approval, and any
deviation from the spec or plan made during implementation.

| Date | Stage | Decision | Rationale | Approved by |
|---|---|---|---|---|
| [DATE] | Route | Track [A/B/C/D] — [name] | [why this track fits the change's scope/risk] | [human] |
| [DATE] | Extensions | Opted in: [pack-id → rule-file path, or "none"] | [why these packs apply / don't] | [human] |
| [DATE] | Specify | spec.md approved | [one line: what was settled, e.g. clarifications resolved] | [human] |
| [DATE] | Plan | plan.md approved | [constitution gate results; any complexity justified] | [human] |
| [DATE] | Tasks | tasks.md approved | [one line] | [human] |
| [DATE] | Analyze | [implementation-ready / skipped — user's call] | [cross-artifact check: coverage + consistency; loop iterations to zero Blockers/Should-fix, open Notes remaining (if any), or why skipped] | [human] |
| [DATE] | Tests (red) — US[N] | [N tests confirmed failing / skipped — user's call] | [right-reason failures; any characterization decision on Track D] | [human] |
| [DATE] | Implement — US[N] | story green | [tasks completed; any debugger round, deviation from plan, or formal-verification draft/verify round (if applicable)] | [human] |
| [DATE] | Review — US[N] | [approve / approve-with-nits] | [Blockers resolved via the fix loop, if any — debugger/implementor/test-writer/formal-obligation writer, by Kind] | [human] |

The three per-story rows (Tests red → Implement → Review) repeat for each user
story, in the priority order `tasks.md` defines.

## Notes

<!-- Optional free-text for anything that doesn't fit a row: a rejected
     alternative worth remembering, a risk accepted knowingly, a link to an ADR
     this feature triggered. Delete if unused. -->

- [Any deviation from spec/plan made during implementation, with the reason and
  who signed off — so the gap between "what we planned" and "what we shipped" is
  never silent.]
