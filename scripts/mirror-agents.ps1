#!/usr/bin/env pwsh
# mirror-agents.ps1 - Windows/PowerShell twin of mirror-agents.sh.
#
# Generates the per-tool agent file from each canonical definition in
# .agents/agents/<name>.md (YAML front-matter + Markdown body):
#   -> .claude/agents/<name>.md         Claude Code  (verbatim)
#   -> .github/agents/<name>.agent.md   Copilot      (name+description+model + body)
#   -> .codex/agents/<name>.toml        Codex        ([agent] table + instructions)
#
# Run after adding or editing an agent under .agents/agents/. Never hand-edit a
# generated file. Edit the canonical .md only. CI re-runs this and
# fails if the committed files drift from a fresh generation.
#
# Behavioral guardrails: the canonical agent files only carry an empty
# `<!-- GUARDRAILS:agent --><!-- /GUARDRAILS:agent -->` (or `agent-readonly`)
# marker where the shared "No guessing / Investigate before claiming /
# Conservative by default" bullets go - this script fills the marker in with
# the block from docs/guardrails.md at generation time, for every one of the
# three per-tool outputs. There is nothing left to hand-copy and nothing left
# to drift.
#
# Org-specific model/tool policy (which Copilot/Codex model a tier maps to,
# which Codex tool a Claude tool maps to) lives in data, not code: see
# .agents/model-map.conf. EDIT that file to match your org - this script and
# its bash twin just read it, so a config edit can't introduce logic
# divergence between the two.
#
# Fails loudly (throws) rather than emitting a subtly-wrong file: unknown tool
# names, front-matter it can't parse as single-line scalars, or a body that would
# break the Codex TOML string are all hard errors. See mirror-agents.sh.
#
# Usage:  pwsh ./scripts/mirror-agents.ps1   (or: powershell -File .\scripts\mirror-agents.ps1)

$ErrorActionPreference = 'Stop'

$root = (git rev-parse --show-toplevel 2>$null)
if (-not $root) { $root = (Get-Location).Path }
$canon = Join-Path $root '.agents/agents'
$modelMapPath = Join-Path $root '.agents/model-map.conf'
$guardrailsDocPath = Join-Path $root 'docs/guardrails.md'

$srcs = Get-ChildItem $canon -Filter '*.md' -ErrorAction SilentlyContinue
if (-not $srcs) { throw "$canon has no *.md agents - nothing to mirror." }
if (-not (Test-Path $modelMapPath)) { throw "$modelMapPath not found - org policy data is missing." }
if (-not (Test-Path $guardrailsDocPath)) { throw "$guardrailsDocPath not found - guardrails canon is missing." }

$claudeDir  = Join-Path $root '.claude/agents'
$copilotDir = Join-Path $root '.github/agents'
$codexDir   = Join-Path $root '.codex/agents'
foreach ($d in @($claudeDir, $copilotDir, $codexDir)) {
  New-Item -ItemType Directory -Force -Path $d | Out-Null
  Get-ChildItem $d -File | Where-Object { $_.Name -ne '.gitkeep' -and ($_.Extension -in '.md', '.toml') } |
    Remove-Item -Force
}

# Load org policy data (model/tool mappings) from .agents/model-map.conf into
# a flat hashtable keyed "<namespace>.<key>". This is the ONLY place an org
# customizes tiers/tools - edit that data file, not this script. See it for
# the format and namespaces.
$modelMap = @{}
foreach ($line in (Get-Content $modelMapPath)) {
  $t = $line.Trim()
  if (-not $t -or $t.StartsWith('#')) { continue }
  $idx = $t.IndexOf('=')
  if ($idx -lt 0) { continue }
  $k = $t.Substring(0, $idx).Trim()
  $v = $t.Substring($idx + 1).Trim()
  $modelMap[$k] = $v
}

function Get-MapEntry($ns, $key) {
  $full = "$ns.$key"
  if ($modelMap.ContainsKey($full)) { return $modelMap[$full] }
  throw "unknown $ns entry '$key' (add '$full=...' to $modelMapPath)"
}

# Map a Claude tool name to its Codex equivalent. Unknown names are a hard
# error so a typo or a new tool can't pass through mis-mapped.
function Convert-CodexTool($t) { Get-MapEntry 'codex_tool' $t }

# Map a canonical model tier to Copilot's `model` front-matter value.
function Convert-CopilotModel($m) { Get-MapEntry 'copilot_model' $m }

# Map a canonical model tier to a Codex model + reasoning effort.
function Convert-CodexModel($m) { Get-MapEntry 'codex_model' $m }
function Convert-CodexEffort($m) { Get-MapEntry 'codex_effort' $m }

# Pull one delimited canonical guardrails block out of docs/guardrails.md (the
# "skill" / "agent" / "agent-readonly" variants - see that file).
$guardrailsDocText = Get-Content -Raw $guardrailsDocPath
function Get-GuardrailsCanon($variant) {
  $start = "<!-- GUARDRAILS:$variant -->"
  $end = "<!-- /GUARDRAILS:$variant -->"
  if ($guardrailsDocText -notmatch ('(?s)' + [regex]::Escape($start) + '\r?\n(.*?)\r?\n' + [regex]::Escape($end))) {
    throw "$guardrailsDocPath has no (or an empty) GUARDRAILS:$variant block."
  }
  return $Matches[1]
}
$guardrailsCanon = @{
  'agent'          = Get-GuardrailsCanon 'agent'
  'agent-readonly' = Get-GuardrailsCanon 'agent-readonly'
}

# Replace an agent's empty <!-- GUARDRAILS:<variant> --> marker with the
# canonical bullets for that variant. Fails loudly if a canonical file is
# missing the marker (or has a stale hand-written copy instead) so an
# unmirrored guardrails edit can't silently ship.
function Expand-Guardrails($body, $variant) {
  $start = "<!-- GUARDRAILS:$variant -->"
  $end = "<!-- /GUARDRAILS:$variant -->"
  $pattern = [regex]::Escape($start) + '\r?\n' + [regex]::Escape($end)
  if ($body -notmatch $pattern) {
    throw "missing a '$start' ... '$end' marker pair (expected variant '$variant')."
  }
  # Static [regex]::Replace has no (input, pattern, replacement, count)
  # overload - a literal 4th-arg integer silently coerces into RegexOptions
  # instead (1 = IgnoreCase), which is not what we want. Use an instance so
  # the count overload actually limits to one replacement, and escape '$' in
  # the replacement text since .NET treats it as a backreference token.
  $replacement = ($start + "`n" + $guardrailsCanon[$variant] + "`n" + $end) -replace '\$', '$$$$'
  $rx = New-Object System.Text.RegularExpressions.Regex($pattern)
  return $rx.Replace($body, $replacement, 1)
}

$q3 = "'''"
$count = 0
foreach ($src in $srcs) {
  $raw = Get-Content -Raw $src.FullName
  # Split front-matter (between the first two --- lines) from the body.
  if ($raw -notmatch '(?s)^---\r?\n(.*?)\r?\n---\r?\n(.*)$') {
    throw "$($src.Name) has no '---' front-matter block."
  }
  $fm = $Matches[1]; $body = $Matches[2]
  $fields = @{}
  foreach ($line in ($fm -split '\r?\n')) {
    if ($line -match '^\s*([A-Za-z_]+):\s*(.*?)\s*$') { $fields[$Matches[1]] = $Matches[2] }
  }
  $name = $fields['name']; $desc = $fields['description']
  $tools = $fields['tools']; $model = $fields['model']
  foreach ($k in 'name', 'description', 'tools', 'model') {
    if (-not $fields[$k]) { throw "$($src.Name): missing or empty '$k' (block-list YAML is unsupported - use 'tools: A, B')." }
  }
  if ($name -ne $src.BaseName) { throw "$($src.Name): front-matter name '$name' must match the filename." }

  # Which guardrails variant this agent's marker expands to: the two
  # read-only agents (no Write/Edit tool) get "agent-readonly", every agent
  # that can write gets "agent" - see docs/guardrails.md.
  if ($tools -match 'Write|Edit') { $guardrailsVariant = 'agent' } else { $guardrailsVariant = 'agent-readonly' }
  $body = Expand-Guardrails $body $guardrailsVariant

  if ($body.Contains($q3)) { throw "$($src.Name): body contains a triple single-quote, which would break the Codex TOML string." }

  # 1) Claude - canonical front-matter, generated body (guardrails marker expanded).
  $claudeContent = "---`n$fm`n---`n$body"
  Set-Content -NoNewline -Path (Join-Path $claudeDir "$name.md") -Value $claudeContent

  # 2) Copilot - name + description + mapped model front-matter, then body.
  $copilot = "---`nname: $name`ndescription: $desc`nmodel: $(Convert-CopilotModel $model)`n---`n$body"
  Set-Content -NoNewline -Path (Join-Path $copilotDir "$name.agent.md") -Value $copilot

  # 3) Codex - TOML table; tools mapped + de-duplicated; body as instructions.
  $codexTools = @()
  foreach ($t in ($tools -split ',')) {
    $t = $t.Trim(); if (-not $t) { continue }
    $ct = Convert-CodexTool $t
    if ($codexTools -notcontains $ct) { $codexTools += $ct }
  }
  $toolsCsv = ($codexTools | ForEach-Object { "`"$_`"" }) -join ', '
  # YAML double-quoted scalar: strip the outer quotes - its \" and \\ escapes
  # are already valid TOML basic-string escapes, so the inner text passes
  # through verbatim. Plain (unquoted) scalar: escape it for TOML.
  if ($desc -match '^"(.*)"$') { $escDesc = $Matches[1] }
  else { $escDesc = $desc -replace '\\', '\\' -replace '"', '\"' }
  $toml = @"
# Codex custom agent - generated from .agents/agents/$($src.Name) by mirror-agents.
# Do not hand-edit; edit the canonical .md and re-run the mirror.

[agent]
name = "$name"
description = "$escDesc"
# Mapped from canonical tier "$model" - adjust the tier mapping in .agents/model-map.conf.
model = "$(Convert-CodexModel $model)"
model_reasoning_effort = "$(Convert-CodexEffort $model)"
tools = [$toolsCsv]

instructions = $q3
$body$q3
"@
  Set-Content -NoNewline -Path (Join-Path $codexDir "$name.toml") -Value $toml
  $count++
}

Write-Host "Mirrored $count agent(s) -> .claude (.md), .github (.agent.md), .codex (.toml)"
