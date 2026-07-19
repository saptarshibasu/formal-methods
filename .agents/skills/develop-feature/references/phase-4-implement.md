# Phase 4 — Implement (implementor gate)

Delegates the actual red→green→refactor work to the `implementor` agent,
invoked once per story in its own fresh context — its method, model tier,
and hard rules live in `implementor.md`.

**Track A** — no `implementor` invocation: Step R already routes trivial
changes straight to a direct change plus `code-reviewer` on the diff; this
phase does not apply.

**Tracks B/C/D**, once the current story's tests are confirmed red
(`phase-3.7-tests.md`):

1. Invoke the `implementor` agent. Pass it: the approved `tasks.md` and
   `plan.md`/`spec.md`, the current story's scope (which task IDs), the
   test-writer's confirmed-red report for this story, and the path to this
   feature's `learnings.md` (it reads prior entries first and appends its
   own as it works, so nothing found mid-story is lost if the session ends
   before the report). Expect back either a completed-story report or an
   **escalation request** — sub-agents can't invoke each other, so this
   skill runs the `debugger` round (step 3 below).
2. Relay its report: tasks completed, tests now green, any `debugger`
   escalation request, any uncovered case it found but didn't add a test
   for (that's a `test-writer` follow-up, not something implementor should
   have added silently), and any deviation from `plan.md` it had to make. If
   the report includes a proposed `AGENTS.md` correction, relay it and ask
   for approval; on approval, apply the one-line fix directly (or hand it to
   `docs-writer` if it's bigger than a single line) — don't apply it
   unapproved, and don't let it block the rest of the story's progress. Note
   (don't act yet) which `learnings.md` entry, if any, documented the
   now-fixed command — it's a candidate to drop at this story's Phase 5
   compaction offer, not something to edit out of the file right now.
3. If the report contains a `debugger` escalation request: invoke the
   `debugger` agent with the failing test, the exact error and stack trace,
   the spec path, what `implementor` already tried, and the same
   `learnings.md` path (it reads prior entries and appends its own root-cause
   findings the same way `implementor` does); then re-invoke `implementor`
   with the debugger's report so it confirms green and finishes the story's
   remaining tasks.
4. If the report shows a task left incomplete, a `[NEEDS CLARIFICATION]`
   marker, or a flagged-wrong test: resolve it with the human first — loop
   back to whichever phase owns the fix (the test itself → `test-writer`;
   `plan.md` → `planner`; `tasks.md` → `task-decomposer`) before continuing.
   Don't proceed to review on a partially-green story.
5. **Formal verification, if this story has one.** Check `spec.md`'s Formal
   Verification Obligations section and `plan.md`'s Formal Verification tool
   choice for this story. If neither names one, skip to step 6.
   - Invoke the matching writer — `lean4-theorem-writer` for a Lean 4
     obligation, `tlaplus-spec-writer` for a TLA+ one — passing the exact
     obligation text from `spec.md`, the artifact's file path `plan.md`
     named, and — this is what its own "What to read first" now requires —
     the implementation class/method/module `plan.md`'s Formal Verification
     field names as the target, so it reads the real code before modeling
     it. It drafts the `.lean`, or `.tla`+`.cfg`, artifact plus a
     correspondence-mapping statement (model ↔ real code) and hands off; it
     never claims the result verified.
   - Invoke the matching verifier — `lean4-verifier` or `tlaplus-verifier` —
     on that artifact. Relay its report as-is: verified, or not (with the
     compiler's/TLC's own diagnostic). **Retain the writer's full report,
     correspondence mapping included** — Phase 5 step 1 needs it alongside
     the verifier's report; a tool-verified result on its own isn't
     sufficient for `code-reviewer`'s correspondence check there.
   - **Not verified**: pass the verifier's exact diagnostic back to the same
     writer agent for **one focused revision round**, then re-verify. Same
     shape as the `debugger` escalation rule above — one round, then
     reassess. If still not verified after that round, or the writer reports
     the obligation can't be proved/model-checked as stated (it found a
     genuine counterexample or a stuck proof state), **stop and escalate to
     the human** — accept the risk, revise the obligation in `spec.md`, or
     reconsider the approach in `plan.md` — rather than looping
     indefinitely. Don't proceed to step 6 on an unresolved obligation.
6. Once every test for this story is green, the story-level suite passes,
   and (if applicable) step 5's formal verification is confirmed, append an
   **Implement** row to `decision-log.md` for this story and continue to
   `phase-5-review.md`.
