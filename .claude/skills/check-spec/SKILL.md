---
name: check-spec
description: "Use to generate a requirements-quality checklist for a feature spec — \"unit tests for the spec\". Triggers: \"is this spec ready\", \"make a checklist for this spec\", \"review requirements quality\", or before planning a high-stakes feature. Tests whether the requirements are complete, clear, consistent, and measurable — not whether code works, and not for resolving ambiguity (clarify-spec)."
---

# Check Spec

Tests the **spec**, not the code — each item is a unit test for a requirement:
complete, clear, consistent, measurable? A spec that passes `clarify-spec` can still
be vague or contradictory; this catches it before planning.

Output: `specs/<NNN-feature>/checklist.md` (or `security-checklist.md`), from
`templates/checklist.template.md`.

## When to run

After `clarify-spec`, before `plan`, on any risky or ambiguous feature. Also for
narrow repeatable domain passes — security, accessibility, migration-readiness.

Track-gated (see `develop-feature`'s Step R):

| Track | Run check-spec? | Why |
|---|---|---|
| **A · Trivial — Direct change** | Skip | No `spec.md` to check. |
| **B · Simple — Patch** | Optional | Short spec, low stakes — offer it; don't default it on. |
| **C · Moderate — Feature** | Default-on, skippable up front | Standard-depth spec; run unless the human declines before it starts. |
| **D · Complex — Architecture / brownfield** | Default-on, skippable up front | Highest stakes; same default as C. |

"Skippable up front" means the human may decline before it starts, the same
convention `artifact-analyzer` uses — it is not a mid-run bailout. Record
whichever way it goes (ran, or declined) at the approval gate.

## Behavioral guardrails

<!-- GUARDRAILS:skill -->
- **No guessing.** Where input leaves something unspecified, write
  `[NEEDS CLARIFICATION: specific question]` and surface it — never silently
  invent an assumption.
- **Investigate before claiming.** Never make statements about the codebase
  without first reading the relevant files. If a claim requires looking at
  code, look first.
- **Conservative by default.** Recommend before you write; stop and ask before
  anything irreversible (deleting files, force-pushing, dropping tables,
  external service calls).
<!-- /GUARDRAILS:skill -->
- **Test requirements, not implementation.** Items interrogate spec quality
  ("Is the latency target measurable?"), never code behaviour. Runnable-only
  items belong in the test suite.
- **No trivially-passing items.** Each must be one a real spec could fail.
- **Requires `templates/checklist.template.md`** — missing? Stop.

## Step 1 — Pick the checklist's purpose

Ask (or infer) which kind of pass:

- **Requirements-quality** (default) — completeness, clarity, consistency,
  measurability of the spec as a whole.
- **Domain pass** — security, accessibility, migration-readiness, performance,
  data-privacy.

One checklist, one purpose.

## Step 2 — Generate items from the spec

Read `spec.md` (and `plan.md` if present). Derive concrete, checkable items.
For a requirements-quality pass, cover:

- **Completeness** — Are all user stories given acceptance criteria? Is every
  error/edge case from the Edge Cases section reflected in a requirement? Is Out
  of Scope filled in, not a placeholder?
- **Clarity** — Is each requirement testable and unambiguous? Any "fast",
  "secure", "intuitive" left undefined?
- **Consistency** — Do any requirements contradict each other or the success
  criteria? Do the non-functional targets match what the stories imply?
- **Measurability** — Is every success criterion a number or a binary, not a
  vibe?

For a domain pass, generate items from that domain's known failure modes (e.g.
security: authN/authZ stated per endpoint, secrets handling, input validation,
PII classification, rate limiting).

Write each as `CHK0NN [item]`, grouped by category per the template. Strip
instructional comments from output.

## Step 3 — Run the checklist and report

Check each item: `[x]` pass, `[ ]` fail, one-line note on every finding (not
trivial passes). Then:

- Summarise failures — the spec's real gaps.
- Route: ambiguity → `clarify-spec`; missing requirements → spec author.
- State if spec is plan-ready.

**Example output** (requirements-quality pass, abbreviated):

```markdown
## Measurability
- [x] CHK001 Every success criterion is a number or binary.
- [ ] CHK002 Latency target is quantified.
      ✖ SC-002 says "responses feel fast" — no number. → clarify-spec (suggest p95 target).

## Completeness
- [ ] CHK003 Every user story has acceptance scenarios.
      ✖ US2 has none. → back to author.
- [x] CHK004 Out of Scope is filled in, not a placeholder.
```

> Verdict: 2 of 4 failing — not plan-ready. CHK002 is an ambiguity (route to
> `clarify-spec`); CHK003 is a missing requirement (route to the author).

## What this skill does not do

- Doesn't test implementation — that's the test suite.
- Doesn't resolve gaps — routes them to `clarify-spec` or the author.
