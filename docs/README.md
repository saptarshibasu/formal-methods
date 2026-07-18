# docs/

Deep-reference material — one file per topic, referenced *by name* from
`AGENTS.md` and the constitution rather than inlined, so the always-loaded
context stays small.

## Reference guides

| File | What it covers |
|---|---|
| [`guardrails.md`](guardrails.md) | Universal behavioral guardrails shared across all skills — the always-on rules every skill session inherits. |

## Project reference

| File / dir | What it covers |
|---|---|
| [`glossary.md`](glossary.md) | Domain vocabulary, referenced from AGENTS.md's Domain Language section. |
| [`adr/`](adr/) | Architecture Decision Records. Use the `create-adr` skill to add new ones; check here before changing a cross-cutting pattern. |
| [`../.agents/extensions/`](../.agents/extensions/) | Opt-in rule packs (e.g. `security/baseline`) layered onto a feature on demand. Authoring format in its own `README.md`; loaded only when opted in. |

## How these are meant to be used

These docs are *not* loaded into every agent session — that would defeat the
token discipline they describe. They are pulled in on demand: `AGENTS.md` and
the constitution point at them by name, and an agent (or a human) reads the one
relevant to the task at hand. Keep it that way when you add a doc: put the
deep material here, and add a one-line by-name pointer wherever it's relevant.
