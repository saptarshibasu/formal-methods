# formal-methods

Spring Boot service skeleton persisted to PostgreSQL via Spring Data JPA —
its business domain hasn't been decided yet. What's already established is
the spec-driven-development agent/skill kit in this repo, and Lean 4 / TLA+
as a cross-cutting formal-verification feedback harness used via the
framework's coding-assistant agents rather than run by the service itself
(see **Related** below) — that harness is domain-independent, not a preview
of what this service tracks.

## Architecture

The service is currently a bare Spring Boot bootstrap
(`Application.java`) — the domain/API layers that implement job tracking
have not been (re)built yet. See `AGENTS.md`'s Project Structure and Code
Style sections for the package-by-layer convention new code should follow.

## Prerequisites

- Java 17+
- A local PostgreSQL 16 instance (no container runtime required)
- To run a Lean 4 proof or TLA+ model check: run the framework's
  `lean4-verifier` / `tlaplus-verifier` agent from a coding-assistant session
  (needs a Lean 4 toolchain or a `tla2tools.jar` respectively) — this
  service never executes that toolchain itself (constitution Article I).

## Running locally

Install PostgreSQL locally and create the database/role the service expects
(or override the env vars below to point at an existing instance):

```bash
createdb formal_methods
psql -c "CREATE ROLE formal_methods LOGIN PASSWORD 'formal_methods';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE formal_methods TO formal_methods;"
```

Then run the service:

```bash
./gradlew bootRun
```

The service starts on `:8080`. No migration tool manages the schema — create
any required tables in the database yourself; Hibernate only validates that
the entity mappings match at startup.

## Configuration

Datasource settings are environment-overridable (see
`src/main/resources/application.yml`):

| Env var | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/formal_methods` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `formal_methods` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `formal_methods` | DB password |

## API

No REST API is implemented yet — the controller/service/repository layers
described in `AGENTS.md` are pending (re)implementation. Actuator's
`/actuator/health` and `/actuator/info` are reachable once the service is
running (see Article II in the constitution).

## Testing

```bash
./gradlew test
```

No test classes exist yet — see `AGENTS.md`'s Testing Discipline section
for where new tests should live once code is added.

## Related: the kit's formal-methods agents

This repo's agent-development kit (`.agents/agents/`) ships four
standalone agents that work with Lean 4 / TLA+ files directly, independent of
this Java service:

- `.agents/agents/lean4-theorem-writer.md` — drafts/revises a `.lean`
  theorem or proof to satisfy a spec's formal-proof obligation.
- `.agents/agents/tlaplus-spec-writer.md` — drafts/revises a `.tla`/`.cfg`
  pair to formalize a spec's behavior/invariants.
- `.agents/agents/lean4-verifier.md` — type-checks a `.lean` file with Lean 4
  and reports the compiler's own diagnostics. Read-only.
- `.agents/agents/tlaplus-verifier.md` — model-checks a `.tla`/`.cfg` pair
  with TLC and reports pass/violation/deadlock with the counterexample trace.
  Read-only.

They're mirrored to `.claude/agents/`, `.github/agents/`, and `.codex/agents/`
by `scripts/mirror-agents.sh`/`.ps1` — use them from an agent session to draft
or check a proof/spec file directly, for whatever domain this service ends
up serving.
