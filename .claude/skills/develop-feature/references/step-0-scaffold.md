# Step 0 — Scaffold (mechanical — don't use judgment here)

**First, check for an existing feature — don't blind-scaffold.** The script
always mints a *new* incremented folder, so running it for work that already
has one creates a duplicate. Before running it: list `specs/` and compare
against the user's request. If a folder plausibly matches (by slug or topic),
**stop and confirm** with the user — "resume `specs/<NNN>-<slug>/` or start a
new feature?" — and on resume, follow "Resuming an in-progress feature" in
`SKILL.md` instead of scaffolding. Only scaffold once you've confirmed this is
genuinely new. When in doubt, ask; a wrong guess either duplicates or
overwrites intent.

Then run the scaffold script with the feature description as a single argument.
**Path matters**: the script lives next to the top-level `SKILL.md`, not at
the repo root. Don't run a bare `scripts/start-feature.sh` relative path —
your shell's current directory is usually the repo root (or something else
entirely), not this skill's folder, and that lookup will fail with "no such
file or directory." Instead, build the full path yourself from where you read
`SKILL.md`: `.claude/skills/develop-feature/`, `.codex/skills/develop-feature/`,
`.github/skills/develop-feature/`, and `.agents/skills/develop-feature/` are
byte-identical mirrors — take whichever prefix matches the copy you
loaded, and append `scripts/start-feature.sh` (or `.ps1`). Run the command
with your shell's cwd at the **repo root** — these paths are repo-root-relative,
not relative to the skill folder. Two byte-equivalent versions — use the one
matching the current OS:

```bash
# macOS / Linux (bash) — substitute the tool prefix you're running under:
bash .claude/skills/develop-feature/scripts/start-feature.sh "<feature description>"
```

```powershell
# Windows (PowerShell) — substitute the tool prefix you're running under:
pwsh .claude/skills/develop-feature/scripts/start-feature.ps1 "<feature description>"
# (on Windows PowerShell, equivalently: powershell -File .claude/skills/develop-feature/scripts/start-feature.ps1 "<feature description>")
```

Prefer `.ps1` on Windows, `.sh` on macOS/Linux. Unsure? Try one and fall back.

If neither script runs (path genuinely doesn't resolve, no shell available,
etc.), do it by hand: find the highest `NNN-` prefix under
`specs/`, increment, slugify to kebab-case, create `specs/<NNN>-<slug>/`, copy
the five templates as `spec.md`, `plan.md`, `tasks.md`, `decision-log.md`,
`learnings.md`.

**Immediately after scaffolding:**

- Fill `decision-log.md`'s first two rows (**Route** and **Extensions**) from
  the Step R decisions. This file is committed — the feature's durable audit trail.
- **Track B** does not use `plan.md`: delete the scaffolded `plan.md` and note
  "plan skipped (Track B)" in the decision log, unless a design decision later
  forces a promotion to Track C (record that promotion in the log too).
- `learnings.md` is scaffolded but ungated — unlike the other three, it never
  gets a `decision-log.md` row or a Status flip. `implementor` and `debugger`
  read and append to it directly once Phase 4 starts; nothing to do here.
- **If the triggering prompt included content beyond WHAT/WHY** — existing-
  implementation context ("the current system does X"), a candidate approach,
  a library/pattern the human wants considered — create `research.md` now
  from `templates/research.template.md` and seed it: existing-system context
  and candidate approaches go in **Alternatives Investigated**; anything
  version-sensitive or still genuinely open goes in **Open Questions
  Resolved** / **Still Open**. Do this **regardless of track** — `research.md`
  isn't Track-D-exclusive, it's created whenever the triggering prompt (or a
  later human message) actually has this kind of content, same as `plan.md`
  is skipped on Track B when there's no design decision to make. This is what
  keeps that content from being silently dropped: `specifier` is WHAT/WHY-only
  and won't put it in `spec.md`, and `planner` receives nothing but file paths
  when invoked at Phase 2 — `research.md` is the file that carries it forward.
  The same applies mid-session, not just at kickoff: if the human volunteers
  this kind of content later (e.g. while giving Phase 1 revision feedback),
  create or append to `research.md` then, don't wait for Phase 2.
