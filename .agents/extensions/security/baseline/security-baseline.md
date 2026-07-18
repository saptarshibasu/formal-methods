# Security Baseline (extension pack)

**Category**: security · **IDs**: `SEC-01`…`SEC-07` · **Enforcement**: blocking
when opted in (see `.agents/extensions/README.md`).

> **Directional reference, not a policy.** These rules are a starting point for
> building effective security constraints into the workflow. Every organisation
> should review, customise, and test them against its own threat model and
> compliance obligations before relying on them. They do not constitute a
> complete security review.

When this pack is opted in for a feature, each rule below is a **blocking
constraint**. At every gate (Specify → Plan → Tasks) and during `code-reviewer`,
verify each rule's **Verification** conditions; an unmet condition blocks the
stage until resolved or until a human explicitly accepts the risk (record the
acceptance, with who signed off, in the feature's `decision-log.md`). Cite rule
IDs in findings.

---

## Rule SEC-01: Validate and sanitize all untrusted input

**Rule.** Treat every input crossing a trust boundary — request bodies, query
and path params, headers, uploaded files, message-queue payloads, third-party
API responses — as hostile until validated. Validate against an allow-list
(type, range, length, format) at the boundary, before the value reaches business
logic. Never build SQL, shell commands, file paths, LDAP, or template strings by
concatenating untrusted input.

**Verification.**
- Every new external entry point validates its inputs before use, with a
  defined rejection path for invalid data.
- Data access uses parameterised queries / prepared statements or an ORM's
  safe API — no string-built queries with interpolated input.
- Any OS command, file path, or dynamic code execution derived from input is
  rejected or built from a fixed allow-list, never raw concatenation.

## Rule SEC-02: Enforce authentication and authorization on every protected path

**Rule.** Every endpoint or action that exposes non-public data or
state-changing behaviour must authenticate the caller and authorize the specific
operation on the specific resource (object-level checks, not just "is logged
in"). Deny by default; access is granted explicitly.

**Verification.**
- New protected routes/actions have an explicit authz check tied to the target
  resource and the caller's permissions.
- There is no "authenticated therefore authorized" gap — ownership/role is
  checked for the specific object being acted on.
- Default behaviour for an unmatched or unauthenticated request is denial.

## Rule SEC-03: Never hard-code or commit secrets

**Rule.** Credentials, API keys, tokens, private keys, and connection strings
come from configuration or a secret manager at runtime — never literals in
source, and never committed (including test fixtures and sample configs).

**Verification.**
- No secret-like literals introduced in the diff (the `.githooks/pre-commit`
  secret scan is the computational backstop; this rule is the intent behind it).
- New config keys for secrets are documented as externally provided, with no
  real value in the repo.
- No secret is written to logs, error messages, or analytics.

## Rule SEC-04: Protect sensitive data in transit and at rest

**Rule.** Transmit sensitive data only over TLS. Hash passwords with a
purpose-built algorithm (bcrypt/scrypt/argon2 — never a fast/plain hash).
Encrypt sensitive data at rest where the threat model requires it, using the
platform's vetted crypto library — never a hand-rolled scheme.

**Verification.**
- No new plaintext transmission of credentials or personal data.
- Passwords/secrets are stored using an approved one-way KDF, not reversible or
  fast-hashed.
- Cryptographic operations use a standard library primitive, not a custom
  algorithm or a hard-coded IV/key.

## Rule SEC-05: Encode output to prevent injection in the consuming context

**Rule.** Encode/escape data for the context it renders in — HTML, JS, SQL, CLI,
URL — to prevent XSS and related injection. Prefer the framework's
context-aware auto-escaping; disable it only with a written, reviewed reason.

**Verification.**
- User-influenced data rendered into a response is escaped for its output
  context (templating auto-escape on; no raw/`unsafe`/`dangerouslySetInnerHTML`
  sinks without justification recorded in the decision log).
- Redirects and forwards built from input are validated against an allow-list.

## Rule SEC-06: Keep dependencies current and free of known vulnerabilities

**Rule.** New or upgraded dependencies must be from a trusted source, pinned,
and free of known high/critical vulnerabilities at the time of introduction.
Prefer maintained libraries over abandoned ones.

**Verification.**
- New dependencies are pinned to a specific version and run through the
  project's SCA/vulnerability scanner (name it in `AGENTS.md`; treat the scan as
  the computational enforcement of this rule).
- No dependency added with a known unpatched high/critical advisory; if
  unavoidable, the risk acceptance is recorded in `decision-log.md`.

## Rule SEC-07: Fail securely — safe errors, complete audit logging

**Rule.** On error, fail closed (deny, don't fall through to allow). Do not leak
stack traces, internal paths, SQL, or PII in responses. Log
security-relevant events (authn success/failure, authz denials, input-validation
rejections) with enough context to investigate — and without logging secrets or
full PII.

**Verification.**
- Error paths deny access on failure rather than defaulting open.
- Client-facing errors are generic; detailed diagnostics go to server-side logs
  only.
- Security-relevant events are logged with actor, action, and outcome; logs
  contain no secrets or unmasked sensitive data.
