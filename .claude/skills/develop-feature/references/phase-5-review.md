# Phase 5 — Review & commit (code-reviewer gate)

Once a story is fully green, invoke the `code-reviewer` agent on that story's
diff before moving to the next story's Phase 3.7 (or batch several stories
into one review pass if the human explicitly asks for that — record the
choice in `decision-log.md`; the default is one review per story so diffs
stay small and issues surface early).

1. **Formal verification recheck, if this story has one.** Check `spec.md`'s
   Formal Verification Obligations and `plan.md`'s tool choice for this
   story. If either names one, invoke the matching verifier —
   `lean4-verifier` or `tlaplus-verifier` — fresh, against the artifact as it
   stands in the current diff. This re-runs what Phase 4 step 5 already
   checked once, for the same reason `code-reviewer`'s own gates re-run
   coverage/build itself rather than trusting `implementor`'s report: Phase
   4's pass was against its own intermediate state, not the finished diff
   under review here. Carry the verifier's report (verified / not, with its
   exact diagnostic) into step 2.
2. Invoke `code-reviewer`. Pass it: the diff (or the files `implementor`
   touched), the spec path, the feature's `decision-log.md` (for opted-in
   extension packs), and — if step 1 ran — the verifier's report **and the
   theorem/spec writer's report retained from Phase 4 step 5, correspondence
   mapping included** — a "verified" tool result alone doesn't satisfy
   `code-reviewer`'s formal gate; its own correspondence check reads that
   mapping against the diff independently. Tell it to fold either a "not
   verified" verifier result, or a missing/incredible correspondence
   mapping, into its Blocker list as `Kind: formal`. Its gates, checks, and
   severity/`Kind` definitions live in `code-reviewer.md`; expect findings
   grouped by severity, every Blocker tagged with a `Kind`, and a verdict.
3. If the verdict has Blockers, **this skill runs the review↔fix loop** —
   sub-agents can't invoke each other or pause for approval, so the loop
   lives here, not inside `code-reviewer`. First, split the numbered Blocker
   list by `Kind` (definitions in `code-reviewer.md`'s "How to report"):
   **`defect`** → `debugger`; **`design`** → `implementor` (per its
   design-Blocker section — nothing to reproduce, so `debugger` is a role
   mismatch); **`coverage`** → `test-writer` (per its add-coverage mode —
   the fix is a new test, not a code change); **`formal`** → the matching
   `lean4-theorem-writer`/`tlaplus-spec-writer` to redraft, then the
   matching `lean4-verifier`/`tlaplus-verifier` to recheck before this
   round's `code-reviewer` re-check pass (same writer→verifier shape as
   Phase 4 step 5, run again here because a redraft needs its own fresh
   verification, not an assumed pass).

   Then, per round:
   - Relay the complete findings, then ask the human: *"Invoke the debugger
     on the [N] defect Blocker(s), implementor on the [M] design Blocker(s),
     test-writer on the [K] coverage Blocker(s), and the
     lean4-theorem-writer/tlaplus-spec-writer on the [J] formal Blocker(s)
     above?"* Wait for explicit approval before the **first** round only;
     later rounds in the same loop don't re-ask.
   - Invoke `debugger` on the open `defect` Blockers (numbered list —
     file:line, description, suggested fix — plus this feature's
     `learnings.md` path), `implementor` on the open `design` Blockers (per
     its "invoked with a reviewer's design Blocker" section), `test-writer`
     on the open `coverage` Blockers (per its "add coverage to existing
     code" mode — pass the specific uncovered lines/branches, not just the
     aggregate percentage), and the matching `lean4-theorem-writer`/
     `tlaplus-spec-writer` on the open `formal` Blockers (pass the prior
     verifier's exact diagnostic, then re-verify via the matching
     `lean4-verifier`/`tlaplus-verifier` before this round's `code-reviewer`
     re-check) — all four can run in the same round since they touch
     different findings. On round 2+, note per Blocker what's still open and
     what was already tried, so no agent repeats a failed fix.
   - Re-invoke `code-reviewer` for a **re-check pass**, passing the prior
     findings, all agents' reports for the round (however many ran), and the
     files touched (see the Fix-loop handoff section of
     `.agents/agents/code-reviewer.md`) — this re-check re-runs the
     deterministic gates too, not just the qualitative read, since a
     `test-writer` fix should move the coverage number and an `implementor`
     fix should move the static-analysis count.
   - Decide from the re-check verdict: all Blockers resolved → step 4; some
     resolved, some still open → another round; **no forward progress** on a
     given Blocker (it survives two consecutive rounds, `debugger` calls it a
     spec bug, `implementor` reports it can't apply the fix cleanly,
     `test-writer` reports the uncovered code can't be characterized without
     a spec decision, or the theorem/spec writer reports the obligation can't
     be proved/model-checked as stated) → stop looping on that Blocker and
     take it to the human — accept the risk, revise the spec/floor/obligation,
     or redesign (step 5). Other still-open Blockers can keep looping
     independently.
   - If any round's `debugger`, `implementor`, or `test-writer` report
     includes a proposed `AGENTS.md` correction, relay it and get approval
     the same way Phase 4 step 2 does — apply directly if approved, don't
     let it block the loop.
   - If `implementor` flags a **recurring pattern** of `design` Blockers
     across stories on this feature (its report's step 6), relay that to the
     human alongside the Blocker resolution itself — it's signal that
     `tasks.md` is under-specified or the plan is being under-enforced, a
     separate thing to fix from closing out the individual findings, and
     worth surfacing even though it doesn't block this round's verdict. A
     recurring pattern of `coverage` Blockers on the same area is the same
     kind of signal for `test-writer`'s "add coverage to existing code" mode,
     and a recurring pattern of `formal` Blockers is the same kind of signal
     that the obligation itself (in `spec.md`) or the tool choice (in
     `plan.md`) needs a second look, not just another draft — surface each
     the same way.
4. On a clean verdict (`approve` or `approve-with-nits`): append a **Review**
   row to `decision-log.md`, let the human commit (the `.githooks/pre-commit`
   hook runs its own checks), then continue to step 6 before moving to the
   next story's `phase-3.7-tests.md` — or, if this was the last story, step 6
   still applies before the feature is called done (see `SKILL.md`'s closing
   note on `docs-writer`).
5. On `request-changes` with a Blocker `code-reviewer` escalated to the human
   (not resolved by its internal loop): resolve it together. If the root
   cause turns out to be a spec, plan, or tasks bug rather than an
   implementation bug, loop back to `specifier`, `planner`, or
   `task-decomposer` (whichever owns the affected artifact) instead of
   forcing another `implementor` pass on an already-correct implementation.
   Once resolved, re-run Phase 5 for this story.
6. **Offer a `learnings.md` compaction pass.** This story's checkpoint —
   Review row just committed — is the natural point to garbage-collect the
   feature's append-only scratchpad before it's carried, unread in full,
   into the next story's fresh `implementor`/`debugger` context. This skill
   (never `implementor`/`debugger` themselves) does the offering:
   - Read the current `learnings.md`. If it's short and every entry is still
     distinct, accurate, and relevant, say so and skip — compaction isn't
     mandatory every story, only worth doing once there's real duplication,
     bloat, or a contradiction (repeated variants of the same gotcha, entries
     about a command that an approved `AGENTS.md` correction already fixed,
     or two entries that disagree because one is now stale).
   - Otherwise, draft a compacted version: merge duplicate/near-duplicate
     entries into one, and resolve any pair where a later entry says it
     supersedes an earlier one (`implementor`/`debugger` flag this by noting
     "supersedes the [date] entry about X" when they append — they never edit
     the old entry themselves, mid-story writes stay append-only-with-no-gate
     by design; **this compaction pass is where that gets reconciled into one
     accurate entry**, not a backstop for something they should have caught).
     Also drop any entry whose discovery an **applied**
     `AGENTS.md` correction (Phase 4 step 2 or this phase's step 3) has
     already fixed — that entry's job (surviving until the fix landed) is
     done. Never drop an entry just because it's old, or one that isn't
     clearly superseded or contradicted.
   - Show the human the before/after (or a summary of what's merging into
     what and what's being dropped, for a long file) and wait for approval
     before rewriting the file. On approval, rewrite `learnings.md` with the
     compacted entries plus the one marker line the template's Compaction
     rules describe, so a later read knows a pass happened and roughly what
     it removed. On decline, leave the file as-is and move on — this is an
     offer, not a gate the story is blocked on.
   - This step never runs mid-story and never runs unattended — same
     "propose, human approves" model as the `AGENTS.md` self-correction rule,
     applied to this file instead of that one.
