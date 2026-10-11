# =======================================================================
# ExpenseChain - Public Demo Pre-flight & Health Verification
# =======================================================================
[CmdletBinding()]
param ()

$ErrorActionPreference = "Continue"

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "       EXPENSECHAIN - PUBLIC DEMO READINESS & SECURITY AUDIT       " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host ""

$RootPath = $PSScriptRoot
$AllChecksPassed = $true

# -------------------------------------------------------------------------
# [1/6] Check Java 8 Environment
# -------------------------------------------------------------------------
Write-Host "[1/6] Verifying Java 8 Runtime..." -ForegroundColor Yellow
$Java8Path = Join-Path $RootPath "tools\jdk8\bin\java.exe"
if (Test-Path $Java8Path) {
    $jVer = & $Java8Path -version 2>&1 | Out-String
    Write-Host "      [OK] Project Java 8 found: tools\jdk8\bin\java.exe" -ForegroundColor Green
    $verLine = ($jVer -split "`n")[0].Trim()
    Write-Host "           $verLine" -ForegroundColor DarkGray
} else {
    $sysJava = Get-Command java -ErrorAction SilentlyContinue
    if ($sysJava) {
        $jVer = & java -version 2>&1 | Out-String
        if ($jVer -match "1\.8\.") {
            Write-Host "      [OK] System Java 8 found: $($sysJava.Source)" -ForegroundColor Green
        } else {
            Write-Host "      [FAIL] Project Java 8 not found in tools\jdk8, and system Java is not 1.8." -ForegroundColor Red
            Write-Host "             Run SETUP_ENVIRONMENT.bat to download Java 8." -ForegroundColor Yellow
            $AllChecksPassed = $false
        }
    } else {
        Write-Host "      [FAIL] Java 8 not found at tools\jdk8\bin\java.exe or in system PATH." -ForegroundColor Red
        Write-Host "             Please run SETUP_ENVIRONMENT.bat first." -ForegroundColor Yellow
        $AllChecksPassed = $false
    }
}

# -------------------------------------------------------------------------
# [2/6] Check Cloudflare Tunnel (cloudflared)
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/6] Verifying Cloudflare cloudflared CLI..." -ForegroundColor Yellow
$cfCmd = Get-Command cloudflared -ErrorAction SilentlyContinue
if (-not $cfCmd) {
    # Check WinGet links folder as fallback if PATH hasn't refreshed
    $wingetCf = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\cloudflared.exe"
    if (Test-Path $wingetCf) {
        $cfCmd = [PSCustomObject]@{ Source = $wingetCf }
    }
}

if ($cfCmd) {
    $cfVer = & $cfCmd.Source --version 2>&1 | Out-String
    Write-Host "      [OK] cloudflared is installed: $($cfCmd.Source)" -ForegroundColor Green
    Write-Host "           $($cfVer.Trim())" -ForegroundColor DarkGray
} else {
    Write-Host "      [FAIL] cloudflared was NOT detected on PATH." -ForegroundColor Red
    Write-Host "             Please run: INSTALL_CLOUDFLARED.bat" -ForegroundColor Yellow
    $AllChecksPassed = $false
}

# -------------------------------------------------------------------------
# [3/6] Check Backend Port (localhost:8080)
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/6] Verifying Web Backend (localhost:8080)..." -ForegroundColor Yellow
$BackendOnline = $false
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $iar = $tcp.BeginConnect("127.0.0.1", 8080, $null, $null)
    $success = $iar.AsyncWaitHandle.WaitOne(1000, $false)
    if ($success) {
        $tcp.EndConnect($iar)
        $BackendOnline = $true
    }
    $tcp.Close()
} catch {}

if ($BackendOnline) {
    Write-Host "      [OK] TCP port 8080 is listening and accepting connections." -ForegroundColor Green
} else {
    Write-Host "      [NOTICE] Port 8080 is not currently active." -ForegroundColor Yellow
    Write-Host "               (Run START_PUBLIC_DEMO.bat or START_DEV.bat to launch backend & Corda nodes)" -ForegroundColor DarkGray
}

# -------------------------------------------------------------------------
# [4/6] Check Health Endpoint (/api/health)
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[4/6] Querying Backend Health Endpoint (http://127.0.0.1:8080/api/health)..." -ForegroundColor Yellow
if ($BackendOnline) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
        if ($health -and $health.status -eq "UP") {
            Write-Host "      [OK] Backend Status: UP (HTTP 200)" -ForegroundColor Green
            Write-Host "           Service: $($health.service)" -ForegroundColor DarkGray
            
            if ($health.corda) {
                Write-Host "      Corda Nodes Readiness:" -ForegroundColor Cyan
                $allReady = $health.corda.allNodesReady
                $nodesObj = $health.corda.nodes
                foreach ($prop in $nodesObj.PSObject.Properties) {
                    $nodeName = $prop.Name
                    $isReady = $prop.Value
                    if ($isReady) {
                        Write-Host "         + $nodeName : CONNECTED" -ForegroundColor Green
                    } else {
                        Write-Host "         - $nodeName : NOT YET CONNECTED" -ForegroundColor Yellow
                    }
                }
                if ($allReady) {
                    Write-Host "      [OK] All 3 Corda participant nodes are connected." -ForegroundColor Green
                } else {
                    Write-Host "      [NOTICE] Some Corda nodes are still initializing." -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "      [WARN] Health check responded with non-UP status: $($health | ConvertTo-Json -Compress)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "      [WARN] Could not retrieve health endpoint: $($_.Exception.Message)" -ForegroundColor Yellow
    }
} else {
    Write-Host "      [INFO] Skipped (Backend is not currently running)." -ForegroundColor DarkGray
}

# -------------------------------------------------------------------------
# [5/6] Security Audit: Verify Corda Ports Are NOT Internet-Facing
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[5/6] Security Audit: Inspecting Listening Network Interfaces..." -ForegroundColor Yellow
Write-Host "      Target rule: ONLY Port 8080 may be exposed. Corda RPC/P2P ports must be loopback only." -ForegroundColor DarkGray

$CordaPorts = @(10002, 10003, 10005, 10006, 10008, 10009, 10011, 10012, 10023, 10026, 10029, 10032)
$LoopbackAddresses = @("127.0.0.1", "::1", "localhost")

$allListen = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue
$cordaSecurityIssue = $false
$activeCordaCount = 0

if ($allListen) {
    foreach ($conn in $allListen) {
        $port = $conn.LocalPort
        $addr = $conn.LocalAddress

        if ($CordaPorts -contains $port) {
            $activeCordaCount++
            if ($LoopbackAddresses -contains $addr) {
                Write-Host "      [SAFE] Corda Port $port is bound exclusively to local loopback ($addr)." -ForegroundColor Green
            } else {
                Write-Host "      ==========================================================" -ForegroundColor Red
                Write-Host "      [SECURITY WARNING] Corda Port $port is listening on NON-LOOPBACK address: $addr" -ForegroundColor Red
                Write-Host "      Corda ports (10002-10032) must NOT be exposed to the public internet!" -ForegroundColor Red
                Write-Host "      Ensure Cloudflare tunnel exposes ONLY http://localhost:8080." -ForegroundColor Red
                Write-Host "      ==========================================================" -ForegroundColor Red
                $cordaSecurityIssue = $true
            }
        }
    }
}

if ($activeCordaCount -eq 0) {
    Write-Host "      [INFO] No active Corda ports found currently (Nodes are not running)." -ForegroundColor DarkGray
} elseif (-not $cordaSecurityIssue) {
    Write-Host "      [OK] Verified all $activeCordaCount active Corda RPC/P2P ports are private (loopback-only)." -ForegroundColor Green
}

# -------------------------------------------------------------------------
# [6/6] Check Cloudflare Tunnel State
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "[6/6] Checking Cloudflare Tunnel State..." -ForegroundColor Yellow
$cfProcs = Get-CimInstance Win32_Process -Filter "Name = 'cloudflared.exe'" -ErrorAction SilentlyContinue
$activeTunnel = $false

if ($cfProcs) {
    foreach ($p in $cfProcs) {
        if ($p.CommandLine -match "tunnel") {
            $activeTunnel = $true
            Write-Host "      [ACTIVE] cloudflared tunnel is running (PID: $($p.ProcessId))" -ForegroundColor Cyan
        }
    }
}

$pidFile = Join-Path $RootPath "logs\cloudflared-demo.pid"
if (Test-Path $pidFile) {
    $savedPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    Write-Host "      [INFO] Demo launcher PID file: PID $savedPid" -ForegroundColor DarkGray
}

if (-not $activeTunnel) {
    Write-Host "      [INFO] No active Cloudflare tunnel detected." -ForegroundColor DarkGray
}

# -------------------------------------------------------------------------
# Summary
# -------------------------------------------------------------------------
Write-Host ""
Write-Host "=======================================================================" -ForegroundColor Cyan
if ($AllChecksPassed -and (-not $cordaSecurityIssue)) {
    Write-Host " AUDIT SUMMARY: ALL SYSTEM PREREQUISITES READY                         " -ForegroundColor Green
    Write-Host "=======================================================================" -ForegroundColor Cyan
    Write-Host " * To start the temporary public demo, run: START_PUBLIC_DEMO.bat" -ForegroundColor White
    Write-Host " * To stop the temporary public demo, run:  STOP_PUBLIC_DEMO.bat" -ForegroundColor White
} else {
    Write-Host " AUDIT SUMMARY: ISSUES DETECTED. PLEASE REVIEW MESSAGES ABOVE          " -ForegroundColor Red
    Write-Host "=======================================================================" -ForegroundColor Cyan
}
Write-Host ""
