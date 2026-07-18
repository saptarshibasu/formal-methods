#!/usr/bin/env pwsh
# quiet.ps1 - Windows/PowerShell twin of scripts/quiet.sh.
#
# Context-efficient backpressure: runs any command, swallows its output, and
# reports pass/fail + the first relevant error only. Full output is kept in a
# temp log for deeper investigation. Keep behavior in sync with quiet.sh.
#
# Usage:
#   pwsh scripts/quiet.ps1 <command> [args...]
#   pwsh scripts/quiet.ps1 npm test
#
# Tuning (env vars): QUIET_MAX_LINES (default 40), QUIET_ERR_RE,
# QUIET_TIMEOUT (max seconds before the command is killed; default 300,
# 0 disables). A hung command is the exact failure mode this wrapper exists
# to prevent - don't let it hang the agent with zero output instead.

param(
  [Parameter(Mandatory = $true, ValueFromRemainingArguments = $true)]
  [string[]]$Command
)

$max = if ($env:QUIET_MAX_LINES) { [int]$env:QUIET_MAX_LINES } else { 40 }
$errRe = if ($env:QUIET_ERR_RE) { $env:QUIET_ERR_RE } else {
  '(FAILED|FAILURES?|\d+ (failed|errors?)|error(\[[A-Za-z0-9]+\])?:|Error:|ERROR|Exception|Traceback|AssertionError|assert(ion)? ?fail|panic:|not ok|BUILD FAILED|Compilation failed|✖|✗)'
}
$timeoutS = if ($env:QUIET_TIMEOUT) { [int]$env:QUIET_TIMEOUT } else { 300 }

$log = Join-Path ([System.IO.Path]::GetTempPath()) ("quiet-" + [System.IO.Path]::GetRandomFileName() + ".log")
$cmdLine = $Command -join ' '

$proc = Start-Process -FilePath $Command[0] `
  -ArgumentList @($Command | Select-Object -Skip 1) `
  -NoNewWindow -PassThru `
  -RedirectStandardOutput $log `
  -RedirectStandardError "$log.err"

$timedOut = $false
if ($timeoutS -gt 0) {
  if (-not $proc.WaitForExit($timeoutS * 1000)) {
    $timedOut = $true
    try { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } catch {}
  }
}
else {
  $proc.WaitForExit()
}

# Merge stderr into the combined log (order isn't preserved across streams,
# but that's true of the original 2>&1 pipe too once redirected to a file).
if (Test-Path "$log.err") {
  Get-Content "$log.err" -ErrorAction SilentlyContinue | Add-Content -Path $log
  Remove-Item "$log.err" -ErrorAction SilentlyContinue
}

$status = if ($timedOut) { 124 } else { $proc.ExitCode }
if ($null -eq $status) { $status = 1 }

$all = @(Get-Content $log)
$lines = $all.Count

if ($status -eq 0) {
  Write-Host "PASS: $cmdLine (exit 0, $lines output lines suppressed)"
  Remove-Item $log -ErrorAction SilentlyContinue
  exit 0
}

if ($timedOut) {
  Write-Host "FAIL (timeout): $cmdLine (exceeded ${timeoutS}s, killed) - full output: $log"
  Write-Host "-- last $max lines before kill --"
  $all | Select-Object -Last $max | ForEach-Object { Write-Host $_ }
  exit $status
}

Write-Host "FAIL: $cmdLine (exit $status) - full output: $log"

$ic = [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
$first = -1
for ($i = 0; $i -lt $lines; $i++) {
  if ([regex]::IsMatch($all[$i], $errRe, $ic)) { $first = $i; break }
}

if ($first -ge 0) {
  $last = [Math]::Min($first + $max - 1, $lines - 1)
  Write-Host ("-- first relevant error (line {0} of {1}; full log above) --" -f ($first + 1), $lines)
  $all[$first..$last] | ForEach-Object { Write-Host $_ }
  if ($last -lt ($lines - 1)) { Write-Host "-- truncated at $max lines --" }
}
else {
  Write-Host "-- no error pattern matched; last $max lines --"
  $all | Select-Object -Last $max | ForEach-Object { Write-Host $_ }
}

exit $status
