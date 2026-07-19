# Phase 1 — Specify

Delegates the drafting to the `specifier` agent, invoked in its own fresh
context. Its drafting protocol, model tier, and rules live in
`specifier.md` — pass it inputs; don't re-teach or override them here.

1. Invoke the `specifier` agent. Pass it: the user's feature description, the
   path to the newly-scaffolded `spec.md`, and the pack ID(s) and rule-file
   path(s) of any opted-in extensions from Step R (paths, not rule text —
   Step R's rule). It writes `spec.md` per the canonical template, Status
   still `Draft`.
2. The agent returns a short summary — not the document text — covering any
   open `[NEEDS CLARIFICATION]` markers, the Completeness Checklist result,
   and extension-compliance notes. Relay all of it — don't summarize away an
   unmet **Verification** condition; surface it before approval, same as if
   you'd drafted it yourself.
3. **Stop.** Tell the human the file path (`specs/<NNN>/spec.md`) and the
   summary from step 2 (see `SKILL.md`'s "don't reprint drafted documents"
   guardrail). **Offer the optional sharpeners before approval** — they
   aren't auto-run, so name them or the human won't know they exist: if any
   `[NEEDS CLARIFICATION]` markers remain, recommend the `clarify-spec`
   skill; for a high-stakes, security-sensitive, or ambiguous spec, offer the
   `check-spec` skill (a requirements-quality or domain pass). Both are
   optional — surface them and let the human choose; don't run them
   unprompted. Then ask for explicit approval. Approval requires every
   `[NEEDS CLARIFICATION]` marker to be resolved **in `spec.md` itself** —
   the marker answered and removed from the document — before touching
   `plan.md`; a logged exemption in `decision-log.md` records that a human
   accepted a risk, it does not substitute for writing the resolution into
   the artifact. Don't proceed on your own judgment. On approval, set
   `spec.md`'s **Status** to `Approved — <who>, <date>` and append a
   **Specify** row to `decision-log.md`.
4. If the human requests substantive changes instead of approving, re-invoke
   `specifier` with the specific feedback (it re-reads its own prior draft
   from disk) rather than hand-editing `spec.md` yourself — the drafting
   agent stays responsible for spec quality, not the orchestrator. Small
   wording fixes you can make directly.
