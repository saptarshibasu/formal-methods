# ADR-0001: Use JPA @Version Optimistic Locking to Arbitrate Concurrent Order Status Updates

**Status**: Accepted
**Date**: 2026-07-18
**Deciders**: sapb2004@gmail.com

## Context

The order lifecycle feature requires that when two conflicting status updates for the same order are processed concurrently (e.g. by different service instances), at most one is accepted, with history and current state left mutually consistent (FR-013). There's no real multi-instance deployment in this repo to test against, so the plan needed a concrete, testable arbitration mechanism rather than an abstract guarantee.

## Decision

We will add an `@Version` column to the `Order` entity. Each lifecycle mutation runs in its own `@Transactional` unit; when two transactions race to update the same order row, the database itself arbitrates via the version check — one commits, the other raises `ObjectOptimisticLockingFailureException`, which `OrderService` maps to HTTP 409. In a real multi-instance deployment, all instances share one PostgreSQL database, so the row's version column is the single arbiter regardless of which instance a request lands on — N threads racing in one instance and N requests racing across N instances are the same race against the same row.

## Consequences

This is a reusable pattern for any future feature on this service needing concurrent-write correctness on a PostgreSQL-backed entity, without building application-level distributed locking. It couples correctness to DB transaction/version-check semantics — a future move away from a single shared PostgreSQL database (e.g. sharding, a different datastore) would need a new arbitration mechanism. Automated tests exercise the rejection path by mocking the repository to throw `ObjectOptimisticLockingFailureException` (constitution Article IV forbids a real DB in the automated suite); the full-interleaving safety property is discharged separately by the TLA+ model (`Specs/OrderLifecycle.tla`), and real-DB arbitration is confirmed by a manual local PostgreSQL run.

## Alternatives considered

- **Application-level distributed lock (e.g. a lock table or Redis-based mutex)** — rejected: adds a new infrastructure dependency and a second correctness mechanism to reason about, when the database already provides this guarantee for free via standard JPA versioning.
- **Pessimistic locking (`SELECT ... FOR UPDATE`)** — rejected: holds a row lock for the duration of the transaction, which is unnecessary contention for what's expected to be a low-conflict-rate workload (races are the exception, not the norm, for a given order).
