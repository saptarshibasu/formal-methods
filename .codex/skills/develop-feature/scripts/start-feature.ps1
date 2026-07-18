#!/usr/bin/env pwsh
# start-feature.ps1 - Windows/PowerShell twin of start-feature.sh.
#
# Deterministic scaffolding for a new spec-driven feature: pick the next feature
# number, slugify the description, create specs/<NNN-slug>/, and copy the
# canonical templates in with the mechanical header fields pre-filled. Does NOT
# write any actual spec/plan/task content - that's the agent's job, per SKILL.md.
# Behaviour is byte-equivalent to start-feature.sh; keep the two in sync.
#
# Usage: ./start-feature.ps1 "<feature description>"

param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Words)
$ErrorActionPreference = 'Stop'

$description = ($Words -join ' ').Trim()
if ([string]::IsNullOrWhiteSpace($description)) {
  Write-Error 'Usage: start-feature.ps1 "<feature description>"'
}

$repoRoot = (git rev-parse --show-toplevel 2>$null)
if (-not $repoRoot) { $repoRoot = (Get-Location).Path }
$specsDir     = Join-Path $repoRoot 'specs'
$templatesDir = Join-Path $repoRoot 'templates'

# 1. Refuse to proceed without the canonical templates (one source of truth).
foreach ($f in 'spec.template.md', 'plan.template.md', 'tasks.template.md', 'decision-log.template.md', 'learnings.template.md') {
  if (-not (Test-Path (Join-Path $templatesDir $f))) {
    Write-Error "Missing $templatesDir/$f`nCopy this knowledge base's templates/ folder into the project root first."
  }
}
New-Item -ItemType Directory -Force -Path $specsDir | Out-Null

# 2. Next 3-digit feature number. Numbers are never reused, even if an earlier
#    feature folder is later deleted.
$last = Get-ChildItem $specsDir -Directory -ErrorAction SilentlyContinue |
  Where-Object { $_.Name -match '^[0-9]{3}-' } |
  ForEach-Object { [int]$_.Name.Substring(0, 3) } |
  Sort-Object | Select-Object -Last 1
$nextNum = if ($null -eq $last) { '001' } else { '{0:D3}' -f ($last + 1) }

# 3. Slugify: lowercase, non-alphanumeric -> hyphen, collapse/trim hyphens,
#    keep the first five words so folder names stay readable.
$slug = $description.ToLowerInvariant()
$slug = [regex]::Replace($slug, '[^a-z0-9]+', '-').Trim('-')
$slug = (($slug -split '-' | Where-Object { $_ -ne '' }) | Select-Object -First 5) -join '-'

$featureSlug = "$nextNum-$slug"
$featureDir  = Join-Path $specsDir $featureSlug

if (Test-Path $featureDir) { Write-Error "Refusing to overwrite existing $featureDir" }
New-Item -ItemType Directory -Force -Path (Join-Path $featureDir 'contracts') | Out-Null

# 4. Copy the templates and fill in only the mechanical header fields
#    (feature id, title, date) - never the actual content.
$today = Get-Date -Format 'yyyy-MM-dd'
foreach ($doc in 'spec', 'plan', 'tasks', 'decision-log', 'learnings') {
  $src = Join-Path $templatesDir "$doc.template.md"
  $dst = Join-Path $featureDir "$doc.md"
  $content = (Get-Content -Raw $src).
    Replace('[###-feature-name]', $featureSlug).
    Replace('[FEATURE NAME]', $description).
    Replace('[FEATURE]', $description).
    Replace('[DATE]', $today)
  Set-Content -Path $dst -Value $content -NoNewline
}

Write-Host "Created $featureDir"
Write-Host "  spec.md          <- fill in next (Phase 1: Specify)"
Write-Host "  plan.md          <- do not fill in until spec.md is approved"
Write-Host "  tasks.md         <- do not fill in until plan.md is approved"
Write-Host "  decision-log.md  <- committed audit trail; append at each gate"
Write-Host "  learnings.md     <- ungated scratchpad; implementor/debugger append as they go"
