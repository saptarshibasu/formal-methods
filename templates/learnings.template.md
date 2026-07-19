<!--
  TEMPLATE — one per feature, at specs/<NNN-feature-name>/learnings.md.

  Unlike spec/plan/tasks/decision-log, this file is NOT gated. It has no
  Status header and no approval step. It exists purely so a discovery made
  mid-story survives the fresh-context boundary between story iterations —
  each implementor/debugger run starts with a clean context and no memory of
  what the last run figured out the hard way. Without this file, that
  discovery dies with the sub-agent that found it and gets re-discovered
  (or re-broken) next story.

  Rules:
  - Append only — for `implementor` and `debugger`, within a story. Never
    rewrite or delete a prior entry, even a superseded one — add a new entry
    noting what changed instead of erasing the old one. This is what keeps
    mid-story writes safe to do without a gate.
  - One entry per discovery, newest at the bottom.
  - `implementor` and `debugger` read this file first (if it exists) before
    starting work on this feature, and append to it as they go — not only in
    their final report, so the discovery survives even if the session ends
    mid-story.
  - Not a substitute for `decision-log.md`. That file is the gated,
    human-approved audit trail of what was decided. This file is the
    ungated "things we learned the hard way" scratchpad — low ceremony,
    no approval needed to append, and it may include things that turned out
    to be wrong (say so in the entry) as well as things that were right.
  - Delete this comment block once the first real entry is appended.

  Compaction (the one exception to append-only):
  - Append-only applies to `implementor`/`debugger` mid-story so they never
    need to pause for a gate. It does not mean this file grows forever unread
    — the orchestrator (`develop-feature`), never a sub-agent, offers a
    compaction pass at each story's Phase 5 checkpoint (see
    `references/phase-5-review.md`): propose a deduped/merged version (fold
    repeated gotchas into one entry, drop entries an applied `AGENTS.md`
    correction has already fixed, keep everything still-relevant), show the
    before/after to the human, and only rewrite the file on explicit approval.
  - A compaction pass replaces the file's body, not silently — leave one
    marker entry at the top of the (still append-only from here) log:

    `## [DATE] — compaction — N entries merged into M; superseded-by-AGENTS.md-fix entries dropped: [list]`

    so a later read still knows a GC pass happened and roughly what it removed.
  - Never compact mid-story, and never let `implementor`/`debugger` trigger
    or perform it themselves — same "propose, human approves" gate as the
    `AGENTS.md` self-correction rule, applied to this file instead.
-->

# Learnings: [FEATURE NAME]

**Feature**: `[###-feature-name]`

Append-only between compaction passes. Newest entries at the bottom. Each
entry should tell a future session — including a fresh run of yourself —
something it could not already infer from `AGENTS.md`, the spec, or the code
itself: a wrong-turn command that looked right but wasn't, the real location
of something the plan assumed was elsewhere, a version-specific gotcha, a
flaky test and its actual cause. Periodically, at a story checkpoint, the
orchestrator may propose a human-approved compaction pass that dedupes/merges
entries and drops ones an applied `AGENTS.md` correction already fixed — see
the comment block above for how that's marked.

<!-- Entry format — copy for each new entry:

## [ENTRY-DATE] — [implementor|debugger] — [US# or task ID]

[What was discovered, specific enough to act on directly. Skip anything
already obvious from AGENTS.md or the spec — this file is for what those
don't cover yet.]

-->
