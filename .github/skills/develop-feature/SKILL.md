---
name: develop-feature
description: "Use when starting, resuming, or amending spec-driven development on any change, large or trivial — triggers: \"create a spec for X\", \"start a new feature: Z\", \"resume <feature folder>\", \"continue feature NNN\", \"use SDD for this\", \"write a spec before we code\", or any prompt naming an existing specs/<NNN>-<slug>/ folder. Proposes a right-sized workflow track for approval, then orchestrates Specify -> Plan -> Tasks -> Analyze -> (Tests red -> Implement green -> Review) through dedicated agents, gating each phase on human approval; on resume, diffs the prompt against the approved documents before any code is touched."
---

# Spec-Driven Feature

Runs the Specify → Plan → Tasks workflow described in this project's
`AGENTS.md`, populating `specs/<NNN-feature-slug>/{spec.md,plan.md,tasks.md}`
from the canonical templates in `templates/`. This skill is a thin
**orchestrator**: it routes the work, scaffolds the folder, and owns every
approval gate and `decision-log.md` entry, but the actual drafting of each
document is delegated to a dedicated agent — `specifier` (`spec.md`),
`planner` (`plan.md`), `task-decomposer` (`tasks.md`) — each invoked fresh, so
none of them carries the other phases' revision back-and-forth into its own
context, and each can be pinned to the model tier its phase actually needs
(`AGENTS.md` Model Routing). Three gated phases —
never skip a gate, and never merge two phases into one turn.

This file is intentionally short: it covers routing and the rules that apply
throughout the whole session. **Each phase's actual protocol lives in its own
file under `references/`, read only when you reach that phase** — see
"Phases" below. A feature commonly spans days across many separate
conversation turns; re-reading the phase file at the point of use, rather
than relying on one big upfront read, is what keeps the protocol from
decaying as the session grows.

## Behavioral guardrails (apply throughout this skill session)

These rules are active from Step R through Phase 5 — routing, drafting,
analysis, and implementation alike.

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
- **The human owns every approval gate — you never self-approve.** Each
  drafting phase (1, 2, 3) and the Analyze gate (3.5) ends by *stopping* for
  explicit human approval; never flip a document's Status to `Approved`, merge
  two phases into one turn, or proceed on your own judgment because the draft
  "looks fine." This is the kit's core promise ("agent proposes, human
  approves") and it holds for the whole session, not just when a phase
  reference file happens to be freshly in context.
- **Route fixes to the owning phase — never hand-apply a finding.** When
  `artifact-analyzer` (or any check) reports an issue, loop it back to the
  agent that owns that artifact — `specifier` for `spec.md`, `planner` for
  `plan.md`, `task-decomposer` for `tasks.md` — and re-run the gate after the
  human approves the fix. The analyzer is read-only and reports; you are the
  orchestrator and route. Don't edit `spec.md`/`plan.md`/`tasks.md` yourself to
  clear a finding (small wording fixes aside) — the drafting agent stays
  responsible for its artifact's quality.
- **No over-engineering.** Only specify, plan, and build what is directly
  requested — no abstractions, extra projects, or flexibility for hypothetical
  future requirements unless the user explicitly asks.
- **Template fidelity.** `spec.md`, `plan.md`, `tasks.md`, and
  `decision-log.md` follow their canonical templates in `templates/` exactly —
  no invented sections, fields, or status values beyond what the template
  defines. This binds the orchestrator's own edits (the Status header,
  `decision-log.md` rows) just as much as what `specifier`/`planner`/
  `task-decomposer` draft into the body — a freelanced addition is a template
  deviation whether it's a paragraph or a single field. If a template
  genuinely seems to be missing something a feature needs, raise it as a
  question back to the human rather than freelancing a fix.
- **Don't reprint drafted documents in chat.** Every drafting phase (1, 2, 3)
  ends the same way: an agent already wrote the document to disk, so tell the
  human the file path and the agent's summary — retyping the content doubles
  output-token cost for no benefit. If the human asks to see it inline
  ("show me", "print it"), read the file and relay it then; otherwise let them
  review it in the file directly.
- **Every prompt gets routed, not just the first one.** No matching
  `specs/<NNN>-<slug>/` folder → Step R fresh. Mid-session on a feature in
  progress → the resume diff-check ("Resuming an in-progress feature" below)
  before anything else. A feature that already finished — every document
  `Approved`, every story cleared Phase 5 — is not exempt: it goes through
  "Reopening a completed feature" below. Never skip straight to editing code
  because the request "sounds small," "sounds urgent," repeats something said
  earlier, or because every gate already shows `Approved` — routing and the
  diff-check determine that, not your own in-the-moment judgment.

## Before starting

Confirm `templates/spec.template.md`, `templates/plan.template.md`,
`templates/tasks.template.md`, `templates/decision-log.template.md`, and
`templates/learnings.template.md` exist at the project root. If not, **stop**
— tell the user to copy `templates/` from this kit first. One source of
truth: the project root, not this skill.

## Resuming an in-progress feature

If `specs/<NNN>-<slug>/` already exists: **resume, don't start** — Step 0
refuses to overwrite by design.

0. **Diff the triggering prompt against what's already drafted, before
   anything else** — including before touching any code. Check `spec.md`,
   `plan.md`, and `tasks.md` far enough to answer one question: does this
   prompt describe something already captured there (rephrasing aside), or is
   it introducing something new — a requirement, an acceptance criterion, a
   changed scope, a different approach than what was approved? If it's
   already covered, go to step 1. If it's new, don't hand-apply it: route it
   to whichever phase owns the affected artifact, per the "route fixes to the
   owning phase" guardrail above — a human's new instruction is a finding
   like any other; the source doesn't change who owns the fix. Two capture
   rules before invoking the owning agent, both for the same reason (agents
   run in fresh contexts — a file they can read is a durable input, a remark
   in this conversation is not):
   - **Implementation-specific input** (existing-system context, a candidate
     approach — not a WHAT/WHY change) goes into `research.md` first (create
     from `templates/research.template.md` if it doesn't exist), *before*
     invoking `planner`. Follow that file's own revision-discipline note:
     if the new input contradicts or corrects an entry already there, edit
     that entry in place rather than appending beside it.
   - **A change to a currently-`Approved` document** follows "Reopening a
     completed feature" steps 1-4 below for the exact mechanics — record the
     pending amendment, flip to the literal `Draft` string *before* the
     owning agent touches it, re-gate, cascade. Those mechanics govern
     amending *any* `Approved` document, whether or not the rest of the
     feature is done.
   Get the human's approval on the amended document (flipping its Status and
   logging the gate like any other approval) before resuming below.
1. Read each document's **Status** header (`spec.md`, `plan.md`, `tasks.md`):
   `Draft` = drafted but not yet approved; `Approved` = that gate is cleared. A
   document still full of placeholders hasn't been started.
2. Resume at the first phase whose document is not `Approved`; honour the
   approval gate before moving on. Any `[NEEDS CLARIFICATION]` markers still in
   the documents are the open questions left to settle. **Read that phase's
   reference file (below) before acting** — don't resume from memory of an
   earlier read. One special case: a document at `Draft` that already has an
   approval row for its gate in `decision-log.md` is a **reopen in flight**,
   not an unapproved first draft — a prior session flipped it and was
   interrupted before the amendment landed. Look for its entry under
   `research.md` → Pending Amendments and hand that to the owning agent as
   the amendment input. If there's no entry and the triggering prompt carries
   no detail, the reason for the flip is gone with the old conversation —
   **ask the human what changed**; never re-present the stale body for
   approval as if nothing was pending.
3. Cross-check `decision-log.md` — it carries one committed row per approved gate.
4. If `learnings.md` has entries, skim it before re-invoking `implementor` or
   `debugger` — it may already record why a prior attempt at this story went
   sideways. If it's grown large or repetitive across many prior stories and
   no compaction pass has run recently, this is also a good moment to offer
   one (see `references/phase-5-review.md`'s compaction step) before it's
   handed, unread in full, into another fresh sub-agent context.
5. **Re-read all three Status headers before relying on `decision-log.md`**
   — don't carry step 1's conclusion forward. This step applies only if
   `spec.md`, `plan.md`, and `tasks.md` each read exactly
   `Approved — <who>, <date>`; if even one doesn't, go to step 2 instead.
   Only then is the resume point inside the per-story loop (3.7 → 4 → 5),
   not at a document gate.

   With the precondition actually confirmed, use `decision-log.md`'s row
   history to find the story and its state, and act on it:
   - **No rows at all for a story** → it hasn't started. Invoke `test-writer`
     (Phase 3.7) — never jump straight to `implementor` before tests exist;
     that skips tests-first.
   - **A Tests (red) row but no Implement row** → mid-implementation. Invoke
     `implementor` for that story and let it find the exact task itself, via
     its own step 1 (run each task's test in order; the first one still red
     is where real work resumes, anything already green is prior-session work
     already done) — that check is more reliable than any status field, since
     it's the actual code state, not a claim about it.
   - **An Implement row but no Review row** → awaiting review. Invoke
     `code-reviewer` directly (Phase 5) — the story's implementation is
     already done and logged, so re-invoking `implementor` would be wrong.

   `decision-log.md` narrows this to a story and a phase — don't try to
   derive the exact task from the log too (that's `implementor`'s own job,
   per its step 1, in the mid-implementation branch only). **State what you
   read and what it implies before invoking anything** — e.g. "US2 has an
   Implement row but no Review row → resuming at Phase 5, invoking
   `code-reviewer`." This isn't a new approval gate (Phase 4→5 has never
   required stop-and-ask); it's turning what would otherwise be a silent
   inference into a stated one.

### Reopening a completed feature

A feature can be fully done — every document `Approved`, every story cleared
Phase 5 — and still get a new prompt later ("actually we also need X",
"change Y's behavior"). Steps 1-2 above only find a resume point when *some*
document is still `Draft`; an all-`Approved` set has no such point, and that
is not the same as "nothing left to gate." **The steps below are the general
mechanics for reopening any `Approved` document** — the resume diff-check
(step 0 above) points here for exactly this reason, whether the feature is
fully shipped or still mid-Phase-4:

1. **Never hand-apply the change.** Route it to the artifact's owning agent
   per the "route fixes to the owning phase" guardrail; if it's
   implementation-specific rather than a WHAT/WHY change, capture it in
   `research.md` first, exactly as in the resume diff-check (step 0 above).
2. **Record, then flip.** First write the requested change under
   `research.md` → **Pending Amendments** (create the file from
   `templates/research.template.md` if it doesn't exist) — verbatim enough
   to act on without this conversation, naming the target document. Then
   flip that document's Status from `Approved — <who>, <date>` back to the
   literal string `Draft` — nothing appended (the Status field is a rigid
   enum; see "Approval status" below) — before the owning agent touches it.
   An `Approved` document silently rewritten in place is the one outcome
   this skill must never produce. Recording comes *before* the flip so that
   an interruption between flip and redraft leaves the reason for the flip
   on disk — recoverable by a bare "resume" (see step 2 of "Resuming"
   above) — instead of only in a conversation that no longer exists.
3. **Re-run that phase's gate**: the agent drafts the amendment, you stop for
   explicit human approval (same as any Phase 1-3 gate), then flip Status
   back to `Approved — <who>, <new date>`, append a *new* `decision-log.md`
   row, and delete the Pending Amendments entry — the amendment now lives in
   the approved document, and a cleared entry is what distinguishes "done"
   from "in flight." Don't overwrite the prior decision-log row — the earlier
   approval happened and stays in the audit trail; the amendment is a
   separate, later decision.
4. **Cascade the reopen.** If the change touches `spec.md` or `plan.md`,
   treat every downstream phase whose output it invalidates as reopened too
   — re-run Analyze (3.5) if scope shifted, and send the affected user
   stories back through Tests → Implement → Review (3.7-5) rather than
   assuming prior-green tests or a prior clean review still hold for the
   amended behavior.

## Approval status (the resume signal)

Each document carries a **Status** field in its header — `Draft` until you
approve that gate, then `Approved — <who>, <YYYY-MM-DD>`. This field *is* the
resume state: it records what has been ratified without a separate breadcrumb
file.

- **At each approval gate**, flip the just-approved document's Status from
  `Draft` to `Approved — <who>, <date>` in the same step that appends the
  `decision-log.md` row.
- **On resume**, the first document still at `Draft` (or all-placeholder) is
  where work picks up — see "Resuming an in-progress feature" above.
- The filled-in body shows what's *drafted*; the Status field shows whether it's
  *approved*; `decision-log.md` is the durable, committed audit trail of those
  approvals. No throwaway scratch file is needed.
- **The Status field is a rigid two-value enum: `Draft` or
  `Approved — <who>, <date>` — write it exactly, nothing else appended.**
  Resume detection (`Resuming an in-progress feature`, step 1 above) matches
  this field literally; a third state invented in the moment — extra wording,
  a rationale, a synthesized in-between value — doesn't get recognized as
  unapproved *or* approved, so it can silently pass through gate detection
  instead of stopping it. Any rationale for why a document changed belongs in
  `decision-log.md`, not in the Status field, and — per that file's own
  template — only as a row logged at the moment a gate is actually approved,
  never as an in-progress or pending row.
- **Never add a changelog / revision-history section to `spec.md`, `plan.md`,
  or `tasks.md`.** The canonical templates carry no such section — the Status
  header plus `decision-log.md` are the *only* two places this workflow
  records approval state, deliberately, so there's exactly one field to check
  on resume. (`research.md`'s Pending Amendments slot carries the *content*
  of an in-flight amendment, not state — "in flight" is still read off
  Status + decision-log.) A per-document changelog invented ad hoc creates a
  second, unchecked place for status to live, which is exactly how a document
  can look "handled" to a human skimming it while gate detection never sees
  it.

## Step R — Route the work (right-size before you scaffold)

Before scaffolding, **propose a track**.
*You recommend; human decides.* Never pick silently.

Propose exactly one track with a one-line rationale and the artifacts you'll produce:

- **Track A · Trivial — Direct change.** Trivial, localized, no design choices: a typo,
  copy/comment edit, config value, dependency bump, obvious one-liner. *No
  feature folder, no spec, no tasks.* Make the change; if it touches behaviour,
  write and confirm a failing test first, then make it pass. When done, invoke
  the `code-reviewer` agent on the diff (no spec path — it reviews against
  `AGENTS.md` and `memory/constitution.md` only). Capture the rationale in the
  commit message.
- **Track B · Simple — Patch.** A localized bug fix or small enhancement with no new
  architecture. Scaffold the folder, write a **short `spec.md`** (problem +
  acceptance + **unchanged-behavior / regression guard** + out-of-scope) and
  `tasks.md`; **skip `plan.md`** unless a design decision surfaces. Tests-first —
  for a bug fix, that includes a regression test for each unchanged-behavior
  invariant (write it first, confirm it stays green) plus a test that fails on
  the bug and passes once fixed.
- **Track C · Moderate — Feature (default).** A normal new capability. Full Specify → Plan →
  Tasks at standard depth. This is the default when you're unsure between B and C.
- **Track D · Complex — Architecture / brownfield.** A new service, a cross-cutting change,
  or modifying untested legacy code. Full pipeline at maximum depth: add
  `data-model.md` as needed, use the strongest model
  (see `AGENTS.md` Model Routing), offer **characterization tests first** for any
  legacy area (ask-first — the human decides at Phase 3.7, never auto-run),
  and record the cross-cutting decision as an **ADR** under
  `docs/adr/` (the decision log gets a one-line pointer to it).

`research.md` isn't Track-D-exclusive, unlike the rest of this list — it's
created on **any** track the moment the triggering prompt (or a later human
message) hands you existing-implementation context or a candidate approach
worth recording, so that content has somewhere to live besides `spec.md`
(WHAT/WHY-only) or your own context (gone once the phase ends). See Step 0's
"Immediately after scaffolding" for what goes in it and when.

In the same turn:

1. **Scan for opt-in extensions.** List every `*.opt-in.md` under
   `.agents/extensions/`, present each opt-in question. Don't load full rules
   yet — only the small prompts. No `*.opt-in.md` = always enforced; note it.
2. **Stop for route approval.** Present: track + rationale, artifacts, extension
   opt-in choices. Wait for confirmation before scaffolding.
3. After approval, record only the opted-in **pack IDs and their rule-file
   paths** (e.g. `security/baseline` →
   `.agents/extensions/security/baseline/security-baseline.md`) — never read
   the full pack rules into this skill's own context. This orchestrator routes
   extensions, it does not enforce them: each downstream agent (`specifier`,
   `planner`, `task-decomposer`, `test-writer`, `artifact-analyzer`,
   `code-reviewer`) has its own `Read` tool and is passed the pack path(s), not
   the rule text, so it loads and checks the rules itself, in its own context,
   at its own phase.

Record the approved track and extension choices (pack IDs + paths, not rule
text) as the first entries in `decision-log.md` immediately after
scaffolding — every later phase resolves the full rules from that log entry,
not from anything carried in this session's context.

**Track A**: no folder, no further phases — implement, then invoke `code-reviewer` on the diff.
**Tracks B/C/D**: continue to Step 0.

## Step 0 — Scaffold (mechanical — don't use judgment here)

Before running the scaffold script, check whether a matching feature folder
already exists under `specs/` — don't blind-scaffold a duplicate. **Read
`references/step-0-scaffold.md` now** for the existing-feature check, the
exact scaffold-script invocation (path pitfalls, OS variants, manual fallback),
and the immediately-after-scaffolding checklist (decision-log rows, Track B's
`plan.md` deletion, `learnings.md`).

## Phases (read the reference file at phase entry)

Each row below is a one-line map, not the protocol. **Before starting a
phase, `Read` its reference file in `references/`** — the full step-by-step
detail lives there, not here, so it stays out of context until the moment
it's actually needed (and gets a fresh, undecayed read every time you re-enter
that phase, even late in a multi-day session).

| Phase | Gate | Reference file |
|---|---|---|
| 1 — Specify | `specifier` drafts `spec.md`; human approves | `references/phase-1-specify.md` |
| 2 — Plan | `planner` drafts `plan.md`; human approves | `references/phase-2-plan.md` |
| 3 — Tasks | `task-decomposer` drafts `tasks.md`; human approves | `references/phase-3-tasks.md` |
| 3.5 — Analyze | `artifact-analyzer` cross-checks; loops to clean verdict or logged skip | `references/phase-3.5-analyze.md` |
| 3.7 — Tests (red) | `test-writer` writes failing tests per story | `references/phase-3.7-tests.md` |
| 4 — Implement (green) | `implementor` (+ `debugger` on escalation; + `lean4-theorem-writer`/`tlaplus-spec-writer` + matching verifier on a story's formal obligation) | `references/phase-4-implement.md` |
| 5 — Review & commit | `code-reviewer` (+ `debugger`/`implementor`/`test-writer`/formal-obligation writer loop on Blockers, by `Kind`); human commits; offers a `learnings.md` compaction pass | `references/phase-5-review.md` |

Phases 3.7 → 4 → 5 repeat **per user story**, in `tasks.md`'s priority order,
until the last story clears Phase 5 — see `phase-3.7-tests.md` for how that
loop is sequenced. When the last story clears Phase 5, the feature is
complete. If any doc (`README.md`, `AGENTS.md`, a glossary) now describes
something inaccurately, offer the `docs-writer` agent — it edits docs only,
never application code.
