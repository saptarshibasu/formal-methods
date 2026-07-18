---
name: create-adr
description: >-
  Use when recording an architecture decision — triggers on phrases like
  "add an ADR", "record this decision", "create an ADR for X", "document why
  we chose Y", or "write an architecture decision record". Finds the next ADR
  number, fills the template from user input, and writes docs/adr/<NNNN-slug>.md
  with explicit approval before writing.
---

# ADR

Records a new Architecture Decision Record in `docs/adr/`.

## Behavioral guardrails

<!-- GUARDRAILS:skill -->
<!-- /GUARDRAILS:skill -->
- **One decision per ADR.** If the user describes multiple decisions, split them
  or ask which to record first.
- **Immutable once written.** Remind the user: to reverse a decision, add a new
  ADR that supersedes this one; never edit a committed ADR's Decision section.
- **Short by design.** An ADR records a choice and its reasoning so a future
  agent or human doesn't relitigate it — it is not a design doc.
- **Approval before writing.** Present the full draft and wait for explicit
  approval before touching the filesystem.

## Steps

### 1 — Find the next number

List `docs/adr/`, take the highest NNNN and increment by 1. Zero-pad to four digits.
If the folder is empty or has no numbered files, start at `0001`.

### 2 — Gather inputs

If the user's message already contains enough context (title, problem, decision,
consequences), extract it. Otherwise ask — group into one pass:

- **Title** — short, decision-focused (e.g. "Use PostgreSQL for session
  storage")
- **Context** — what forced the decision now? What constraints apply?
- **Decision** — the choice, stated actively ("We will …")
- **Consequences** — what becomes easier, harder, or must now be lived with?
  Include negatives honestly.
- **Alternatives considered** — what was rejected and why (one line each)?

### 3 — Derive the slug

Lowercase the title, replace spaces and punctuation with hyphens, trim to ~40
chars. Example: "Use PostgreSQL for session storage" → `use-postgresql-for-session-storage`.

### 4 — Draft

Use this structure:

```
# ADR-<NNNN>: <Title>

**Status**: Accepted
**Date**: <today YYYY-MM-DD>
**Deciders**: <from user, or leave blank for user to fill>

## Context
<context>

## Decision
<decision>

## Consequences
<consequences>

## Alternatives considered
- **<alternative>** — <why rejected>
```

Remove the HTML comment block from the template — it must not appear in the
real ADR.

### 5 — Approval gate

**Stop. Present the full draft.** Ask for explicit approval before writing.
Note the target path: `docs/adr/<NNNN-slug>.md`.

### 6 — Write

After approval, delegate the actual file write to the `docs-writer` agent —
pass it the finalized draft text and the target path
(`docs/adr/<NNNN-slug>.md`). Don't write the file yourself: `docs-writer`
owns everything under `docs/`, ADRs included, and has its own `Write`/`Edit`
tools. Once `docs-writer` reports the file written, confirm the path to the
user.

No mirroring is needed — ADRs are plain docs, not skills or agents.
