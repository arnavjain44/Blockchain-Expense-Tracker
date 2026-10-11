# =======================================================================
# ExpenseChain - Public Demo Launcher (Laptop + Cloudflare Quick Tunnel)
# =======================================================================
[CmdletBinding()]
param (
    [switch]$NoBrowser
)

$ErrorActionPreference = "Continue"

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "          EXPENSECHAIN - PUBLIC DEMO LAUNCHER                          " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host " Architecture: Internet -> Cloudflare Tunnel -> localhost:8080 -> Corda Nodes" -ForegroundColor DarkGray
Write-Host ""

$RootPath = $PSScriptRoot
$LogsPath = Join-Path $RootPath "logs"
$PidFile = Join-Path $LogsPath "cloudflared-demo.pid"
$TunnelLog = Join-Path $LogsPath "cloudflared-demo.log"

if (-not (Test-Path $LogsPath)) {
    New-Item -ItemType Directory -Force -Path $LogsPath | Out-Null
}

# -------------------------------------------------------------------------
# [1/5] Prerequisite Verification
# -------------------------------------------------------------------------
Write-Host "[1/5] Verifying environment prerequisites..." -ForegroundColor Yellow

# A. Verify cloudflared
$cfCmd = Get-Command cloudflared -ErrorAction SilentlyContinue
$cfCmdPath = $null
if ($cfCmd) {
    $cfCmdPath = $cfCmd.Source
} else {
    $wingetCf = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\cloudflared.exe"
    if (Test-Path $wingetCf) {
        $cfCmdPath = $wingetCf
    }
}

if (-not $cfCmdPath) {
    Write-Host ""
    Write-Host "=======================================================================" -ForegroundColor Red
    Write-Host " ERROR: Cloudflare CLI (cloudflared) is not found!                     " -ForegroundColor Red
    Write-Host "=======================================================================" -ForegroundColor Red
    Write-Host " Please install cloudflared by running:" -ForegroundColor Yellow
    Write-Host "    INSTALL_CLOUDFLARED.bat" -ForegroundColor White
    Write-Host " or install via winget:" -ForegroundColor Yellow
    Write-Host "    winget install --id Cloudflare.cloudflared" -ForegroundColor White
    Write-Host "=======================================================================" -ForegroundColor Red
    exit 1
}
Write-Host "      + Cloudflare CLI detected: $cfCmdPath" -ForegroundColor Green

# B. Verify Java 8
$JavaHome = Join-Path $RootPath "tools\jdk8"
$JavaExe = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path $JavaExe)) {
    $sysJava = Get-Command java -ErrorAction SilentlyContinue
    if ($sysJava) {
        $jVer = & java -version 2>&1 | Out-String
        if ($jVer -match "1\.8\.") {
            $JavaExe = $sysJava.Source
        }
    }
}

if (-not (Test-Path $JavaExe)) {
    Write-Host ""
    Write-Host "=======================================================================" -ForegroundColor Red
    Write-Host " ERROR: Java 8 runtime was not found!                                 " -ForegroundColor Red
    Write-Host " Expected: tools\jdk8\bin\java.exe                                    " -ForegroundColor Red
    Write-Host "=======================================================================" -ForegroundColor Red
    Write-Host " Please run SETUP_ENVIRONMENT.bat to configure the portable Java 8 JDK." -ForegroundColor Yellow
    exit 1
}
Write-Host "      + Java 8 runtime confirmed: $JavaExe" -ForegroundColor Green

# -------------------------------------------------------------------------
# [2/5] Check Existing Tunnel & Avoid Duplicate Instances
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/5] Checking for existing demo tunnel..." -ForegroundColor Yellow

$existingTunnelRunning = $false
if (Test-Path $PidFile) {
    $oldPidStr = (Get-Content $PidFile -ErrorAction SilentlyContinue).Trim()
    if ($oldPidStr -match '^\d+$') {
        $oldPid = [int]$oldPidStr
        $p = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($p -and $p.ProcessName -match "cloudflared") {
            $existingTunnelRunning = $true
            Write-Host "      + Existing demo tunnel is already active (PID: $oldPid)." -ForegroundColor Green
        }
    }
}

# Also inspect running processes for cloudflared tunnel pointing to 8080
if (-not $existingTunnelRunning) {
    $cfProcs = Get-CimInstance Win32_Process -Filter "Name = 'cloudflared.exe'" -ErrorAction SilentlyContinue
    foreach ($cp in $cfProcs) {
        if ($cp.CommandLine -match "8080") {
            $existingTunnelRunning = $true
            Set-Content -Path $PidFile -Value $cp.ProcessId
            Write-Host "      + Active cloudflared tunnel found (PID: $($cp.ProcessId))." -ForegroundColor Green
            break
        }
    }
}

# -------------------------------------------------------------------------
# [3/5] Ensure Corda Nodes and Spring Boot Backend are Online
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/5] Checking ExpenseChain Backend status..." -ForegroundColor Yellow

$HealthUrl = "http://127.0.0.1:8080/api/health"
$backendReady = $false

try {
    $resp = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
    if ($resp -and $resp.status -eq "UP") {
        $backendReady = $true
        Write-Host "      + Backend is already UP and healthy (HTTP 200)." -ForegroundColor Green
    }
} catch {}

if (-not $backendReady) {
    Write-Host "      - Backend is not running. Launching Corda nodes and backend..." -ForegroundColor DarkGray
    Write-Host "      - Invoking start-dev.ps1 in background process..." -ForegroundColor DarkGray
    
    $startDevArgs = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $RootPath "start-dev.ps1"), "-NoBrowser", "-NonInteractive")
    $devProc = Start-Process -FilePath "powershell.exe" -ArgumentList $startDevArgs -WorkingDirectory $RootPath -PassThru -WindowStyle Hidden

    Write-Host ""
    Write-Host "=======================================================================" -ForegroundColor Yellow
    Write-Host " [Public Demo] Waiting for Corda Nodes & Spring Boot Backend to initialize..." -ForegroundColor Yellow
    Write-Host " [Public Demo] Maximum wait: 5 minutes. Initial node startup may take 45-90 seconds." -ForegroundColor DarkGray
    Write-Host "=======================================================================" -ForegroundColor Yellow

    $maxWaitSec = 300
    $startTime = Get-Date
    $lastReportSec = 0

    while (-not $backendReady) {
        Start-Sleep -Seconds 2
        $elapsed = [int]((Get-Date) - $startTime).TotalSeconds

        try {
            $resp = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($resp -and $resp.status -eq "UP") {
                $backendReady = $true
                Write-Host ""
                Write-Host "      + Backend successfully initialized (Status: UP) in $elapsed seconds!" -ForegroundColor Green
                break
            }
        } catch {}

        if ($elapsed -ge ($lastReportSec + 10)) {
            $lastReportSec = $elapsed
            Write-Host "      [Public Demo] $elapsed seconds elapsed... Corda/backend still initializing..." -ForegroundColor DarkGray
        }

        if ($elapsed -ge $maxWaitSec) {
            Write-Host ""
            Write-Host "=======================================================================" -ForegroundColor Red
            Write-Host " [Public Demo] ERROR: Backend timed out after 5 minutes!               " -ForegroundColor Red
            Write-Host " Please check logs in: logs/backend/backend.log and logs/corda/        " -ForegroundColor Yellow
            Write-Host "=======================================================================" -ForegroundColor Red
            exit 1
        }
    }
}

# -------------------------------------------------------------------------
# [4/5] Start Cloudflare Quick Tunnel (Port 8080 Only)
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[4/5] Establishing Cloudflare Quick Tunnel to http://localhost:8080..." -ForegroundColor Yellow
Write-Host "      (Corda RPC/P2P ports 10002-10032 remain private and local)" -ForegroundColor DarkGray

$publicUrl = $null

if ($existingTunnelRunning -and (Test-Path $TunnelLog)) {
    $existingLog = Get-Content -Path $TunnelLog -Raw -ErrorAction SilentlyContinue
    if ($existingLog -match "(https:\/\/[a-zA-Z0-9\-]+\.trycloudflare\.com)") {
        $publicUrl = $matches[1]
        Write-Host "      + Reusing active tunnel URL from previous session." -ForegroundColor Green
    }
}

if (-not $publicUrl) {
    if (Test-Path $TunnelLog) {
        Remove-Item $TunnelLog -Force -ErrorAction SilentlyContinue
    }
    
    $cfArgs = @("tunnel", "--url", "http://localhost:8080")
    $cfOutLog = Join-Path $LogsPath "cloudflared-out.log"
    $tunnelProc = Start-Process -FilePath $cfCmdPath -ArgumentList $cfArgs -RedirectStandardError $TunnelLog -RedirectStandardOutput $cfOutLog -PassThru -WindowStyle Hidden
    
    Set-Content -Path $PidFile -Value $tunnelProc.Id
    Write-Host "      + Tunnel process started (PID: $($tunnelProc.Id)). Waiting for public URL..." -ForegroundColor DarkGray

    $cfStart = Get-Date
    $maxCfWait = 60
    while (-not $publicUrl) {
        Start-Sleep -Milliseconds 800
        if (Test-Path $TunnelLog) {
            $logContent = Get-Content -Path $TunnelLog -Raw -ErrorAction SilentlyContinue
            if ($logContent -match "(https:\/\/[a-zA-Z0-9\-]+\.trycloudflare\.com)") {
                $publicUrl = $matches[1]
                break
            }
        }

        if ($tunnelProc.HasExited) {
            Write-Host "      ! ERROR: cloudflared process terminated unexpectedly (Exit Code: $($tunnelProc.ExitCode))" -ForegroundColor Red
            if (Test-Path $TunnelLog) {
                Get-Content $TunnelLog -Tail 10 | ForEach-Object { Write-Host "        $_" -ForegroundColor DarkGray }
            }
            exit 1
        }

        $cfElapsed = [int]((Get-Date) - $cfStart).TotalSeconds
        if ($cfElapsed -gt $maxCfWait) {
            Write-Host "      ! ERROR: Timed out waiting for Cloudflare Quick Tunnel URL." -ForegroundColor Red
            exit 1
        }
    }
}

# -------------------------------------------------------------------------
# [5/5] Copy to Clipboard & Display Banner
# -------------------------------------------------------------------------
$clipboardStatus = "Not copied"
try {
    Set-Clipboard -Value $publicUrl -ErrorAction Stop
    $clipboardStatus = "Copied to clipboard!"
} catch {
    try {
        $publicUrl | clip.exe
        $clipboardStatus = "Copied to clipboard!"
    } catch {}
}

# Check optional DEMO_ACCESS_CODE
$accessCode = $env:DEMO_ACCESS_CODE
$accessCodeNotice = ""
$directLink = $publicUrl
if ($accessCode -and $accessCode.Trim() -ne "") {
    $directLink = "$publicUrl/?accessCode=$($accessCode.Trim())"
    $accessCodeNotice = " DEMO ACCESS CODE ACTIVE: Protected with code '$($accessCode.Trim())'`n Direct Link: $directLink"
}

Write-Host ""
Write-Host "========================================================" -ForegroundColor Green
Write-Host " EXPENSECHAIN PUBLIC DEMO IS LIVE                       " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""
Write-Host " Public URL:" -ForegroundColor White
Write-Host " $publicUrl" -ForegroundColor Cyan
Write-Host ""
Write-Host " Share this URL with your demo users." -ForegroundColor White
Write-Host " Status: $clipboardStatus" -ForegroundColor DarkGray
if ($accessCodeNotice) {
    Write-Host ""
    Write-Host $accessCodeNotice -ForegroundColor Yellow
}
Write-Host ""
Write-Host " IMPORTANT:" -ForegroundColor Yellow
Write-Host " Keep this laptop ON." -ForegroundColor Yellow
Write-Host " Keep this window open." -ForegroundColor Yellow
Write-Host " Corda ports are NOT exposed." -ForegroundColor Yellow
Write-Host " To stop demo: Run STOP_PUBLIC_DEMO.bat or press Ctrl+C" -ForegroundColor Gray
Write-Host "========================================================" -ForegroundColor Green
Write-Host ""

if (-not $NoBrowser) {
    Start-Process $directLink
}

# Keep the launcher monitoring until user exits
$savedPid = $null
if (Test-Path $PidFile) {
    $savedPid = (Get-Content $PidFile -ErrorAction SilentlyContinue).Trim()
}

try {
    while ($true) {
        Start-Sleep -Seconds 2
        if ($savedPid -match '^\d+$') {
            $p = Get-Process -Id ([int]$savedPid) -ErrorAction SilentlyContinue
            if (-not $p) {
                Write-Host "[Public Demo] Tunnel process has terminated." -ForegroundColor Yellow
                break
            }
        }
    }
} finally {
    Write-Host ""
    Write-Host "[Public Demo] Session closing. To stop all Corda nodes & backend, run STOP_PUBLIC_DEMO.bat" -ForegroundColor Gray
}
