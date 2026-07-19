#!/usr/bin/env bash
# mirror-agents.sh — propagate canonical agents into every tool directory.
#
# Single source of truth: .agents/agents/. Unlike skills (which are byte-for-byte
# mirrors), each tool reads agents in its own format, so this script GENERATES the
# per-tool file from the canonical definition instead of copying it:
#
#   .agents/agents/<name>.md   (canonical: YAML front-matter + Markdown body)
#     → .claude/agents/<name>.md         Claude Code   (verbatim — same format)
#     → .github/agents/<name>.agent.md   Copilot       (name+description+model front-matter + body)
#     → .codex/agents/<name>.toml        Codex         ([agent] table + instructions)
#
# Run this AFTER you add or edit an agent under .agents/agents/. Never hand-edit a
# generated file in .claude/.github/.codex — it will be overwritten. Edit the
# canonical .md only. CI re-runs this and fails if the committed files
# drift from a fresh generation.
#
# Behavioral guardrails: the canonical agent files only carry an empty
# `<!-- GUARDRAILS:agent --><!-- /GUARDRAILS:agent -->` (or `agent-readonly`)
# marker where the shared "No guessing / Investigate before claiming /
# Conservative by default" bullets go — this script fills the marker in with
# the block from docs/guardrails.md at generation time, for every one of the
# three per-tool outputs. There is nothing left to hand-copy and nothing left
# to drift: the single source is docs/guardrails.md, and a stale copy simply
# can't exist because the bullets are never written into a canonical file by
# hand in the first place.
#
# Org-specific model/tool policy (which Copilot/Codex model a tier maps to,
# which Codex tool a Claude tool maps to) lives in data, not code: see
# .agents/model-map.conf. EDIT that file to match your org — this script and
# its Windows twin just read it, so a config edit can't introduce logic
# divergence between the two.
#
# Fails loudly (exit 1) rather than emitting a subtly-wrong file: unknown tool
# names, front-matter it can't parse as single-line scalars, or a body that would
# break the Codex TOML string are all hard errors.
#
# Idempotent. Cross-platform twin: mirror-agents.ps1 (Windows/PowerShell) — both
# must emit byte-identical files or CI's drift guard will fail.
# Usage: bash scripts/mirror-agents.sh
# (rev: hardened with fail-loud validation)

set -euo pipefail

die() { echo "✖ $*" >&2; exit 1; }

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
CANON="$ROOT/.agents/agents"
MODEL_MAP="$ROOT/.agents/model-map.conf"
GUARDRAILS_DOC="$ROOT/docs/guardrails.md"

if [ ! -d "$CANON" ] || ! ls "$CANON"/*.md >/dev/null 2>&1; then
  die "$CANON has no *.md agents — nothing to mirror."
fi

[ -f "$MODEL_MAP" ] || die "$MODEL_MAP not found — org policy data is missing."
[ -f "$GUARDRAILS_DOC" ] || die "$GUARDRAILS_DOC not found — guardrails canon is missing."

# Pull one delimited canonical guardrails block out of docs/guardrails.md
# (the "skill" / "agent" / "agent-readonly" variants — see that file).
extract_guardrails_canon() {
  local variant="$1"
  awk -v start="<!-- GUARDRAILS:$variant -->" -v end="<!-- /GUARDRAILS:$variant -->" '
    index($0, start) == 1 { f=1; next }
    index($0, end) == 1 { f=0 }
    f { print }
  ' "$GUARDRAILS_DOC"
}

# Look up (and validate) the canonical block for a variant on demand. No
# associative array here on purpose: `declare -A` needs bash 4+, and macOS
# still ships bash 3.2 as /bin/bash (frozen there over GPLv3 licensing), so a
# preloaded map would break the script for anyone running the stock shell.
guardrails_canon_for() {
  local variant="$1" canon
  canon="$(extract_guardrails_canon "$variant")"
  [ -n "$canon" ] || die "$GUARDRAILS_DOC has no (or an empty) GUARDRAILS:$variant block."
  printf '%s' "$canon"
}

# Fail fast at startup (before touching any agent file) if either variant's
# block is missing or empty — same fail-fast timing the old preloaded-array
# version had.
for variant in agent agent-readonly; do
  guardrails_canon_for "$variant" >/dev/null
done

# Replace an agent's empty <!-- GUARDRAILS:<variant> --> marker with the
# canonical bullets for that variant. Fails loudly if a canonical file is
# missing the marker (or has a stale hand-written copy instead) so an
# unmirrored guardrails edit can't silently ship.
inject_guardrails() {
  local body="$1" variant="$2" canon
  local start="<!-- GUARDRAILS:$variant -->" end="<!-- /GUARDRAILS:$variant -->"
  case "$body" in
    *"$start"*"$end"*) ;;
    *) die "missing a '$start' ... '$end' marker pair (expected variant '$variant')." ;;
  esac
  canon="$(guardrails_canon_for "$variant")"
  # canon spans multiple lines. Passing a multi-line value through awk's -v
  # runs it through string-literal escape processing, and macOS's /usr/bin/awk
  # (the "one true awk") hard-errors on any raw embedded newline there:
  # "awk: newline in string ... at source line 1". gawk (Linux default)
  # tolerates it, which is why this only surfaces on Mac. Route the value
  # through the environment/ENVIRON instead — every POSIX awk accepts that
  # verbatim, no escape parsing involved.
  GUARDRAILS_CANON_ENV="$canon" awk -v start="$start" -v end="$end" '
    index($0, start) == 1 { print; print ENVIRON["GUARDRAILS_CANON_ENV"]; f=1; next }
    index($0, end) == 1 { f=0 }
    !f { print }
  ' <<< "$body"
}

CLAUDE_DIR="$ROOT/.claude/agents"
COPILOT_DIR="$ROOT/.github/agents"
CODEX_DIR="$ROOT/.codex/agents"
mkdir -p "$CLAUDE_DIR" "$COPILOT_DIR" "$CODEX_DIR"

# Wipe previously generated files (keep .gitkeep) so deletions propagate.
find "$CLAUDE_DIR" "$COPILOT_DIR" "$CODEX_DIR" -type f \
     \( -name '*.md' -o -name '*.toml' \) ! -name '.gitkeep' -delete 2>/dev/null || true

# Look up "<namespace>.<key>=<value>" straight out of .agents/model-map.conf
# on each call — this is the ONLY place an org customizes tiers/tools; edit
# that data file, not this script. Deliberately not preloaded into an
# associative array: `declare -A` needs bash 4+, and macOS still ships bash
# 3.2 as /bin/bash (frozen there over GPLv3 licensing), so a preloaded map
# would break this script for anyone running the stock shell there. Kept in
# sync with the loader in mirror-agents.ps1 only in the sense that both read
# the same data file.
map_lookup() {
  local ns="$1" key="$2" full="$1.$2" val status
  val="$(awk -v want="$full" '
    {
      line = $0
      trimmed = line
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", trimmed)
      if (trimmed == "" || trimmed ~ /^#/) next
      eq = index(line, "=")
      if (eq == 0) next
      k = substr(line, 1, eq - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", k)
      if (k != want) next
      v = substr(line, eq + 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", v)
      sub(/\r$/, "", v)
      print v
      found = 1
      exit
    }
    END { exit(found ? 0 : 3) }
  ' "$MODEL_MAP")"
  status=$?
  [ "$status" -eq 0 ] || die "unknown $ns entry '$key' (add '$full=...' to $MODEL_MAP)"
  printf '%s\n' "$val"
}

# Map a Claude tool name to its Codex equivalent. Unknown names are a hard
# error so a typo or a newly-introduced tool can't silently pass through
# mis-mapped.
codex_tool() { map_lookup codex_tool "$1"; }

# Map a canonical model tier to Copilot's `model` front-matter value.
copilot_model() { map_lookup copilot_model "$1"; }

# Map a canonical model tier to a Codex model + reasoning effort.
codex_model() { map_lookup codex_model "$1"; }
codex_effort() { map_lookup codex_effort "$1"; }

# Read a single-line front-matter scalar ("key: value"). Returns the raw
# value, surrounding quotes included. Errors if the key is declared but has no
# inline value (e.g. a YAML block list) — single-line scalars only.
frontmatter_value() {
  local file="$1" key="$2"
  awk -v key="$key" '
    /^---[[:space:]]*\r?$/ { d++; next }
    d==1 && $0 ~ "^"key":" {
      sub("^"key":[[:space:]]*", "")
      sub(/[[:space:]]*\r?$/, "")
      found=1; print; exit
    }
    END { if (!found) exit 3 }
  ' "$file"
}

# Print the Markdown body (everything after the second `---`).
body_after_frontmatter() {
  awk '
    /^---[[:space:]]*\r?$/ { d++; if (d==2) { started=1; next } }
    started { print }
  ' "$1"
}

Q3="'''"

count=0
for src in "$CANON"/*.md; do
  base="$(basename "$src")"
  head -n1 "$src" | grep -qE '^---[[:space:]]*\r?$' \
    || die "$base does not start with a '---' front-matter block."

  name="$(frontmatter_value "$src" name)"        || die "$base: missing 'name'."
  desc="$(frontmatter_value "$src" description)"  || die "$base: missing 'description'."
  tools="$(frontmatter_value "$src" tools)"       || die "$base: missing 'tools'."
  model="$(frontmatter_value "$src" model)"       || die "$base: missing 'model'."

  [ -n "$name" ]  || die "$base: 'name' is empty."
  [ -n "$desc" ]  || die "$base: 'description' is empty."
  [ -n "$tools" ] || die "$base: 'tools' is empty (block-list YAML is unsupported — use 'tools: A, B')."
  [ -n "$model" ] || die "$base: 'model' is empty."
  [ "$name" = "$(basename "$src" .md)" ] \
    || die "$base: front-matter name '$name' must match the filename."

  body="$(body_after_frontmatter "$src")"

  # Which guardrails variant this agent's marker expands to: the two
  # read-only agents (no Write/Edit tool) get "agent-readonly", every agent
  # that can write gets "agent" — see docs/guardrails.md.
  case "$tools" in
    *Write*|*Edit*) guardrails_variant=agent ;;
    *)              guardrails_variant=agent-readonly ;;
  esac
  body="$(inject_guardrails "$body" "$guardrails_variant")"

  case "$body" in
    *"$Q3"*) die "$base: body contains a triple single-quote, which would break the Codex TOML string." ;;
  esac

  # 1) Claude — canonical front-matter, generated body (guardrails marker expanded).
  {
    awk '/^---[[:space:]]*\r?$/ { print; d++; if (d==2) exit; next } { print }' "$src"
    printf '%s\n' "$body"
  } > "$CLAUDE_DIR/$name.md"

  # 2) Copilot — name + description + mapped model in front-matter, then the body.
  {
    printf -- '---\n'
    printf 'name: %s\n' "$name"
    printf 'description: %s\n' "$desc"
    printf 'model: %s\n' "$(copilot_model "$model")"
    printf -- '---\n'
    printf '%s\n' "$body"
  } > "$COPILOT_DIR/$name.agent.md"

  # 3) Codex — TOML table. Tools mapped + de-duplicated; body as instructions.
  codex_tools=""
  seen=" "
  IFS=',' read -ra _tools <<< "$tools"
  for t in "${_tools[@]}"; do
    t="$(printf '%s' "$t" | xargs)"; [ -z "$t" ] && continue
    ct="$(codex_tool "$t")"
    case "$seen" in
      *" $ct "*) ;;
      *) codex_tools="${codex_tools:+$codex_tools, }\"$ct\""
         seen="$seen$ct " ;;
    esac
  done
  # YAML double-quoted scalar: strip the outer quotes — its \" and \\ escapes
  # are already valid TOML basic-string escapes, so the inner text passes
  # through verbatim. Plain (unquoted) scalar: escape it for TOML.
  case "$desc" in
    \"*\") esc_desc="${desc#\"}"; esc_desc="${esc_desc%\"}" ;;
    *)     esc_desc="$(printf '%s' "$desc" | sed 's/\\/\\\\/g; s/"/\\"/g')" ;;
  esac
  {
    printf '# Codex custom agent - generated from .agents/agents/%s by mirror-agents.\n' "$base"
    printf '# Do not hand-edit; edit the canonical .md and re-run the mirror.\n\n'
    printf '[agent]\n'
    printf 'name = "%s"\n' "$name"
    printf 'description = "%s"\n' "$esc_desc"
    printf '# Mapped from canonical tier "%s" - adjust the tier mapping in .agents/model-map.conf.\n' "$model"
    printf 'model = "%s"\n' "$(codex_model "$model")"
    printf 'model_reasoning_effort = "%s"\n' "$(codex_effort "$model")"
    printf 'tools = [%s]\n\n' "$codex_tools"
    printf 'instructions = %s\n' "$Q3"
    printf '%s\n' "$body"
    printf '%s' "$Q3"
  } > "$CODEX_DIR/$name.toml"

  count=$((count + 1))
done

echo "Mirrored $count agent(s) → .claude (.md), .github (.agent.md), .codex (.toml)"
