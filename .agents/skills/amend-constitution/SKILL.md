---
name: amend-constitution
description: "Use when amending an existing memory/constitution.md — triggers: \"amend the constitution\", \"update our principles\", \"change our architectural principles\". Asks targeted questions and requires explicit approval before saving. Initial creation is init-project's job; per-feature decisions belong in spec.md, operational facts in AGENTS.md."
---

# Amend Constitution

Updates sections of `memory/constitution.md` using `templates/constitution.template.md`.
Two gated phases: gather principles → ratify. Never skip the gate.

## What belongs in a constitution vs. elsewhere

Keep this distinction clear so output lands in the right file:

| Belongs in **constitution.md** | Belongs in **AGENTS.md** | Belongs in **spec.md** |
|---|---|---|
| Always-true, every feature, every session | Repo-specific facts and operational rules | Feature-specific requirements |
| Architectural principles and their rationale | Commands, tech stack, project structure | User stories, acceptance criteria |
| Universal quality gates (test-first, no secrets) | Git/PR workflow, model routing | Success criteria, edge cases |
| Governance and amendment process | Brownfield area details, performance idioms | Out of scope for this feature |

Only-sometimes-true or feature-specific principles don't belong here.

## Behavioral guardrails (apply throughout this skill session)

<!-- GUARDRAILS:skill -->
<!-- /GUARDRAILS:skill -->
- **No over-populating.** A short constitution enforced consistently beats a
  long one that gets ignored. Push back on conditional or feature-specific
  content that doesn't belong in the constitution.

## Before starting

1. Confirm `memory/constitution.md` (or the path referenced in AGENTS.md's
   Specs section) already exists. If it doesn't, tell the user to run
   `init-project` first — this skill amends an existing constitution, it does
   not create one from scratch.
2. Show the user the current constitution briefly and ask which section(s) they
   want to amend. Skip to just those sections in Phase 1 rather than walking
   every section.
3. Read `AGENTS.md` — note anything there that looks like a universal principle
   that should be in the constitution instead; surface it as a candidate.

## Phase 1 — Gather Principles

Walk sections one or two at a time — don't dump all at once.

### Core Principles (Articles I–V and any custom ones)

The template pre-populates Articles III (Test-First) and V
(Simplicity/Anti-Abstraction) because these are non-negotiable starting
points for spec-driven development. For the others:

**Article I — Architecture pattern:**
> "What is the primary architectural boundary for new features — should each
> feature start as a standalone module/library before being wired into the
> app, or do you have a different pattern you enforce?"

**Article II — Observability:**
> "How should components expose their behaviour for inspection — CLI
> interfaces, admin endpoints, structured logging, or something else? Is
> there a standard you want enforced on every feature?"

**Article IV — Testing strategy:**
> "Should tests run in isolation — mocks/stubs/fakes at the external
> boundary — rather than against real services and databases, or do you use
> a hybrid approach? Are contract tests required before any cross-boundary
> implementation?"

**Additional principles:**
> "Are there any other universal principles you want enforced on every
> feature — performance floors, security mandates, compliance requirements,
> deployment policies?"

For each answer:
- If it's clearly always-true and universal → include it in the constitution
- If it's conditional or feature-specific → note that it belongs in
  AGENTS.md or spec.md instead, and explain why
- If it's unspecified → mark `[NEEDS CLARIFICATION: question]`

**Example of applying the test:**
- *"No secrets in version control"* → always true, every feature, every
  session → **constitution.**
- *"Use an in-memory SQLite database for tests"* → a tooling choice, not a
  universal principle → **AGENTS.md**, not here.
- *"The checkout flow must support Apple Pay"* → one feature's requirement →
  **spec.md**, not here.

### Additional Constraints

Ask specifically about:
- Security / credentials policy (the template already includes a "no
  secrets committed" placeholder — confirm whether this applies as-is or
  needs to be worded differently for their context)
- Compliance or regulatory requirements (GDPR, HIPAA, SOC 2, etc.)
- Non-negotiable performance floors across all features
- Deployment or release policy (who can trigger production deploys, etc.)

### Development Workflow / Quality Gates

> "What gates apply to every PR without exception — required CI checks,
> minimum reviewers, a test coverage floor (a percentage `code-reviewer`
> enforces on every review — 92% is a common default if you don't already
> have one), a static analysis / cognitive-complexity threshold, anything
> else that blocks a merge?"

If the project already has a coverage floor or complexity threshold in
`AGENTS.md`'s Commands section (as a comment or prior convention) but not
here, that's a gap this amendment should close — the floor/threshold itself
belongs in the constitution (always-true, blocks every merge); the command
that measures it belongs in `AGENTS.md`.

Universal gates only — module/feature-type-specific gates belong in AGENTS.md.

### Governance

> "Who owns the constitution and has authority to amend it? What's the
> process — written rationale, review, backwards-compatibility check?"

Fill in the `[whoever owns this]` placeholder from the user's answer, and
set the version and ratification date.

### Draft and self-check

1. Fill in the template, replacing all placeholders.
2. Remove all instructional HTML comments (`<!-- ... -->`).
3. Remove any Article whose placeholder was never filled.
4. Self-check each principle: always-true, every feature, every session?
   "Sometimes" or "it depends" → flag it.

5. **Stop. Present the full draft** and ask for explicit approval or resolution
   of `[NEEDS CLARIFICATION]` markers before writing. Highlight judgement calls.

## Phase 2 — Ratify

Only after Phase 1 approved.

1. Apply review changes.
2. Write amended file to the same path.
3. Increment **minor version** (e.g. `1.0.0` → `1.1.0`); set **Last Amended** to today.
4. Confirm path and version to the user.

## What this skill does not do

- Doesn't populate `spec.md`, `plan.md`, or `tasks.md` — those are feature-specific.
- Never skips the approval gate — unapproved = unratified.
