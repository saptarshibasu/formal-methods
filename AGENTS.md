# AGENTS.md — formal-methods

## What this is

Java 17 / Spring Boot 3.2 service skeleton — its business domain hasn't been
decided yet, so don't assume or prescribe one. What's already established:
persistence via PostgreSQL/Spring Data JPA, and Lean 4 / TLA+ as a
cross-cutting formal-verification feedback harness for spec-driven
development, used via the framework's coding-assistant agents rather than
run by the service itself (see Article I in the constitution) — that harness
applies to whatever domain gets built here, it isn't the domain. This repo is
also the home of its own spec-driven-development agent/skill kit (`.agents/`,
mirrored to `.claude/`, `.codex/`, `.github/`).

## Always-on context

Before acting on any task, read `memory/constitution.md`. It contains the
project's non-negotiable principles — test-first, simplicity, isolation,
and any project-specific rules ratified by the team. Every agent session
inherits these; they are never optional and are not repeated in this file.

## Commands

- Build (clean): `./gradlew clean build` — compile + test + Checkstyle
  (static analysis/complexity is wired into `check`, which `build` depends
  on; a complexity finding now fails this command, not just a broken
  compile/test). Coverage is **not** part of this command — see below.
- Test (all): `./gradlew test`
- Test (single class): `./gradlew test --tests "<fully.qualified.ClassName>"`
- Test (quiet, agent-preferred): `scripts/quiet.sh ./gradlew test` (or
  `scripts/quiet.ps1` on PowerShell) — condenses output to pass/fail + first
  relevant error; agents should run build/test through this form to keep raw
  logs out of context
- Coverage: `./gradlew jacocoTestCoverageVerification` — JaCoCo; enforces the
  90% overall line-coverage floor (`memory/constitution.md`'s Development
  Workflow). Deliberately **not** wired into `check`/`build` (Jacoco's own
  default), so run it explicitly — `code-reviewer`'s coverage gate does.
  HTML report after a test run: `build/reports/jacoco/test/html/index.html`.
- Lint/format: not configured yet — no Spotless/equivalent formatter.
- Static analysis / complexity: `./gradlew checkstyleMain checkstyleTest` —
  Checkstyle (`config/checkstyle/checkstyle.xml`), cyclomatic-complexity and
  NPath-complexity checks only, Checkstyle's own default thresholds. Already
  covered by plain `./gradlew build` (see above); this form is for running
  it in isolation.
- Run locally: `./gradlew bootRun` (needs a local PostgreSQL — see README)
- Regenerate agent mirrors after editing `.agents/agents/*.md` or
  `.agents/skills/*/SKILL.md`: `bash scripts/mirror-agents.sh` /
  `bash scripts/mirror-skills.sh` (or their `.ps1` twins)

## Tech Stack

- Java 17, Gradle 8.7 (wrapper committed — `./gradlew`, no local Gradle install required)
- Spring Boot 3.2.5 — Web, Data JPA, Validation, Actuator
- PostgreSQL via Spring Data JPA — no schema-migration tool; the schema is
  created manually in the developer's local database, and Hibernate
  (`ddl-auto: validate`) only checks entity mappings against it
- Lombok for entity/DTO boilerplate (`@Getter`/`@Setter`/`@NoArgsConstructor`)
- JUnit 5 + AssertJ + Mockito for tests — no Testcontainers, no embedded DB
  (see constitution Article IV)
- JaCoCo (coverage) + Checkstyle (static analysis/complexity) — see Commands
  above for the exact commands and `memory/constitution.md`'s Development
  Workflow for the floor/threshold each one enforces

## Project Structure

- `src/main/java/com/formalmethods/` — currently just `Application.java`
  (Spring Boot bootstrap); new code goes in a
  `domain`/`dto`/`repository`/`service`/`web`/`config` layer per the
  package-by-layer convention below, not a new top-level package
- `src/main/resources/` — `application.yml`
- `src/test/java/...` — unit tests, package-mirrored to `src/main`
- `.agents/agents/` — canonical coding-assistant agent definitions (single
  source of truth); mirrored to `.claude/agents/`, `.codex/agents/`,
  `.github/agents/` by `scripts/mirror-agents.sh`/`.ps1` — **never hand-edit
  a mirrored copy**, edit the canonical `.md` and re-run the mirror
- `.agents/skills/` — the spec-driven-development workflow skills
  (`develop-feature`, `init-project`, `amend-constitution`, etc.), same
  mirroring rule as agents
- `templates/` — `spec.md`/`plan.md`/`tasks.md`/`constitution.md` templates
  the skills scaffold from
- `docs/` — deep-reference docs (`guardrails.md`, etc.); pulled in by name,
  not auto-loaded into every session
- `scripts/` — `mirror-agents`, `mirror-skills`, `quiet` (test-output
  condenser), all with `.sh`/`.ps1` twins
- `config/checkstyle/checkstyle.xml` — the static-analysis/complexity
  ruleset (Commands above); deliberately scoped to complexity checks only,
  not general style/formatting

## Code Style

- Constructor injection only — no field injection (`@Autowired` on a field),
  anywhere in this codebase.
- Package-by-layer under `com.formalmethods`: `domain`, `dto`,
  `repository`, `service`, `web`, `config` — a new class goes in the layer it
  belongs to, not a new top-level package.
- Lombok (`@Getter @Setter @NoArgsConstructor @EqualsAndHashCode(of = "id")
  @ToString(exclude = ...)`) on JPA entities instead of hand-written
  accessors; exclude JPA-relationship fields from `@ToString` to avoid
  lazy-init/recursion issues.

## Git / PR Workflow

No commits exist yet in this repo, so there is no established convention to
observe — the following is a proposed default, not an inferred fact:

- Branch naming: `<type>/<short-description>` (e.g. `feat/short-description`)
- Commit message format: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, ...)
- PR requirements: `./gradlew build` (compile + test + Checkstyle) passes
  locally, and `./gradlew jacocoTestCoverageVerification` meets the coverage
  floor, before opening a PR

## Boundaries

**✅ Always** — do this without asking:
- Run `./gradlew test` (or `scripts/quiet.sh ./gradlew test`) before
  considering a task done.
- Regenerate agent/skill mirrors (`scripts/mirror-agents.sh` /
  `mirror-skills.sh`) after editing a canonical `.agents/` file, and commit
  the regenerated output alongside the source edit.
- Follow the existing package-by-layer structure for new classes.

**⚠️ Ask first** — high-impact but not categorically forbidden:
- Adding a new Gradle dependency or plugin.
- Changing the Spring Boot or Gradle wrapper version.
- Reintroducing Docker, Testcontainers, or any tool-execution capability
  (e.g. shelling out to `lean`/`lake`/TLC) into this Java service — this
  project deliberately keeps that outside the service; see constitution
  Article I.
- Applying an `implementor`/`debugger`-proposed `AGENTS.md` correction —
  they propose (a command that turned out wrong), a human approves, only
  then does the file change. Never auto-applied.

**🚫 Never** — hard stops, no exceptions:
- Hand-edit a generated file under `.claude/agents/`, `.codex/agents/`,
  `.github/agents/`, or the mirrored `skills/` copies — edit the canonical
  `.agents/` source and re-run the mirror script.
- Add a new `*FeedbackSensorAgent`-style Java class (or any Java code) that
  shells out to an external verification tool — that capability belongs to
  the framework's coding-assistant agents (`lean4-verifier`,
  `tlaplus-verifier`, `lean4-theorem-writer`, `tlaplus-spec-writer`), not to
  this service. See constitution Article I and Article V.

## Specs

- Feature specs live in `specs/<NNN-feature-name>/{spec.md, plan.md,
  tasks.md}`. Always populate each from its matching file in `templates/` —
  do not invent a different structure for any of the three. Mark anything
  ambiguous with `[NEEDS CLARIFICATION: ...]` rather than guessing.
- Each feature also gets `specs/<NNN>/learnings.md` (from
  `templates/learnings.template.md`) — an append-only, ungated scratchpad,
  not a fourth gated document.
- To start a new feature, use the `develop-feature` skill rather than
  creating `specs/<NNN>/` by hand.
- To resume or amend an in-progress (or completed) feature, also go through
  `develop-feature` — never edit code directly from a "resume <feature>" or
  "change X in feature NNN" prompt.
- Project-wide, always-true principles live in `memory/constitution.md` —
  see that file before architecture-level decisions, not just the current
  feature's spec. To amend it, use `amend-constitution`.
- Architecture decisions are recorded in `docs/adr/` (via `create-adr`) —
  check for an existing ADR before changing a cross-cutting pattern.
- If `spec.md` has a Formal Verification Obligations section, `artifact-analyzer`
  checks it during the Analyze phase — before any `.lean`/`.tla` file exists —
  confirming `plan.md` names exactly one tool (Lean 4 xor TLA+) and a concrete
  implementation target per obligation, and that `tasks.md` has a draft task
  (naming the writer agent and file path) plus a verify task (naming the
  verifier agent) for it. It checks that the *path* to the artifact is fully
  laid, not the artifact's content — `lean4-verifier`/`tlaplus-verifier` check
  that later, on the actual file, once it exists.

## Testing Discipline

- The project's test-first mandate and "never weaken a failing test" rule
  are defined in the constitution (Article III) — read that before writing
  or changing any test.
- Test location mirrors `src/main`: a class at
  `src/main/java/.../service/Foo.java` gets its test at
  `src/test/java/.../service/FooTest.java`.
- Mock at the repository boundary (Spring Data JPA interfaces, via
  Mockito) — no real database, no Testcontainers, per constitution
  Article IV.

## Formal Verification Tooling

Neither toolchain is installed in this environment (checked 2026-07-18: no
`lean`/`lake`/`elan` on `PATH`; no `tla2tools.jar` found in the repo or via a
disk-wide search, though that search isn't exhaustive). `lean4-verifier` /
`tlaplus-verifier` will report an **environment error**, not a proof/spec
failure, until one is set up — don't mistake one for the other. When a story
first needs one:

- **Lean 4**: install a toolchain via `elan`
  (https://github.com/leanprover/elan), then scaffold a `lakefile.lean` at
  the repo root (or a dedicated Lake project under `Proofs/`) pinning the
  version via `lean-toolchain`. Once it exists, document the exact build
  command here — `lake build`, or `lake env lean <file>` for a single file —
  so `lean4-verifier` uses it instead of guessing.
- **TLA+**: place `tla2tools.jar` at `tools/tla2tools.jar` (already
  `.gitignore`d — don't commit the jar itself). Once it exists, document the
  exact invocation here — expected shape: `java -jar tools/tla2tools.jar
  -workers auto -config <Spec>.cfg <Spec>.tla` — so `tlaplus-verifier` uses
  it instead of guessing.

Either toolchain lives on the machine running the coding-assistant agent,
**never** as a `build.gradle` dependency or inside the deployed service —
this service doesn't execute verification tools itself (constitution
Article I). Adding one is a local/CI environment setup step, not a Gradle
change.

## Model Routing

- Specify / Plan phases: use the strongest available model explicitly — do
  not rely on auto-model-selection for these two phases.
- Tasks / routine Implement work, `lean4-theorem-writer` /
  `tlaplus-spec-writer` drafting: mid-tier model, or auto-selection, is fine.
- Quick edits, file search, formatting, lint fixes: fast/cheap tier, or
  auto-selection.
