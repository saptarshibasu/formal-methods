# Extensions — opt-in rule packs

An **extension** is a small, self-contained pack of *blocking* rules that you can
layer on top of the core workflow per feature — security, compliance,
property-based testing, accessibility, whatever your project needs. They are the
kit's answer to "how do I add a class of constraint without bloating `AGENTS.md`
or the constitution, which are read on *every* call?"

Extensions are **read on demand, never auto-loaded** (same discipline as
`docs/`): the `develop-feature` skill scans only the tiny `*.opt-in.md`
prompts at the start of a feature, and the full rules file is pulled in *only if
the human opts that pack in*. A pack you don't use costs zero context.

> Influence: this mechanism is adapted from AWS Labs' AI-DLC extension system
> (MIT-0).

## How a pack is structured

Each pack lives in its own directory under a **category**, and is two files
paired by base name — `<name>` need not match the pack directory (the shipped
example is `security/baseline/security-baseline.md` +
`security-baseline.opt-in.md`):

```
.agents/extensions/
└── <category>/                     # e.g. security, testing, compliance
    └── <pack>/                     # e.g. baseline, property-based
        ├── <name>.md               # the rules (loaded only when opted in)
        └── <name>.opt-in.md        # the opt-in question (always scanned)
```

- **`<name>.opt-in.md`** — a single, structured multiple-choice question shown to
  the human during the Route/Specify stage ("Does this feature touch
  authentication, user data, or external input?"). Small on purpose: it is the
  only part read when a pack is *not* used.
- **`<name>.md`** — the actual rules. Loaded **only** when the human opts in
  (derived by naming convention: strip `.opt-in.md`, the sibling `<name>.md` is
  the rules). Each rule is a heading `## Rule <PREFIX-NN>: <Title>` with a
  **Rule** section (the requirement) and a **Verification** section (concrete,
  checkable conditions). IDs (`SEC-01`, …) are stable and are cited in the
  feature's `decision-log.md` and in `code-reviewer` findings.

A pack **without** a matching `*.opt-in.md` is *always enforced* — use that for
constraints that are non-negotiable for every feature in your repo.

## The lifecycle

1. **Scan** — at feature start, `develop-feature` lists every
   `*.opt-in.md` under this directory and presents each question to the human.
2. **Opt in / out** — the human's choice is recorded in the feature's
   `specs/<NNN>/decision-log.md` (Extensions row). Opt-outs are recorded too, so
   the decision is auditable, not silent.
3. **Enforce** — for each opted-in pack, its rules become **blocking
   constraints**. At every gate (Specify, Plan, Tasks) and during review the
   model must verify compliance before the stage may proceed; an unmet
   **Verification** condition blocks until resolved.
4. **Review** — the `code-reviewer` agent re-checks opted-in rules by ID and
   treats any violation as a **Blocker** (see `.agents/agents/code-reviewer.md`).

This makes an extension a true *harness sensor*: mostly inferential today (the
model checks the Verification conditions), but each rule is written so its checks can be promoted
to a computational sensor — a lint rule, SAST job, or CI gate — when you have
one. Mechanize what you can; infer what you must.

## Adding your own pack

1. Create `<category>/<pack>/` (reuse a category or start a new one).
2. Write `<name>.md`. Give every rule a unique ID `## Rule <PREFIX-NN>: <Title>`
   — the prefix is a short category tag (`SEC`, `A11Y`, `COMPLIANCE`), `NN` is
   sequential. IDs must be unique across *all* packs, since they appear in audit
   logs. Each rule needs a **Rule** section and a **Verification** section.
3. Write `<name>.opt-in.md` (copy `security/baseline/security-baseline.opt-in.md`
   for the format). Omit this file only if the pack must always apply.
4. Keep it short and high-signal — an extension is subject to the same
   token-budget discipline as everything else here.

> The shipped `security/baseline` pack is a **directional reference**, not a
> production security policy. Review, customise, and test rules against your own
> threat model before relying on them.
