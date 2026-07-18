# Phase 2 — Plan

Only after the user has approved Phase 1. Delegates the drafting to the
`planner` agent — protocol, model tier, and constitution-gate method live in
`planner.md`.

1. Invoke the `planner` agent. Pass it: the path to the now-approved
   `spec.md`, the path to `research.md` **if it exists** (seeded at Step 0
   from the triggering prompt, or from an earlier pass — check for it before
   invoking, don't assume it's absent just because this is a non-Track-D
   feature), and the pack ID(s) and rule-file path(s) of any opted-in
   extensions (paths, not rule text — Step R's rule). It writes `plan.md`
   per the canonical template, Status still `Draft`.
2. The agent returns a short summary — not the document text — covering the
   three gate verdicts with their reasoning, any Complexity Tracking entries,
   research findings, and extension-compliance notes. Relay all of it — a
   gate fail or an unmet **Verification** condition is a blocker unless a
   human explicitly accepts the risk, recorded in `decision-log.md`; don't
   summarize it away.
3. **Stop.** Tell the human the file path (`specs/<NNN>/plan.md`) and the
   gate/extension/research summary from step 2 (see `SKILL.md`'s "don't
   reprint drafted documents" guardrail). Ask for explicit approval, and
   resolution of any `[NEEDS CLARIFICATION]` markers left in `plan.md`,
   before touching `tasks.md` — resolution means the marker is answered and
   removed from `plan.md` itself, not just noted as an accepted exemption in
   `decision-log.md`. Don't proceed on your own judgment. On approval, set
   `plan.md`'s **Status** to `Approved — <who>, <date>` and append a **Plan**
   row to `decision-log.md`.
4. On requested changes, re-invoke `planner` with the specific feedback rather
   than hand-editing `plan.md` yourself, for the same reason as Phase 1.
