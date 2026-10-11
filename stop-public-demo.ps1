# =======================================================================
# ExpenseChain - Stop Public Demo & Local Development Environment
# =======================================================================
[CmdletBinding()]
param ()

$ErrorActionPreference = "SilentlyContinue"

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host " Stopping ExpenseChain Public Demo & Backend Environment...             " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

$RootPath = $PSScriptRoot
$LogsPath = Join-Path $RootPath "logs"
$PidFile = Join-Path $LogsPath "cloudflared-demo.pid"

# -------------------------------------------------------------------------
# [1/2] Terminate Cloudflare Demo Tunnel
# -------------------------------------------------------------------------
Write-Host "[1/2] Stopping Cloudflare Quick Tunnel..." -ForegroundColor Yellow
$stoppedTunnel = $false

# Check recorded PID
if (Test-Path $PidFile) {
    $savedPidStr = (Get-Content $PidFile -ErrorAction SilentlyContinue).Trim()
    if ($savedPidStr -match '^\d+$') {
        $savedPid = [int]$savedPidStr
        try {
            $p = Get-Process -Id $savedPid -ErrorAction SilentlyContinue
            if ($p -and ($p.ProcessName -match "cloudflared")) {
                Write-Host "      - Terminating recorded cloudflared process (PID: $savedPid)..." -ForegroundColor DarkGray
                Stop-Process -Id $savedPid -Force -ErrorAction SilentlyContinue
                $stoppedTunnel = $true
            }
        } catch {}
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# Safely check any lingering cloudflared tunnel targeting port 8080 (without killing unrelated tunnels)
try {
    $cfProcs = Get-CimInstance Win32_Process -Filter "Name = 'cloudflared.exe'" -ErrorAction SilentlyContinue
    foreach ($cp in $cfProcs) {
        if ($cp.CommandLine -match "tunnel" -and $cp.CommandLine -match "8080") {
            $tPid = [int]$cp.ProcessId
            Write-Host "      - Terminating ExpenseChain cloudflared process (PID: $tPid)..." -ForegroundColor DarkGray
            Stop-Process -Id $tPid -Force -ErrorAction SilentlyContinue
            $stoppedTunnel = $true
        }
    }
} catch {}

if ($stoppedTunnel) {
    Write-Host "      + Cloudflare Demo Tunnel stopped." -ForegroundColor Green
} else {
    Write-Host "      + No active demo Cloudflare tunnel was found." -ForegroundColor Green
}

# Clean stale tunnel logs
$TunnelLog = Join-Path $LogsPath "cloudflared-demo.log"
if (Test-Path $TunnelLog) {
    Remove-Item $TunnelLog -Force -ErrorAction SilentlyContinue
}

# -------------------------------------------------------------------------
# [2/2] Run Existing STOP_DEV.bat / stop-dev.ps1
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/2] Stopping Corda Nodes and Spring Boot Backend..." -ForegroundColor Yellow

$StopDevScript = Join-Path $RootPath "stop-dev.ps1"
if (Test-Path $StopDevScript) {
    & $StopDevScript
} else {
    Write-Host "      ! Warning: stop-dev.ps1 not found at $StopDevScript" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host " EXPENSECHAIN PUBLIC DEMO STOPPED SUCCESSFULLY                         " -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host ""
