# Phase 3.5 — Analyze (gate, non-destructive)

The last guide-side gate before implementation: cross-check the artifacts
against each other and the constitution **while no code yet exists** — the
cheapest place to catch a requirement that never became a task. Invoke the
`artifact-analyzer` agent (full detail there). It is **conditional on the track**:

- **Track A** — skip (no artifacts to cross-check).
- **Track B** — optional quick pass: spec ↔ tasks coverage only (no `plan.md`).
- **Track C** — default-on: full spec ↔ plan ↔ tasks cross-check.
- **Track D** — default-on, extended: also reconcile `data-model.md` /
  `contracts/`, the ADR, and characterization-test ordering.

`research.md` is reconciled on any track whenever it exists — not
Track-D-exclusive like the rest of that list; see `artifact-analyzer.md`.

On C/D analyze runs **by default**, but starting it is a gate the human
controls, not a hard requirement: the user may **explicitly skip** it —
*before it ever runs*. Don't skip silently — offer to run it, and if the user
declines, **record the skip** (and that it was their call) in
`decision-log.md` before proceeding to implementation. Skipping is the user's
decision to make knowingly, exactly as with review. But this decision is only
available up front: once analyze has been invoked once for a feature, there is
no mid-loop bailout on Blocker or Should-fix findings — see step 2.

`analyze` **reports, it does not edit** — what it checks lives in
`artifact-analyzer.md`. Each finding is routed to **the phase that owns the
fix — Specify, Plan, or Tasks** (a missing task is a `tasks.md` fix; a
spec/plan contradiction is a `spec.md` or `plan.md` fix).

1. Offer to run analyze at the depth for the track. If the user declines on C/D,
   log the skip (step 4) and proceed — this is the only exit that doesn't
   require a clean verdict.
2. **Any Blocker or Should-fix finding**: loop back to whichever phase owns
   the fix (Specify / Plan / Tasks) — not always Tasks — fix there, then
   **re-run analyze**. Repeat until a run comes back with zero open Blockers
   and zero open Should-fix findings. When re-running, tell the analyzer
   which artifact(s) changed since the last pass — it focuses the re-check
   there while still re-verifying cross-artifact consistency. There is no
   accepting a Blocker or Should-fix finding in place of fixing it once
   analyze has run — a deliberate scope call (e.g. "not covering FR-004 this
   iteration") is resolved by writing it into the owning artifact (e.g.
   `spec.md`'s Out-of-Scope section) so analyze no longer flags it, not by
   logging acceptance and moving on. Don't start implementation while any
   Blocker or Should-fix finding is open. **Notes are advisory, not gating:**
   report them every run and let the human act on any they
   choose, but a run with open Notes and nothing else open is a clean
   verdict — it does not trigger another loop iteration by itself.
3. Each iteration still goes through the normal per-phase approval gate: the
   owning agent (`specifier` / `planner` / `task-decomposer`) proposes the
   fix, the human approves it, then analyze re-runs. This is human-approved at
   every step, just never human-skippable mid-loop on a Blocker or Should-fix.
   If a Blocker or Should-fix finding survives two consecutive runs unchanged,
   say so explicitly at that approval gate — that is where the human notices
   a stuck loop and redirects the fix. Deliberately no separate iteration cap
   for these; Notes were demoted to advisory instead, since
   those — not Blocker/Should-fix — were the non-convergent case in
   practice.
4. On a clean verdict (zero Blockers, zero Should-fix — open Notes don't
   block this), **or an explicit skip decided in step 1**, append an
   **Analyze** row to `decision-log.md` (verdict, or "skipped — user's call",
   how many loop iterations it took, and how many Notes remained open at
   closure, if any) and tell the user implementation can begin story by
   story.
