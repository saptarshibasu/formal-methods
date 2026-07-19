---
name: clarify-spec
description: "Use after a draft spec exists but before planning, to surface and resolve ambiguity — triggers: \"clarify the spec\", \"what's ambiguous here\", \"are there gaps in this spec\". Asks a few targeted questions one at a time, then writes the answers back into spec.md. Not for writing a spec from scratch (develop-feature)."
---

# Clarify Spec

Hardens a draft `spec.md` before planning by surfacing decisions an implementer
would otherwise guess. Cheapest quality gate: one question now vs. a rebuild later.

## When to run

After spec draft exists, before `plan`. Clears `[NEEDS CLARIFICATION]` markers.

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
- **Ask, don't assume.** Every question must be a real open decision. If you can
  answer it from the spec or codebase, go look — don't ask.
- **One or two questions at a time.** Focused conversation, not a questionnaire.
- **Cap it.** At most ~5 per pass. More than five real ambiguities = spec not
  ready for clarification; send it back for more drafting.
- **Read-only until the end.** Don't edit `spec.md` until the user has answered;
  then write resolutions in — don't paraphrase them away.

## Step 1 — Scan for ambiguity

Read spec and relevant codebase. Build a candidate list prioritised by blast
radius (downstream damage from a wrong guess):

- Existing `[NEEDS CLARIFICATION: ...]` markers — pre-identified, go to the top.
- **Underspecified behaviour:** error cases, empty/boundary inputs, concurrency,
  idempotency, ordering, pagination limits.
- **Ambiguous criteria:** "fast", "secure", "user-friendly" with no target.
- **Unstated assumptions** about auth, data ownership, multi-tenancy, scale.
- **Scope edges:** things a reader might assume in or out that Out of Scope
  doesn't pin down.

Narrow to ~5 highest-impact. Discard anything you can resolve by reading.

**Reason before asking.** For each candidate: what breaks downstream if you
guess wrong? That's how you rank and how you tell a real question from something
to just go read.

## Step 2 — Ask, one or two at a time

Ask concrete, decision-shaped questions with options where natural:

> "When a user submits a duplicate order, should the system reject it (409),
> silently dedupe, or accept both? The spec doesn't say, and it changes the
> data model."

State *why it matters* in one clause — proves the question is load-bearing.

## Step 3 — Write resolutions back into the spec

1. Replace each `[NEEDS CLARIFICATION]` marker with the decided behaviour in the
   right section (requirement → FR; assumption → Assumptions; scope → Out of Scope).
2. For unmarked ambiguities, add the decision to the appropriate section.
3. Don't invent beyond what the user decided — new question raised? Surface it.
4. Re-run the spec's own Spec Completeness Checklist.

**Example write-back** — user answered "reject duplicates with a 409":

```diff
- **FR-004**: System MUST handle duplicate order submissions
-   [NEEDS CLARIFICATION: reject, dedupe, or accept both?]
+ **FR-004**: System MUST reject a duplicate order submission with HTTP 409
+   and a machine-readable error code; no second order is created.
```

## Step 4 — Report

What resolved, what remains open, whether spec is ready for `plan`. If markers
remain deferred, say so — don't leave the spec looking done when it isn't.

## What this skill does not do

- Doesn't write specs from scratch (`develop-feature`) or move to `plan`/`tasks`.
- Never invents answers to its own questions.
