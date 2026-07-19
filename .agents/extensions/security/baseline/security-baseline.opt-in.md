# Opt-in — Security Baseline

**Pack**: `security/baseline` · **Rule IDs**: `SEC-01`…`SEC-07`
**Loads**: `security-baseline.md` (only if opted in)

Present this question to the human during the Route/Specify stage and record the
answer in `specs/<NNN>/decision-log.md` (Extensions row).

> **Does this feature touch any of: authentication/authorization, secrets or
> credentials, user-supplied or external input, persisted user data, file/path
> handling, outbound network calls, or system/shell execution?**
>
> 1. **Yes — opt in (recommended if unsure).** Load the Security Baseline pack;
>    rules `SEC-01`…`SEC-07` become blocking constraints for this feature.
> 2. **No — opt out.** None of the above applies (e.g. a docs-only change, an
>    internal refactor with no new I/O). The pack will not be loaded.
>
> If any single item applies, choose **Yes**. When in doubt, opt in — the cost
> of the pack is a few checks; the cost of skipping it can be a vulnerability.

Record the outcome as, e.g.:

> `| 2026-06-26 | Extensions | Opted in: security/baseline (SEC-01..07) | feature adds a login endpoint + stores user email | <human> |`
