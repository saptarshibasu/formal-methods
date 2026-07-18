# Phase 3.7 — Write failing tests (test-writer gate)

After the Analyze gate clears (or is explicitly skipped), invoke the
`test-writer` agent to write failing tests **before any implementation begins**.
This is the point where TDD becomes mechanical rather than advisory.

Conditioned on track — mirrors the Analyze pattern:

- **Track A** — skip (trivial changes; still test-first if behaviour changes,
  but enforced in the commit message, not here).
- **Track B** — default-on: write a regression test for the bug and one test
  per acceptance scenario. Confirm each fails for the right reason before
  proceeding.
- **Track C** — default-on: write tests for every user story's acceptance
  scenarios before implementation of that story begins.
- **Track D** — default-on for the story's acceptance tests. Characterization
  tests are **ask-first, never auto-run**: when a story touches an untested
  brownfield area identified in `AGENTS.md` or the plan, offer a
  characterization pass *before* any changes to that code and let the human
  decide — record the choice either way in `decision-log.md`. If accepted,
  the test-writer handles it in its characterization mode — those land in
  `tests/characterization/` and pin *current* behaviour, not desired
  behaviour. If declined, proceed without them: the logged row is the human's
  accepted risk, and the analyzer and reviewer treat it as such.

**Tests are written and confirmed red one user story at a time, immediately
before that story's implementation** — not all upfront for the whole feature.
Phases 3.7, 4, and 5 therefore form one loop that repeats per story, in the
priority order `tasks.md` lays out (P1 first): test-writer writes and
confirms that story's tests red (3.7), implementor makes them green (4),
code-reviewer reviews and the human commits (5) — then the loop starts over
at 3.7 for the next story. It only stops once the last story clears Phase 5.
This keeps test intent close to the implementation it drives and lets each
story's review happen while the work is still fresh.

1. For the next user story in priority order, invoke the `test-writer` agent.
   Pass it: the approved `spec.md` (that story's acceptance scenarios), the
   approved `tasks.md` (that story's test tasks), and the pack ID(s) and
   rule-file path(s) of any opted-in extensions — not the rule text;
   `test-writer` has its own `Read` tool and loads the rules itself to write
   tests covering their Verification conditions. On Track D, if the story
   touches a brownfield area named in
   `AGENTS.md` or the plan *and* the human accepted the characterization
   offer for that area (never assume — ask now if it hasn't been decided,
   and log the answer), tell it to write those characterization tests first.
   It returns a confirmed-red report rather than the test code — right-reason
   confirmation is its own protocol (`test-writer.md`).
2. Relay the test-writer's report as-is: each test's tier, path, and
   confirmed-failing output; which acceptance criteria are covered and which
   aren't (with reason for any gap). A confirmed-red report is the pass
   condition here — there's no separate human approval gate for "is this
   correctly red," since that's a fact the test-writer already verified by
   running it, not a judgment call.
3. Append a **Tests (red)** row to `decision-log.md` for this story (or "skipped
   — user's call" per the track table above), then continue to `phase-4-implement.md`
   for the same story.
