#!/usr/bin/env bash
# start-feature.sh — deterministic scaffolding for a new spec-driven feature.
#
# Does exactly the mechanical part of "start a new feature": picks the next
# feature number, slugifies the description, creates specs/<NNN-slug>/, and
# copies the canonical templates into it with the obvious header
# fields pre-filled. Does NOT write any actual spec/plan/task content —
# that's the agent's job, done after this script returns, per SKILL.md.
#
# Cross-platform twin: start-feature.ps1 (Windows/PowerShell).
# Usage: start-feature.sh "<feature description>"

set -euo pipefail

if [ $# -eq 0 ]; then
  echo "Usage: start-feature.sh \"<feature description>\"" >&2
  exit 1
fi

DESCRIPTION="$*"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SPECS_DIR="$REPO_ROOT/specs"
TEMPLATES_DIR="$REPO_ROOT/templates"

# 1. Refuse to proceed without the canonical templates. This script does
#    not carry its own fallback copies — one source of truth, at the
#    project root.
for f in spec.template.md plan.template.md tasks.template.md decision-log.template.md learnings.template.md; do
  if [ ! -f "$TEMPLATES_DIR/$f" ]; then
    echo "Missing $TEMPLATES_DIR/$f" >&2
    echo "Copy this knowledge base's templates/ folder into the project root first." >&2
    exit 1
  fi
done

mkdir -p "$SPECS_DIR"

# 2. Compute the next 3-digit feature number. Numbers are never reused, even
#    if an earlier feature folder is later deleted/abandoned.
LAST_NUM=$(find "$SPECS_DIR" -maxdepth 1 -type d -name '[0-9][0-9][0-9]-*' 2>/dev/null \
  -exec basename {} \; | sed -E 's/^([0-9]{3})-.*/\1/' | sort -n | tail -1)
if [ -z "$LAST_NUM" ]; then
  NEXT_NUM="001"
else
  # 10# forces base-10 so a leading zero (e.g. "008") isn't read as octal.
  NEXT_NUM=$(printf "%03d" $((10#$LAST_NUM + 1)))
fi

# 3. Slugify the description: lowercase, non-alphanumeric -> hyphen,
#    collapse/trim hyphens, keep it to the first five words so folder names
#    stay readable.
SLUG=$(echo "$DESCRIPTION" \
  | tr '[:upper:]' '[:lower:]' \
  | tr -cs 'a-z0-9' '-' \
  | sed -E 's/^-+//; s/-+$//' \
  | cut -d'-' -f1-5)

FEATURE_SLUG="${NEXT_NUM}-${SLUG}"
FEATURE_DIR="$SPECS_DIR/$FEATURE_SLUG"

if [ -d "$FEATURE_DIR" ]; then
  echo "Refusing to overwrite existing $FEATURE_DIR" >&2
  exit 1
fi
mkdir -p "$FEATURE_DIR/contracts"

# 4. Copy the templates and fill in only the mechanical header
#    fields (feature id, title, date) — never the actual content.
TODAY="$(date +%Y-%m-%d)"
# Escape sed replacement metacharacters (backslash, ampersand, slash) in the
# free-text description so a title like "Login & Logout" isn't mangled — sed
# reads a bare & in the replacement as "the whole matched text." The slug and
# date are [a-z0-9-]/digits and need no escaping. (The .ps1 twin uses a literal
# string Replace, so it is exact without this step.)
DESC_ESC=$(printf '%s' "$DESCRIPTION" | sed -e 's/[\&/]/\\&/g')
for doc in spec plan tasks decision-log learnings; do
  cp "$TEMPLATES_DIR/${doc}.template.md" "$FEATURE_DIR/${doc}.md"
  sed -i.bak \
    -e "s/\[###-feature-name\]/${FEATURE_SLUG}/g" \
    -e "s/\[FEATURE NAME\]/${DESC_ESC}/g" \
    -e "s/\[FEATURE\]/${DESC_ESC}/g" \
    -e "s/\[DATE\]/${TODAY}/g" \
    "$FEATURE_DIR/${doc}.md"
  rm -f "$FEATURE_DIR/${doc}.md.bak"
done

echo "Created $FEATURE_DIR"
echo "  spec.md          <- fill in next (Phase 1: Specify)"
echo "  plan.md          <- do not fill in until spec.md is approved"
echo "  tasks.md         <- do not fill in until plan.md is approved"
echo "  decision-log.md  <- committed audit trail; append at each gate"
echo "  learnings.md     <- ungated scratchpad; implementor/debugger append as they go"
