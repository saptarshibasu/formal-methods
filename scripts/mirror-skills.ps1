#!/usr/bin/env pwsh
# mirror-skills.ps1 - Windows/PowerShell twin of mirror-skills.sh.
#
# Propagates canonical skills (.agents/skills) into the .claude, .github, and
# .codex mirror dirs. Run after adding or editing a skill under
# .agents/skills/. Never hand-edit a mirror. See mirror-skills.sh for rationale.
#
# Every file is copied byte-for-byte EXCEPT each skill's top-level SKILL.md,
# whose empty `<!-- GUARDRAILS:skill --><!-- /GUARDRAILS:skill -->` marker
# (see docs/guardrails.md) gets expanded with the canonical "No guessing /
# Investigate before claiming / Conservative by default" bullets at mirror
# time. The canonical .agents/skills/*/SKILL.md never carries the bullets
# itself - there is nothing to hand-copy and nothing left to drift.
#
# Usage:  pwsh ./scripts/mirror-skills.ps1   (or, on Windows PowerShell: powershell -File .\scripts\mirror-skills.ps1)

$ErrorActionPreference = 'Stop'

$root = (git rev-parse --show-toplevel 2>$null)
if (-not $root) { $root = (Get-Location).Path }
$canon = Join-Path $root '.agents/skills'
$guardrailsDocPath = Join-Path $root 'docs/guardrails.md'

if (-not (Test-Path $canon) -or -not (Get-ChildItem $canon -Directory -ErrorAction SilentlyContinue)) {
  Write-Error "$canon is empty or missing - nothing to mirror. Copy this kit's .agents/skills/ in first."
}
if (-not (Test-Path $guardrailsDocPath)) { throw "$guardrailsDocPath not found - guardrails canon is missing." }

# Pull the canonical "skill" guardrails block out of docs/guardrails.md.
$guardrailsDocText = Get-Content -Raw $guardrailsDocPath
$startMarker = '<!-- GUARDRAILS:skill -->'
$endMarker = '<!-- /GUARDRAILS:skill -->'
if ($guardrailsDocText -notmatch ('(?s)' + [regex]::Escape($startMarker) + '\r?\n(.*?)\r?\n' + [regex]::Escape($endMarker))) {
  throw "$guardrailsDocPath has no (or an empty) GUARDRAILS:skill block."
}
$guardrailsSkillCanon = $Matches[1]

# Expand a mirrored SKILL.md's empty guardrails marker in place. Fails loudly
# if the marker is missing (or was hand-edited into a stale copy instead) so
# an unmirrored guardrails edit can't silently ship.
function Expand-SkillGuardrails($file) {
  $text = Get-Content -Raw $file
  if (-not $text.Contains($startMarker)) {
    throw "$file`: missing a '$startMarker ... $endMarker' marker pair."
  }
  $pattern = [regex]::Escape($startMarker) + '\r?\n' + [regex]::Escape($endMarker)
  # Static [regex]::Replace has no (input, pattern, replacement, count)
  # overload - use an instance so the count overload actually limits to one
  # replacement, and escape '$' in the replacement text since .NET treats it
  # as a backreference token in replacement templates.
  $replacement = ($startMarker + "`n" + $guardrailsSkillCanon + "`n" + $endMarker) -replace '\$', '$$$$'
  $rx = New-Object System.Text.RegularExpressions.Regex($pattern)
  $newText = $rx.Replace($text, $replacement, 1)
  Set-Content -NoNewline -Path $file -Value $newText
}

foreach ($tool in @('.claude/skills', '.github/skills', '.codex/skills')) {
  $dest = Join-Path $root $tool
  New-Item -ItemType Directory -Force -Path $dest | Out-Null
  foreach ($skill in Get-ChildItem $canon -Directory) {
    $target = Join-Path $dest $skill.Name
    if (Test-Path $target) { Remove-Item -Recurse -Force $target }
    Copy-Item -Recurse -Force $skill.FullName $target
    $skillMd = Join-Path $target 'SKILL.md'
    if (-not (Test-Path $skillMd)) { throw "$skillMd missing after mirroring $($skill.Name)." }
    Expand-SkillGuardrails $skillMd
  }
}

$count = (Get-ChildItem $canon -Directory).Count
Write-Host "Mirrored $count skill(s) -> .claude, .github, .codex"
