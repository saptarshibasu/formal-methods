<!--
  TEMPLATE — copy this file to your repo root as `AGENTS.md` and fill in every
  bracketed placeholder with a fact that is TRUE and SPECIFIC to this repo.
  Delete any section that doesn't apply rather than leaving it generic.

  Ground rule (the single most important one): every line in this file should
  be something the agent could NOT already infer from training data. Generic
  advice ("write clean code," "follow best practices") wastes context budget
  and trains the agent to skim the rest of the file. If a line would be true
  of any repo, delete it.

  Section ordering for KV cache efficiency: put the most-stable sections
  first (Commands, Tech Stack, Project Structure) and the most-conditional
  ones last (Boundaries, Brownfield areas). Most model APIs cache an unchanged
  prompt prefix at ~90% discount. An edit to a late section keeps the stable
  prefix cached; an edit to an early section invalidates the cache for
  everything that follows.

  Once you've filled in (or deleted) a section below, delete its instructional
  comment too — including this one. None of these comments belong in the file
  you actually commit: AGENTS.md is read into every single agent session, so a
  comment explaining the template is tokens spent on something the agent never
  needed at runtime. Keep only the real, filled-in content.
-->

# AGENTS.md — [PROJECT NAME]

## What this is

[One or two sentences: what this repo/service does, and where it sits in the
larger system if there is one. e.g. "Java 17 / Spring Boot 3 service owning
the Order domain. Part of a multi-repo system — this repo's source is the
only source an agent here can see by default."]

## Always-on context

Before acting on any task, read `memory/constitution.md`. It contains the
project's non-negotiable principles — test-first, simplicity, isolation,
and any project-specific rules ratified by the team. Every agent session inherits
these; they are never optional and are not repeated in this file.

## Commands

<!-- Full runnable commands with flags, not just tool names. The agent will
     reference these constantly — get them exactly right. -->

- Build (clean): `[exact command, from a clean state — e.g. after removing
  build artifacts/caches, if that differs from an incremental build]`
- Test (all): `[exact command]`
- Test (single file/case): `[exact command with placeholder]`
- Test (quiet, agent-preferred): `scripts/quiet.sh [exact test command]` —
  condenses output to pass/fail + first relevant error; agents should run
  build/test through this form to keep raw logs out of context
- Coverage (with report): `[exact command producing a per-file coverage
  report, not just an aggregate number — code-reviewer's coverage gate needs
  to name uncovered lines/branches, not just the overall percentage]`
- Lint/format: `[exact command]`
- Static analysis / complexity: `[exact command — a Sonar-style code-smell /
  cognitive-complexity analyzer if the stack has one, otherwise whichever
  linter/analyzer config covers it; delete this line only if the stack
  genuinely has no such tool]`
- Run locally: `[exact command]`
- [Anything else run routinely, e.g. migrations, codegen]

## Tech Stack

<!-- Be specific: versions and key libraries, not just the category.
     "React 18 with TypeScript, Vite, Tailwind" beats "React project." -->

- [Language/runtime + version]
- [Framework + version]
- [Key libraries that shape how code should be written]
- [Database/storage]

## Project Structure

<!-- Where things live, stated explicitly. -->

- `[path/]` — [what lives here]
- `[path/]` — [what lives here]
- `[path/]` — [what lives here]

## Code Style

<!-- One real snippet showing your style beats three paragraphs describing it.
     Only put rules here if they're true repo-wide. A rule that only applies
     to one subtree (e.g. "components in src/frontend/** use this prop-naming
     convention") does NOT belong here — it costs tokens on every session,
     including the ones that never touch that subtree. Create
     `.github/instructions/<name>.instructions.md` with an `applyTo` glob
     instead. -->

```[language]
[A short, real example from this codebase that shows naming conventions,
formatting, and the idioms you actually want repeated.]
```

- [Naming convention not obvious from the snippet]
- [A style rule you've had to correct more than once]

## Git / PR Workflow

- Branch naming: `[pattern]`
- Commit message format: `[pattern]`
- PR requirements: `[e.g., must pass CI, must include a linked issue, must be under N lines]`

## Boundaries

<!-- Three tiers, not a flat list. This is the single highest-leverage
     section in the whole file — get specific here even if you keep
     everything else short. -->

**✅ Always** — do this without asking:
- [e.g., "Run tests before committing"]
- [e.g., "Follow the naming conventions in the style guide"]

**⚠️ Ask first** — high-impact but not categorically forbidden:
- [e.g., "Modifying the database schema"]
- [e.g., "Adding a new dependency"]
- [e.g., "Changing CI/CD configuration"]
- Applying an `implementor`/`debugger`-proposed `AGENTS.md` correction —
  they propose (a command that turned out wrong), a human approves, only
  then does the file change. Never auto-applied.

**🚫 Never** — hard stops, no exceptions:
- [e.g., "Edit `node_modules/` or `vendor/`"]
- [e.g., "Merge to main without a passing CI run"]
- [Add any repo-specific hard stops here. Note: "commit secrets" and
  "delete a failing test" are already in the constitution — don't
  duplicate them here unless you want to call them out for emphasis.]

## Conventions (rule + reason)

<!-- "No field injection" doesn't generalize. "No field injection — it broke
     our ability to unit-test without a Spring context" does, because the
     agent can apply the underlying reasoning to cases this file didn't
     explicitly anticipate. State the reason, briefly, every time.

     Same subtree-scoping caveat as Code Style above: if a rule is scoped to
     one directory or glob rather than the whole repo, it belongs in
     `.github/instructions/<name>.instructions.md`, not here. Keep this
     section to conventions that hold everywhere. -->

- [Do/Don't] [specific practice] — [short reason, ideally tied to a real incident or constraint]
- [Do/Don't] [specific practice] — [short reason]

## Performance & Efficiency

<!-- Don't skip as "obvious": a study of 2,303 agent context files found a
     Performance section in only 14.5% (vs 62-70% for build/architecture), yet
     it measurably changes output. Models default to the most common shape in
     training — for "do X to each item" that's usually a naive per-item loop —
     unless something signals the operation's grain is a design decision. State
     your real idioms; "be efficient" alone won't. A real before/after snippet
     beats prose here. -->

- **Batch/bulk over row-by-row — this is the highest-frequency, highest-cost
  pattern to call out explicitly.** [Name your stack's actual bulk idiom, e.g.
  "use `bulk_create`/`bulk_update`, a single `UPDATE ... WHERE id IN (...)`, or
  `executemany` — not a loop issuing one query/request per item"] — [reason,
  e.g. "a per-row loop against the `orders` table has caused production
  slowdowns before; bulk operations are typically 3-100x faster depending on
  volume"]. Show one real before/after snippet from this codebase if you have
  one — it generalizes far better than the rule alone.
- **Name your null-safety / defensive-check convention, don't let one get
  invented per file.** [e.g. "Use `StringUtils.hasText(x)` / `ObjectUtils.isEmpty(x)`
  (Spring), `Optional`/null-safe operators (Kotlin, Java 8+), or this project's
  own null-handling utility — not a hand-rolled `x != null && !x.isEmpty()`
  check"] — [reason, e.g. "consistency across N files, and the utility already
  handles edge cases a bespoke check tends to miss"].
- [Add any other recurring inefficiency you've actually seen an agent introduce
  in this repo — e.g. N+1 queries through a specific ORM relationship, missing
  an existing cache layer, re-fetching data already available in scope — the
  same "real incident + reason" format as the Conventions section above.]
- If you have a mechanical way to catch a regression (a query-count assertion,
  a framework-specific N+1 detector like Bullet/Prosopite, a benchmark gate in
  CI), name it here and treat it as the actual enforcement — an agent re-reading
  a prose rule every session is a weaker guarantee than a test that fails.

## Domain Language

<!-- Only if your domain has terms that are easy to conflate. Keep this
     section small — a pointer to a glossary file beats inlining 50 terms. -->

[Term A] and [Term B] are distinct concepts, not synonyms — [one-line
distinction]. Full glossary: `[path/to/glossary.md]`. If a term you're about
to use isn't in that glossary, ask rather than guessing at its meaning.

## Specs

<!-- See templates/spec.template.md, plan.template.md, tasks.template.md,
     constitution.template.md in this same templates/ folder. -->

- Feature specs live in `specs/<NNN-feature-name>/{spec.md, plan.md,
  tasks.md}`. **Always populate each from its matching file in `templates/`
  (`templates/spec.template.md`, `templates/plan.template.md`,
  `templates/tasks.template.md`) — do not invent a different structure for
  any of the three, not just the spec.** Mark anything ambiguous with
  `[NEEDS CLARIFICATION: ...]` rather than guessing.
- Each feature also gets `specs/<NNN>/learnings.md` (from
  `templates/learnings.template.md`) — an append-only, ungated scratchpad,
  not a fourth gated document. `implementor` and `debugger` read it before
  starting a story and append discoveries as they go, so a gotcha found mid-
  story survives the fresh-context boundary into the next story or session.
- To start a new feature, use the `develop-feature` skill rather than
  creating `specs/<NNN>/` by hand — it scaffolds the folder and templates
  for you and enforces the Specify → Plan → Tasks approval gates.
- To resume or amend an in-progress (or completed) feature, also go through
  `develop-feature` — never edit code directly from a "resume <feature>" or
  "change X in feature NNN" prompt. The skill diffs the prompt against the
  approved `spec.md`/`plan.md`/`tasks.md` first and routes anything new to
  the owning phase for re-approval before implementation.
- That skill first proposes a **workflow track** (A direct / B patch / C feature
  / D architecture) to right-size the pipeline — approve or override it; don't
  let it pick the depth silently. It also scans `.agents/extensions/` for opt-in
  rule packs (e.g. `security/baseline`) and records the track, opt-ins, and each
  approval in the feature's committed `decision-log.md`.
- Read the spec for the feature you're touching before implementing. Specs
  are not auto-loaded into every prompt — pull in the one relevant to your
  current task explicitly.
- Project-wide, always-true principles (not specific to any one feature) live
  in `[memory/constitution.md or wherever you keep it]` — see that file
  before architecture-level decisions, not just the current feature's spec.
  To create the constitution, use `init-project`. To amend it later, use `amend-constitution`.
- Architecture decisions are recorded in `docs/adr/` — check for an existing
  ADR before changing a cross-cutting pattern.

## Testing Discipline

<!-- The core TDD mandate (test-first, never weaken a failing test,
     characterization tests for brownfield) lives in the constitution at
     [memory/constitution.md or wherever you keep it]. Keep this section
     for repo-specific testing details only — framework idioms, test
     locations, known slow suites, etc. -->

- The project's test-first mandate and "never weaken a failing test" rule
  are defined in the constitution — read that before writing or changing
  any test.
- [If this repo has brownfield/legacy areas:] `[area]` has no test
  coverage — write characterization tests capturing *current* behavior
  before changing anything there. The constitution's Article III defines
  the process.
- Test locations: `[e.g., unit tests in tests/unit/, contract tests in tests/contract/]`
- [Any framework-specific idiom worth naming — e.g. "Use pytest fixtures,
  not setUp/tearDown." or "Mock at the service boundary, not inside
  services."]

## Multi-Repo / Cross-Boundary Notes

<!-- Delete this whole section if this repo is self-contained. Keep it if
     any class/contract/schema this repo depends on lives somewhere this
     agent cannot see by default. -->

- [Dependency name] (`[group:artifact / package]`, version pinned in
  `[file]`) is NOT defined in this repo — it comes from `[other repo]`. Before
  assuming a field/method doesn't exist, check the resolved version and
  inspect the actual sources, don't guess from a similarly named class
  elsewhere.
- A contract snapshot of how this repo consumes that dependency is mirrored
  at `[path]` — do not hand-edit it; if it looks stale, the sync job is
  broken, not the file.
- **Never guess or fabricate class names, field names, method signatures, or
  return types for a dependency you cannot see.** Instead, resolve the source
  in this order and stop at the first that succeeds:
  1. **Sibling repo** — look for the dependency's source at `../[repo-name]/`
     (one level up from this repo's root). If found, read directly from there.
  2. **Source jar** — download the `-sources.jar` for the pinned version and
     extract it to `[e.g., target/dependency-sources/]`. Read the extracted
     `.java` files directly.
  3. **Decompile the binary jar** — run `[e.g., java -jar cfr.jar
     path/to/dep.jar --outputdir target/decompiled/]` and read the output.
     Treat decompiled names as authoritative for field/method signatures.
  4. **Stop and ask** — if none of the above is feasible in the current
     environment, say so explicitly. Do not proceed by inference.

## Model Routing

<!-- Delete this section if your tooling doesn't support per-phase model
     selection, but consider it default-on for any non-trivial project — the
     cost asymmetry is real in both directions. -->

- Specify / Plan phases: use **[your strongest available model]** explicitly
  — do not rely on auto-model-selection for these two phases. A weak model's
  mistake here propagates uncorrected through everything downstream.
- Tasks / routine Implement work: **[mid-tier model]**, or auto-selection, is
  fine.
- Quick edits, file search, formatting, lint fixes: fast/cheap tier, or
  auto-selection.
