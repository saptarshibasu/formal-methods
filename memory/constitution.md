# formal-methods Constitution

<!--
Sync Impact Report:
- Version: 1.0.0 -> 1.1.0
- Modified principles: Development Workflow — replaced the "no numeric
  floor/threshold yet" placeholder with a concrete 90% test-coverage floor
  and a Checkstyle-default complexity threshold, now that JaCoCo and
  Checkstyle are wired into build.gradle
- Added sections: none
- Removed sections: none
- Templates requiring updates: none — code-reviewer.md already reads the
  floor from this file dynamically rather than hard-coding a number;
  AGENTS.md's Commands section updated alongside this amendment with the
  exact commands
-->

## Core Principles

### Article I — Formal-Verification Tool Execution Lives Outside This Service

Lean 4 and TLA+ are used as a cross-cutting, domain-independent feedback
harness for spec-driven development — not evidence of what business domain
this service serves. Whatever that domain turns out to be, this service
never itself invokes an external verification tool (`lean`/`lake`,
TLC/`tla2tools.jar`, or any future tool). Running a proof or model-check
happens exclusively through
the framework's coding-assistant agents (`lean4-verifier`,
`tlaplus-verifier`, and the agents that draft proofs/specs,
`lean4-theorem-writer`/`tlaplus-spec-writer`) — never through a Java class
added to this codebase for that purpose.

**Rationale**: This project already built, and then removed, exactly the
violation this article forbids — a `FeedbackSensorAgent` interface with
`Lean4FeedbackSensorAgent`/`TlaPlusFeedbackSensorAgent` implementations that
shelled out to `lean`/TLC from inside the service, duplicating what the
framework's agents already do. Keeping tool execution out of the deployable
service keeps its dependency surface small (no toolchain to install in
production, no arbitrary external-process execution path) and keeps exactly
one implementation of "run Lean 4" / "run TLC" in the whole project instead
of two that can drift.

### Article II — Every Capability Is Reachable and Observable

A capability that isn't exposed through the REST API and observable via
Actuator (`/actuator/health`, `/actuator/info`) plus structured SLF4J
logging isn't done — no service-internal-only code path ships as "finished."

**Rationale**: The REST layer and Actuator are the only observability
surface this service has; a feature with no reachable endpoint and no log
trail is unverifiable from outside the JVM and untestable the way the rest
of the codebase is tested (contract-level, against the API).

### Article III — Test-First Development

Tests are written before implementation and must fail for the right reason
before any implementation code is written. Every test carries evidence of
its own intent.

Corollaries:

- No implementation code is written before a failing test exists for it.
- Tests are not modified to make them pass; implementation changes to satisfy
  tests, never the reverse, without an explicit, logged exception.
- Every test documents its own intent: a docstring or comment naming the
  acceptance-criterion ID it verifies and a one-line rationale for why the
  test proves that criterion.

**Rationale**: A test written after the implementation tends to describe what
the code does, not what it should do — it inherits the implementation's
blind spots.

### Article IV — Tests Run Isolated, Never Against a Real Database

Unit and service-level tests mock at the repository boundary (Spring Data
JPA interfaces) — never a real PostgreSQL instance, embedded database, or
Testcontainers. This project runs no Docker/container tooling (Additional
Constraints), so no automated test may depend on one being available.
Verifying real PostgreSQL compatibility (schema and entity-mapping
consistency) is done by running the service locally against a developer's
own PostgreSQL instance, not as part of the automated suite.

**Rationale**: The project's explicit no-Docker constraint removes
Testcontainers as an option; mocking at the repository boundary keeps the
suite fast and runnable anywhere Java 17 runs, at the cost of not
mechanically verifying the schema against a real database on every run — a
tradeoff made deliberately, not by omission.

### Article V — Simplicity / Anti-Abstraction

No abstraction, interface, or configuration point is introduced for a
hypothetical future requirement — only for what a current, real requirement
needs. A second concrete implementation is required before extracting an
interface for it.

**Rationale**: This project already paid for one violation of this article —
the `FeedbackSensorAgent` interface (Article I) was built with two
implementations from day one to generalize over "future sensors," when the
actual requirement was already fully served by the framework's agent
definitions. This article exists so that mistake doesn't recur in a
different shape.

## Additional Constraints

- No Docker, container runtime, or Testcontainers dependency anywhere in
  this repository — local PostgreSQL only for anything that needs a real
  database.
- Gradle only — no Maven files (`pom.xml`) or Maven wrapper. The committed
  Gradle wrapper (`./gradlew`) is the only supported build entry point.
- Java 17 language level, enforced via `sourceCompatibility`/
  `targetCompatibility` in `build.gradle` (the build JDK itself may be
  newer).

## Development Workflow

- Every PR must have a local `./gradlew build` (compile + test + Checkstyle)
  pass, and meet the coverage floor below, before it's opened — no CI
  pipeline is configured yet in this repository, so this is presently a
  manual, not an automated, gate.
- **Test coverage floor: 90% overall line coverage**, enforced by JaCoCo's
  `jacocoTestCoverageVerification` (`./gradlew jacocoTestCoverageVerification`
  — see `AGENTS.md`'s Commands section). `code-reviewer`'s coverage gate
  enforces this number on every review.
- **Static analysis / cognitive complexity: Checkstyle's own default
  thresholds** (cyclomatic complexity, NPath complexity —
  `config/checkstyle/checkstyle.xml`), enforced via `./gradlew
  checkstyleMain checkstyleTest`, and already part of `./gradlew build`.
  `code-reviewer`'s static-analysis gate enforces this on every review.

## Governance

This constitution supersedes all other project practices and guidelines.
Amendments require documentation of the change, a migration or compatibility
review, and explicit approval before the new version takes effect.

All PRs and reviews must verify compliance with this constitution. Complexity
introduced beyond these principles must be justified in `decision-log.md`.
Use `AGENTS.md` for day-to-day runtime development guidance; this document
is the constitutional layer it must remain consistent with.

### Amendments

To amend this constitution:

1. Propose the change (what's changing, and why) and reach agreement with the
   project's maintainers.
2. Update this file, filling in the Sync Impact Report at the top.
3. Bump the version per the rules below.
4. Propagate any resulting changes to `AGENTS.md`, templates, and skill
   files so they stay consistent with the new text.

Versioning policy (semantic):

- **MAJOR**: backward-incompatible governance/principle removals or
  redefinitions.
- **MINOR**: a new principle or materially expanded guidance added.
- **PATCH**: clarifications, wording fixes, typo corrections, non-semantic
  refinements.

**Version**: 1.2.0 | **Ratified**: 2026-07-17 | **Last Amended**: 2026-07-18
