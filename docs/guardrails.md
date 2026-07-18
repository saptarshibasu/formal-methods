# Behavioral Guardrails

These are the canonical universal guardrails that apply to every skill and
agent in this kit. This file is the *only* place the wording lives: a
canonical file under `.agents/agents/` or `.agents/skills/` never embeds the
bullets itself — it carries an empty delimited marker instead (e.g.
`<!-- GUARDRAILS:skill --><!-- /GUARDRAILS:skill -->`, right after its
`## Behavioral guardrails` heading), and `scripts/mirror-agents.sh` /
`scripts/mirror-skills.sh` expand that marker with the matching block below
at mirror time, for every one of the generated `.claude/.github/.codex`
copies.

This makes drift structurally impossible instead of merely policed: there is
no verbatim copy anywhere to fall out of sync, because the mirrors are
*generated* from this doc, not hand-copied from it. Editing a `GUARDRAILS:*`
block here and re-running the mirror scripts is the only way the wording
ever changes anywhere. CI's existing mirror-drift guard (`agent-harness.yml`'s
"Skills & agents are in sync with canonical sources" step) catches the one
remaining failure mode — a mirror committed without re-running the
generator — the same way it catches any other unmirrored edit.

## The three universal guardrails (skill variant)

Skills run in the top-level session and can pause to talk to the human
directly. This is the wording every `.agents/skills/*/SKILL.md` file's
`## Behavioral guardrails` section must start with.

<!-- GUARDRAILS:skill -->
- **No guessing.** Where input leaves something unspecified, write
  `[NEEDS CLARIFICATION: specific question]` and surface it — never silently
  invent an assumption.
- **Investigate before claiming.** Never make statements about the codebase
  without first reading the relevant files. If a claim requires looking at
  code, look first.
- **Conservative by default.** Recommend before you write; stop and ask before
  anything irreversible (deleting files, force-pushing, dropping tables,
  external service calls).
<!-- /GUARDRAILS:skill -->

## Sub-agent variant

Agents under `.agents/agents/` (`code-reviewer`, `debugger`, `implementor`,
etc.) are invoked as sub-agents by a skill or another agent — they return one
report to their caller and cannot themselves pause a conversation with the
human. Two of the three universal guardrails need different wording there so
the agent isn't holding an instruction it has no way to execute:

- **Conservative by default** — skills say "stop and ask before anything
  irreversible." Agents instead say: flag anything irreversible and **return
  it to the caller as a question** instead of proceeding. The caller (a skill
  running at the top level, or a human session) is the one that can actually
  ask.
- **No guessing** — skills say "write `[NEEDS CLARIFICATION: ...]`." For the
  two read-only agents with no Write/Edit tool (`code-reviewer`,
  `artifact-analyzer`), change "write" to "state ... in your report" — they
  have no file to write the marker into, only a returned report. Agents that
  do hold Write/Edit keep "write" as-is.

Most agents (`debugger`, `docs-writer`, `implementor`, `planner`, `specifier`,
`task-decomposer`, `test-writer` — anything with a Write/Edit tool) use this
block:

<!-- GUARDRAILS:agent -->
- **No guessing.** Where input leaves something unspecified, write
  `[NEEDS CLARIFICATION: specific question]` and surface it — never silently
  invent an assumption.
- **Investigate before claiming.** Never make statements about the codebase
  without first reading the relevant files. If a claim requires looking at
  code, look first.
- **Conservative by default.** Recommend before you write; flag anything
  irreversible (deleting files, force-pushing, dropping tables, external
  service calls) and return it to the caller as a question instead of
  proceeding — a sub-agent cannot pause to ask the human directly.
<!-- /GUARDRAILS:agent -->

The two read-only agents (`code-reviewer`, `artifact-analyzer` — no
Write/Edit tool) use this block instead:

<!-- GUARDRAILS:agent-readonly -->
- **No guessing.** Where input leaves something unspecified, state
  `[NEEDS CLARIFICATION: specific question]` in your report and surface it —
  never silently invent an assumption. (This agent is read-only — no
  Write/Edit tool — so the marker goes in the returned report, not a file.)
- **Investigate before claiming.** Never make statements about the codebase
  without first reading the relevant files. If a claim requires looking at
  code, look first.
- **Conservative by default.** Recommend, never write. Flag anything
  irreversible (deleting files, force-pushing, dropping tables, external
  service calls) and return it to the caller as a question — a sub-agent
  cannot pause to ask the human directly.
<!-- /GUARDRAILS:agent-readonly -->

When adding a new agent under `.agents/agents/`, give its
`## Behavioral guardrails` section the sub-agent marker — `<!-- GUARDRAILS:agent -->
<!-- /GUARDRAILS:agent -->` for anything with a Write/Edit tool, or
`<!-- GUARDRAILS:agent-readonly -->
<!-- /GUARDRAILS:agent-readonly -->` for the two read-only agents — never the
skill marker, and never the bullets themselves. Likewise a new skill under
`.agents/skills/*/SKILL.md` gets the `<!-- GUARDRAILS:skill -->
<!-- /GUARDRAILS:skill -->` marker. After adding or editing a canonical file,
run `scripts/mirror-agents.sh` / `scripts/mirror-skills.sh` (or their `.ps1`
twins) to regenerate `.claude/`, `.codex/`, and `.github/` with the marker
expanded — never hand-edit those generated copies, and never
hand-write the bullets into a canonical `.agents/` file either.

## Skill-specific additions

Each skill may extend these with its own guardrails — for example:

- `develop-feature` adds **No over-engineering** (only build what's
  directly requested).
- `init-project` and `amend-constitution` add **No over-populating** (short
  and accurate beats long and generic).
- `sync-agents-md` adds **Evidence or nothing** (every claim must trace to a
  file you read) and several file-scoping guardrails.
- `implementor` and `specifier` add **Search before creating** / **Search
  before assuming a gap** — search the codebase broadly, more than one
  plausible name or location, before concluding a capability doesn't already
  exist. The internal-code analog of the constitution's "never guess, always
  verify" rule for external dependencies.

## Maintenance

When editing a guardrail, update the matching `GUARDRAILS:*` block here —
that's the only edit needed. Then run `scripts/mirror-agents.sh` and
`scripts/mirror-skills.sh` (or their `.ps1` twins) to regenerate every
`.claude/.github/.codex` copy with the new wording and commit the result;
CI's mirror-drift guard fails the build if you forget. There is no second
file to hand-edit, and no canonical `.agents/` file to keep in sync — they
only ever carry the empty marker. A skill or agent may add its own extra
bullets after the shared block (e.g. `develop-feature`'s
**No over-engineering**) directly below the closing
`<!-- /GUARDRAILS:* -->` marker in its own file; that part is still
hand-written and untouched by the mirror scripts.
