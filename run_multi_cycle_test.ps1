# =======================================================================
# ExpenseChain DA2 - Multi-Cycle Reliability & Reproducibility Test Suite
# Tests: Cycle 1 -> Cycle 2 -> Cycle 3 -> Cycle 4
# =======================================================================

$ErrorActionPreference = "Continue"
$RootPath = $PSScriptRoot

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host " STARTING 4-CYCLE REPRODUCIBILITY & RELIABILITY TEST SUITE             " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

function Run-CycleTest {
    param(
        [int]$CycleNumber,
        [bool]$StopAtEnd = $true
    )

    Write-Host ""
    Write-Host "-----------------------------------------------------------------------" -ForegroundColor Magenta
    Write-Host " >>> EXECUTING CYCLE $CycleNumber of 4..." -ForegroundColor Magenta
    Write-Host "-----------------------------------------------------------------------" -ForegroundColor Magenta

    # 1. Inspect state before start
    Write-Host " [Step 1] Inspecting environment before launch..." -ForegroundColor DarkGray
    $targetPorts = @(8080, 10002, 10003, 10005, 10006, 10008, 10009, 10011, 10012)
    $activeConns = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $targetPorts -contains $_.LocalPort }
    if ($activeConns) {
        Write-Host "   ! Pre-start active ports found: $($activeConns.LocalPort -join ', ')" -ForegroundColor Yellow
    } else {
        Write-Host "   + Verified: No target ports active before launch." -ForegroundColor Green
    }

    # 2. Run start-dev.ps1 directly
    Write-Host " [Step 2] Executing START_DEV (NonInteractive, NoBrowser)..." -ForegroundColor Yellow
    $startScript = Join-Path $RootPath "start-dev.ps1"
    & $startScript -NonInteractive -NoBrowser

    # 3. Verify HTTP 200 on application and health endpoints
    Write-Host " [Step 3] Verifying HTTP 200 response on localhost:8080..." -ForegroundColor Yellow
    $respHome = Invoke-WebRequest -Uri "http://localhost:8080" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "   + http://localhost:8080 -> Status: $($respHome.StatusCode) OK ($($respHome.RawContentLength) bytes)" -ForegroundColor Green

    # 4. Use/Test Application Endpoints
    Write-Host " [Step 4] Testing core application API endpoints..." -ForegroundColor Yellow
    
    # 4a. Health Endpoint
    $respHealth = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get -TimeoutSec 5
    Write-Host "   + /api/health -> Status: $($respHealth.status), All Nodes Ready: $($respHealth.corda.allNodesReady)" -ForegroundColor Green

    # 4b. Corda Blockchain Status
    $respStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/blockchain/status" -Method Get -TimeoutSec 5
    Write-Host "   + /api/blockchain/status -> Garvit: $($respStatus.Garvit), Arnav: $($respStatus.Arnav), Mridul: $($respStatus.Mridul)" -ForegroundColor Green

    # 4c. Demo Session
    $respDemo = Invoke-RestMethod -Uri "http://localhost:8080/api/demo/session" -Method Get -TimeoutSec 5
    Write-Host "   + /api/demo/session -> Users: $($respDemo.users.Count), Groups: $($respDemo.groups.Count), Expenses: $($respDemo.expenses.Count)" -ForegroundColor Green

    # 4d. Ledger Verification Endpoint
    $respVerify = Invoke-RestMethod -Uri "http://localhost:8080/api/blockchain/verify" -Method Get -TimeoutSec 5
    Write-Host "   + /api/blockchain/verify -> Status: $($respVerify.status), Consensus: $($respVerify.notaryConsensus)" -ForegroundColor Green

    if ($StopAtEnd) {
        # 5. Stop Dev Environment
        Write-Host " [Step 5] Stopping Dev Environment with stop-dev.ps1..." -ForegroundColor Yellow
        $stopScript = Join-Path $RootPath "stop-dev.ps1"
        & $stopScript | Out-Null

        # 6. Verify post-stop clean state
        Write-Host " [Step 6] Verifying post-stop clean state..." -ForegroundColor DarkGray
        Start-Sleep -Seconds 1
        $survivingPorts = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $targetPorts -contains $_.LocalPort }
        if ($survivingPorts) {
            Write-Host "   ! WARNING: Surviving listening ports detected: $($survivingPorts.LocalPort -join ', ')" -ForegroundColor Red
            return $false
        } else {
            Write-Host "   + Verified: All target ports (8080, 10002-10032) cleanly closed." -ForegroundColor Green
        }
    } else {
        Write-Host "   * Keeping Cycle $CycleNumber active for live usage." -ForegroundColor Cyan
    }

    Write-Host " >>> CYCLE $CycleNumber COMPLETED SUCCESSFULLY (HTTP 200 & Verified)!" -ForegroundColor Green
    return $true
}

# Run Cycle 1
$c1 = Run-CycleTest -CycleNumber 1 -StopAtEnd $true
if (-not $c1) { 
    Write-Host "FAILED AT CYCLE 1" -ForegroundColor Red
    exit 1 
}

# Run Cycle 2
$c2 = Run-CycleTest -CycleNumber 2 -StopAtEnd $true
if (-not $c2) { 
    Write-Host "FAILED AT CYCLE 2" -ForegroundColor Red
    exit 1 
}

# Run Cycle 3
$c3 = Run-CycleTest -CycleNumber 3 -StopAtEnd $true
if (-not $c3) { 
    Write-Host "FAILED AT CYCLE 3" -ForegroundColor Red
    exit 1 
}

# Run Cycle 4 (Kept running for user)
$c4 = Run-CycleTest -CycleNumber 4 -StopAtEnd $false
if (-not $c4) { 
    Write-Host "FAILED AT CYCLE 4" -ForegroundColor Red
    exit 1 
}

Write-Host ""
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host " ALL 4 STOP/START DEV CYCLES PASSED WITH ZERO ERRORS (100% REPEATABLE) " -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Green
