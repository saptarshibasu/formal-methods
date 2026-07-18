# verification-service

Spring Boot service that tracks formal-methods verification jobs. A client
submits a proof (`.lean`) or a spec (`.tla` + `.cfg`) as a `VerificationJob`,
and it's persisted to PostgreSQL via Spring Data JPA. Actual verification —
running Lean 4 or TLC against the submitted source — happens outside this
service, via the framework's coding-assistant agents (see **Related** below).

## Architecture

```
web/VerificationController        REST API (create / read / list jobs)
  -> service/VerificationOrchestrationService   job CRUD, no execution
       -> repository/VerificationJobRepository, VerificationResultRepository
            -> domain/VerificationJob, VerificationResult   (Spring Data JPA)
```

A `VerificationResult` is appended to a job by whatever ran the check (today,
an agent session — see **Related** below) rather than by this service itself.

## Prerequisites

- Java 17+
- A local PostgreSQL 16 instance (no container runtime required)
- To actually verify a proof/spec: run the framework's `lean4-verifier` /
  `tlaplus-verifier` agent from a coding-assistant session (needs a Lean 4
  toolchain or a `tla2tools.jar` respectively) — this service only stores the
  job and its result.

## Running locally

Install PostgreSQL locally and create the database/role the service expects
(or override the env vars below to point at an existing instance):

```bash
createdb verification
psql -c "CREATE ROLE verification LOGIN PASSWORD 'verification';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE verification TO verification;"
```

Then run the service:

```bash
./gradlew bootRun
```

The service starts on `:8080`; Flyway applies `db/migration/V1__init_schema.sql`
on boot.

## Configuration

Datasource settings are environment-overridable (see
`src/main/resources/application.yml`):

| Env var | Default | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/verification` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `verification` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `verification` | DB password |

## API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/verification/jobs` | Create a job (status `PENDING`) |
| `GET` | `/api/verification/jobs/{id}` | Job detail, including every past `VerificationResult` |
| `GET` | `/api/verification/jobs?sensorType=&status=` | List/filter jobs (summary, no raw tool output) |

Example — a Lean 4 proof:

```bash
curl -s -X POST localhost:8080/api/verification/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "reflexivity",
    "sensorType": "LEAN4",
    "sourceFileName": "T.lean",
    "sourceContent": "theorem t : 1 = 1 := rfl"
  }'
# -> 201, { "id": 1, "status": "PENDING", ... }
```

Example — a TLA+ spec (`configContent` is required for `TLA_PLUS`; TLC cannot
model-check a spec without its matching `.cfg`):

```bash
curl -s -X POST localhost:8080/api/verification/jobs \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "order-typeok",
    "sensorType": "TLA_PLUS",
    "sourceFileName": "OrderSpec.tla",
    "sourceContent": "---- MODULE OrderSpec ----\n...\n====",
    "configContent": "INVARIANT TypeOK"
  }'
```

## Testing

```bash
./gradlew test
```

- `VerificationOrchestrationServiceTest` — unit tests against a mocked
  repository, so they run without a database.

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
or check a proof/spec file directly; use this service when you want
verification jobs persisted, queryable, and exposed over HTTP.
