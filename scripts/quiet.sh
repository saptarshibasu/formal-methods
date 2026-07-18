#!/usr/bin/env bash
# quiet.sh — context-efficient backpressure between noisy sensors and the agent.
#
# Verbose test/build output is a token leak AND a quality problem: hundreds of
# log lines dumped into an agent's context window degrade the very loop the
# sensor exists to close. This wrapper runs any command, swallows its output,
# and reports only what the loop needs:
#
#   success → one PASS line (exit 0)
#   failure → one FAIL line + the first relevant error excerpt, full log kept
#             in a temp file for deeper investigation (original exit code)
#
# Usage:
#   scripts/quiet.sh <command> [args...]
#   scripts/quiet.sh npm test
#   scripts/quiet.sh pytest tests/unit -q
#
# Tuning (env vars):
#   QUIET_MAX_LINES  lines of error excerpt on failure          (default: 40)
#   QUIET_ERR_RE     extended, case-insensitive regex marking the first
#                    "relevant" error line — override per stack if the generic
#                    one fires early (e.g. on a "0 errors" summary line)
#   QUIET_TIMEOUT    max seconds the command may run before being killed
#                    (default: 300; set 0 to disable). A hung command is the
#                    exact failure mode this wrapper exists to prevent — don't
#                    let it hang the agent with zero output instead.
#
# Used by: the implementor agent (and any agent running build/test commands),
# and the lint/test slot in .githooks/pre-commit. Name the wrapped form of your
# test command in AGENTS.md's Commands section so every agent finds it.
# Cross-platform twin: scripts/quiet.ps1.

set -uo pipefail

if [ $# -lt 1 ]; then
  echo "usage: scripts/quiet.sh <command> [args...]" >&2
  exit 2
fi

max="${QUIET_MAX_LINES:-40}"
err_re="${QUIET_ERR_RE:-(FAILED|FAILURES?|[0-9]+ (failed|errors?)|error(\[[A-Za-z0-9]+\])?:|Error:|ERROR|Exception|Traceback|AssertionError|assert(ion)? ?fail|panic:|not ok|BUILD FAILED|Compilation failed|✖|✗)}"
timeout_s="${QUIET_TIMEOUT:-300}"

log="$(mktemp "${TMPDIR:-/tmp}/quiet.XXXXXX.log")"

if [ "$timeout_s" != "0" ]; then
  timeout --kill-after=5 "$timeout_s" "$@" >"$log" 2>&1
else
  "$@" >"$log" 2>&1
fi
status=$?
lines=$(wc -l <"$log" | tr -d ' ')

if [ "$status" -eq 0 ]; then
  echo "PASS: $* (exit 0, ${lines} output lines suppressed)"
  rm -f "$log"
  exit 0
fi

if [ "$timeout_s" != "0" ] && [ "$status" -eq 124 ]; then
  echo "FAIL (timeout): $* (exceeded ${timeout_s}s, killed) — full output: ${log}"
  echo "── last ${max} lines before kill ──"
  tail -n "$max" "$log"
  exit "$status"
fi

echo "FAIL: $* (exit ${status}) — full output: ${log}"

first=$(grep -niE -m1 -- "$err_re" "$log" | cut -d: -f1)
if [ -n "${first:-}" ]; then
  last=$((first + max - 1))
  echo "── first relevant error (line ${first} of ${lines}; full log above) ──"
  sed -n "${first},${last}p" "$log"
  [ "$last" -lt "$lines" ] && echo "── truncated at ${max} lines ──"
else
  echo "── no error pattern matched; last ${max} lines ──"
  tail -n "$max" "$log"
fi

exit "$status"
