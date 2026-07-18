---
name: sync-agents-md
description: "Use when re-syncing AGENTS.md after the project has drifted — triggers: \"update AGENTS.md\", \"resync AGENTS.md\", \"is AGENTS.md still accurate\", \"audit AGENTS.md for drift\". Corrects AGENTS.md from repo evidence, requiring explicit approval before writing. Initial generation is init-project's job; constitution changes go through amend-constitution."
---

# Sync AGENTS.md

Re-syncs `AGENTS.md` against the current repo state.
Diffs existing file against reality, proposes corrections, preserves human prose.
Nothing written until approved.

## Why this skill exists

As commands, deps, and structure shift, AGENTS.md goes stale silently. Stale is
worse than empty — every session trusts it. Re-grounds it in evidence.

## Behavioral guardrails (active for the entire session)

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
- **Evidence or nothing.** Every line written into AGENTS.md must trace to a
  file you actually read (a build file, lockfile, CI workflow, the directory
  tree, an existing doc). If you cannot ground a fact, do **not** write a
  plausible-sounding one — write `[NEEDS VERIFICATION: what to check and where]`.
  This is the same no-fabrication rule the constitution applies to cross-repo
  signatures.
- **Respect AGENTS.md's own ground rule.** Every line should be something an
  agent could NOT infer from training. Do not add generic advice ("write clean
  code," "follow best practices") — if a line would be true of any repo, leave
  it out. Prefer deleting a section to filling it with filler.
- **Never thicken the pointer files.** Project facts go in AGENTS.md only.
  Never write conventions into `CLAUDE.md` or `.github/copilot-instructions.md`
  — the pre-commit hook and CI block those from growing past a pointer.
- **Stay in lane.** Universal principles → `amend-constitution`, not here.
  Feature requirements → `spec.md`, not here. Path-scoped subtree rules →
  `.github/instructions/*.instructions.md`, not AGENTS.md.
- **Preserve human prose on resync.** Treat existing filled-in text as
  authoritative unless the repo contradicts it. Touch only the lines that have
  actually drifted; never rewrite a section wholesale to "tidy" it.

## Before starting

1. Confirm `AGENTS.md` is filled in, not the stub — if stub, tell user to run
   `init-project` first.
2. Read `memory/constitution.md` (or the path AGENTS.md references) — don't
   duplicate constitution material.

## Evidence sources (read before writing anything)

Detect the ecosystem — don't assume it.

- **Commands & tech stack:** `package.json` scripts, `pom.xml` /
  `build.gradle`, `pyproject.toml` / `Makefile` / `tox.ini`, `go.mod`,
  `Cargo.toml`, `Gemfile`, etc. Take the *exact* build/test/lint/run commands
  with their flags — AGENTS.md's Commands section is referenced constantly and
  must be exactly right.
- **Versions & key libraries:** lockfiles and manifest version pins. Record
  specific versions, not categories ("React 18 + Vite + Tailwind," not "React
  project").
- **Project structure:** the real top-level directory tree (`git ls-files`,
  not a guess). Describe what actually lives in each path.
- **Git / PR workflow:** branch names and commit-message shape from `git log`,
  plus `CONTRIBUTING*`, PR templates, and CODEOWNERS if present.
- **CI / required checks:** `.github/workflows/*` (and this kit's
  `agent-harness.yml`) for what blocks a merge.
- **Boundaries & brownfield areas:** directories with no tests, generated
  code, vendored dirs — candidates for the ⚠️ Ask-first / 🚫 Never tiers and
  for characterization-test flags.
Note every fact's source so the draft can cite it.

## Resync

1. Re-derive facts from the evidence sources above.
2. Build a **drift report**: section · current claim · actual · proposed edit.
3. Flag — don't auto-change — anything ambiguous or that may reflect a deliberate
   human decision. Ask the user.
4. Edit only drifted lines — never rewrite a section wholesale.

## Approval gate

**Stop. Present the drift report and ask for explicit approval**, resolving any
`[NEEDS VERIFICATION]` markers. Call out every judgement call. Write only after
approval.

## Verify before finishing

After writing:

- No leftover `[bracketed placeholders]` in committed AGENTS.md (except accepted
  `[NEEDS VERIFICATION]` markers).
- Each command in Commands exists in its source. Where cheap, `--help` to confirm.
  Don't run build/test suites.
- No project facts in `CLAUDE.md` or `.github/copilot-instructions.md`.

## What this skill does not do

- Doesn't amend the constitution (`amend-constitution` does that).
- Doesn't populate `spec.md` / `plan.md` / `tasks.md`.
- Doesn't fabricate facts — unverifiable lines are marked, not guessed.

## Keeping it current

Offer to schedule a resync cadence or CI warning after completing successfully.
