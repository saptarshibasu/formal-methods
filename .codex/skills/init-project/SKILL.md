---
name: init-project
description: "Use when setting up a project's AI agent configuration from scratch — triggers: \"initialize the project\", \"set up AGENTS.md\", \"bootstrap project config\", \"create project constitution\". Reads the codebase, then produces a filled-in AGENTS.md and memory/constitution.md with explicit approval before writing. One-time setup that precedes any develop-feature work."
---

# Init Project

Bootstraps agent config by scanning the codebase and producing:

1. `AGENTS.md` — from `templates/agents.template.md`, filled with real project
   values; loaded into every agent session
2. `memory/constitution.md` — from `templates/constitution.template.md`,
   universal always-true principles for this project

`AGENTS.md` is generated output — template lives in `templates/agents.template.md`,
never edited at root. Re-run to regenerate after significant changes.

Three gated phases: **Investigate → Draft AGENTS.md → Draft Constitution**.
Never skip a gate or merge phases.

## What goes where (keep this distinction sharp throughout)

| Belongs in **AGENTS.md** | Belongs in **constitution.md** |
|---|---|
| Commands (build, test, lint, run) | Architectural principles (always true) |
| Tech stack and versions | Test-first mandate |
| Project structure | Simplicity / anti-abstraction rules |
| Git/PR workflow | Security and compliance policies |
| Model routing | Quality gates for every PR |
| Performance idioms specific to this stack | Governance / amendment process |
| Brownfield area details | |

If only sometimes true or feature-specific: AGENTS.md or a spec — not the
constitution.

## Behavioral guardrails (apply throughout this skill session)

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
- **Read before asking.** Every question you raise should be something you
  genuinely cannot answer from the files.
- **Approval before writing.** Do not write either file until the user
  explicitly approves the draft.
- **No over-populating.** Short and accurate beats long and generic. Push back
  on conditional or feature-specific content that doesn't belong in these files.

## Before starting

1. Confirm both templates exist:
   - `templates/agents.template.md` — template for AGENTS.md
   - `templates/constitution.template.md` — template for the constitution
   If either is missing, stop and tell the user.
2. Check whether a real `AGENTS.md` already exists and looks filled in (not
   the stub). If it does, ask: **regenerate from scratch** or **amend**?
3. Check whether `memory/constitution.md` (or any constitution path referenced
   in the existing AGENTS.md) already exists. Same question if so.

---

## Phase 1 — Investigate

Before asking anything, scan the project. Look for:

- **Package / build files**: `package.json`, `pyproject.toml`, `pom.xml`,
  `Cargo.toml`, `go.mod`, `Gemfile`, `build.gradle`, etc. — extract language,
  runtime version, framework, key dependencies.
- **Test infrastructure**: test directories (`tests/`, `spec/`, `__tests__/`),
  test runner config (`pytest.ini`, `jest.config.*`, `vitest.config.*`).
- **CI/CD**: `.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile` — extract
  what commands are run in CI (build, lint, test).
- **Project structure**: top-level directories and what they appear to contain.
- **Existing docs**: `README.md`, `docs/`, `ADR/`, any architecture notes.
- **Git config**: branch protection hints, PR templates (`.github/`).
- **Existing AGENTS.md**: note which sections are filled vs. still placeholder.

After scanning, summarise findings and remaining gaps. Group questions by
AGENTS.md section; work 2–3 at a time.

**Key questions to resolve per section (ask only what you can't infer):**

*Commands* — you likely found build/test/lint commands in CI or package files;
confirm edge cases (single-test invocation, local run command). Also confirm
a **coverage command that produces a per-file report** (not just an aggregate
number) and a **static analysis / cognitive-complexity command** (Sonar-style
analyzer if one's configured, otherwise whatever linter covers code smells) —
`code-reviewer`'s deterministic gates need both; if the project genuinely has
neither, note that explicitly rather than leaving the line as a placeholder.

*Tech Stack* — confirm specific versions if not in lockfiles; ask about key
libraries that shape how code should be written (ORM, HTTP client, state
management, etc.).

*Project Structure* — describe what you see; ask the user to correct or
annotate anything ambiguous.

*Code Style* — ask for a real example snippet that shows the preferred style,
or ask where existing idiomatic code lives so you can reference it.

*Git / PR Workflow* — branch naming pattern, commit message format, PR
requirements (CI gate, minimum reviewers, issue link).

*Boundaries* — ask what agents should always do without asking, what requires
human sign-off, and what's a hard stop. Give examples to make the question
concrete.

*Performance & Efficiency* — ask what the highest-frequency bulk operation is
(DB writes, API calls, batch jobs) and whether there's a known idiom for it.
Ask if there's a null/empty-check utility they want used consistently.

*Model Routing* — ask which model tiers are available and which phases should
use which tier (Specify/Plan vs. Tasks/routine vs. quick edits).

*Brownfield areas* — ask if there are areas with no test coverage where agents
should write characterization tests before touching anything.

Once you have enough to fill in the template accurately, move to Phase 2.

---

## Phase 2 — Draft AGENTS.md

Fill in the template from investigation and user answers:

1. Replace every placeholder with a real value; remove inapplicable sections.
2. Remove all HTML comments (`<!-- ... -->`).
3. Remove sections that don't apply rather than leaving them with placeholder text.
4. Performance & Efficiency is high-leverage — include even if only the
   bulk-operation idiom and null-check convention. Agents get these wrong most.
5. Self-check: does each line teach something unguessable from training? True of
   any project → cut it.

**Stop. Write the draft to `AGENTS.md.draft` (project root), not to chat.**
Printing the full document into the conversation burns roughly 3x the tokens
of writing it once to disk. In chat, give only a short summary — section list,
line count, and every `[NEEDS CLARIFICATION]` marker or judgment call — and
point the user to the draft file to read. Ask for explicit approval before
writing anything to its final path. If the user requests changes, edit the
draft file and repeat the summary; do not paste the revised draft into chat.

---

## Phase 3 — Draft Constitution

Only after the user has approved the AGENTS.md draft.

Use `templates/constitution.template.md`. Phase 1 covered most of the project —
ask only what remains unresolved:

**Article I — Architecture pattern:**
> "What is the primary architectural boundary for new features — do they start
> as standalone modules before being wired in, or is there a different pattern
> you enforce?"

**Article II — Observability:**
> "How should components expose their behaviour — CLI interfaces, admin
> endpoints, structured logs? Is there a standard you want on every feature?"

*(Articles III and V are pre-populated in the template — confirm the user
wants them as-is or amended.)*

**Article IV — Testing strategy:**
> "Should tests run in isolation — mocks/stubs/fakes at the external
> boundary — rather than against real services or databases? Are contract
> tests required before any cross-boundary implementation?"

**Additional constraints:**
> "Any security, compliance (GDPR, HIPAA, SOC 2), or performance floors that
> apply to every feature without exception?"

**Quality gates:**
> "What must be true for every PR — specific CI checks, minimum reviewers,
> and two numbers `code-reviewer` will enforce on every review: a **test
> coverage floor** (a percentage — 90-95% is a common range, 92% if you don't
> have a strong opinion) and a **static analysis / cognitive-complexity
> threshold**, if the stack's analyzer supports one?"

**Governance:**
> "Who owns the constitution and has authority to amend it?"

Same filter as AGENTS.md: only-sometimes-true belongs in AGENTS.md or a spec.

Once drafted:
1. Fill in the template, replacing all bracketed placeholders.
2. Remove every HTML comment (`<!-- ... -->`).
3. Remove any Article whose placeholder was never filled and isn't needed.
4. Set **Version** to `1.0.0`, **Ratified** to today's date.

**Stop. Write the draft to `constitution.md.draft` (project root, or next to
the intended save path), not to chat.** Same reasoning as Phase 2: writing it
once to disk beats printing it. In chat, give only a short summary — Article
list and every `[NEEDS CLARIFICATION]` marker or judgment call — and point the
user to the draft file. Require explicit approval before writing either file
to its final path. If the user requests changes, edit the draft file and
repeat the summary; do not paste the revised draft into chat.

---

## Writing files (after both are approved)

1. **AGENTS.md** — move/rename `AGENTS.md.draft` to the project root as
   `AGENTS.md`, replacing the stub.
2. **constitution.md** — confirm save path (default: `memory/constitution.md`);
   create `memory/` if needed, then move/rename `constitution.md.draft` there.
3. Delete any leftover `.draft` file once its final copy is written.
4. Verify AGENTS.md's Specs section references the correct constitution path.
5. Write or update the root `CLAUDE.md` so its only content is two import
   lines: `@AGENTS.md`, then `@memory/constitution.md` (use the confirmed
   constitution path). Claude Code does not read `AGENTS.md` natively and
   does not follow markdown links or prose "read this file" instructions at
   session start — only `@path` imports are inlined at launch. Codex and
   Copilot read `AGENTS.md` natively, so they need no pointer;
   `.github/copilot-instructions.md` may remain a thin markdown pointer for
   older Copilot surfaces. Both pointer files must stay at ≤2 real lines.

Then confirm to the user:
- Both files written and their paths
- Constitution version and ratification date
- Note: `AGENTS.md` needs no mirroring — `CLAUDE.md` imports it and
  `.github/copilot-instructions.md` points to it;
  the mirror scripts propagate agents/skills, not this file.

## What this skill does not do

- Doesn't run `develop-feature` — that's a separate workflow for individual
  features.
- Doesn't fabricate values — unresolvable facts get `[NEEDS CLARIFICATION]`.
- Never writes either file until the user explicitly approves both drafts.
