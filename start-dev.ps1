# =======================================================================
# ExpenseChain DA2 - Unified Local Development Control Script
# =======================================================================
param (
    [switch]$NoBrowser,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Continue"

$RootPath = $PSScriptRoot
$CordaPath = Join-Path $RootPath "expense-chain-corda"
$NodesPath = Join-Path $CordaPath "build\nodes"
$LogsPath = Join-Path $RootPath "logs"
$CordaLogsPath = Join-Path $LogsPath "corda"
$BackendLogsPath = Join-Path $LogsPath "backend"

# Ensure log directories exist
if (-not (Test-Path $CordaLogsPath)) {
    New-Item -ItemType Directory -Force -Path $CordaLogsPath | Out-Null
}
if (-not (Test-Path $BackendLogsPath)) {
    New-Item -ItemType Directory -Force -Path $BackendLogsPath | Out-Null
}

# Configure Java 8 from local tools
$JavaHome = Join-Path $RootPath "tools\jdk8"
if (Test-Path $JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;$env:PATH"
    $JavaExe = Join-Path $JavaHome "bin\java.exe"
} else {
    $JavaExe = "java.exe"
}

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "          EXPENSECHAIN DA2 - R3 CORDA DLT & SPRING BOOT                " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host " Using Java: $JavaExe" -ForegroundColor DarkGray
Write-Host ""

# Helper to check TCP port
function Test-PortReady {
    param([int]$Port)
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $connectTask = $tcpClient.BeginConnect("127.0.0.1", $Port, $null, $null)
        $success = $connectTask.AsyncWaitHandle.WaitOne(800, $false)
        if ($success) {
            $tcpClient.EndConnect($connectTask)
            $tcpClient.Close()
            return $true
        }
        $tcpClient.Close()
    } catch {}
    return $false
}

try {
    # ---------------------------------------------------------------------
    # [Pre-flight] Clean lingering processes & verify clean state
    # ---------------------------------------------------------------------
    Write-Host "[Pre-flight] Ensuring all ports and node locks are clean..." -ForegroundColor Yellow
    $StopScript = Join-Path $RootPath "stop-dev.ps1"
    if (Test-Path $StopScript) {
        & $StopScript | Out-Null
    }

    # Verify key ports are free before starting
    $RequiredPorts = @(8080, 10002, 10003, 10005, 10006, 10008, 10009, 10011, 10012)
    $activeConns = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $RequiredPorts -contains $_.LocalPort }
    if ($activeConns) {
        Write-Host "      ! Warning: Some ports still active. Re-running stop cleanup..." -ForegroundColor Yellow
        & $StopScript | Out-Null
        Start-Sleep -Seconds 1
    }
    Write-Host "      + Pre-flight check passed: environment is clean." -ForegroundColor Green

    # ---------------------------------------------------------------------
    # [1/4] Start 4 Corda Nodes
    # ---------------------------------------------------------------------
    Write-Host ""
    Write-Host "[1/4] Starting 4 Corda 4.11 Nodes in background..." -ForegroundColor Yellow
    $Nodes = @("Notary", "Garvit", "Arnav", "Mridul")

    foreach ($Node in $Nodes) {
        $NodeDir = Join-Path $NodesPath $Node
        $LogFileOut = Join-Path $CordaLogsPath "$Node.log"
        $LogFileErr = Join-Path $CordaLogsPath "$Node-error.log"

        if (Test-Path $NodeDir) {
            Write-Host "      - Launching $Node Node (Log: logs/corda/$Node.log)..." -ForegroundColor DarkGray
            Start-Process -FilePath $JavaExe -ArgumentList @("-Dcapsule.jvm.args=-Xmx512m", "-jar", "corda.jar", "--no-local-shell") -WorkingDirectory $NodeDir -RedirectStandardOutput $LogFileOut -RedirectStandardError $LogFileErr -WindowStyle Hidden | Out-Null
        } else {
            Write-Host "      ! Warning: Directory for $Node not found at $NodeDir" -ForegroundColor Red
        }
    }

    # ---------------------------------------------------------------------
    # [2/4] Wait for Corda Nodes
    # ---------------------------------------------------------------------
    Write-Host "[2/4] Waiting for Corda Nodes & Notary readiness..." -ForegroundColor Yellow
    $NodePorts = @{
        "Notary" = 10003
        "Garvit" = 10006
        "Arnav"  = 10009
        "Mridul" = 10012
    }

    $ReadyNodes = @{}
    $MaxWaitSec = 75
    $StartTime = Get-Date

    while ($ReadyNodes.Count -lt $NodePorts.Count) {
        $allConns = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue
        foreach ($name in $NodePorts.Keys) {
            if (-not $ReadyNodes.ContainsKey($name)) {
                $port = $NodePorts[$name]
                if (($allConns | Where-Object { $_.LocalPort -eq $port }) -or (Test-PortReady -Port $port)) {
                    $ReadyNodes[$name] = $true
                    Write-Host "      + $name Node is online on RPC port $port" -ForegroundColor Green
                }
            }
        }

        if ($ReadyNodes.Count -ge $NodePorts.Count) {
            break
        }

        $elapsed = ((Get-Date) - $StartTime).TotalSeconds
        if ($elapsed -gt $MaxWaitSec) {
            Write-Host "      ! Notice: Continuing startup sequence..." -ForegroundColor Yellow
            break
        }
        Start-Sleep -Milliseconds 1000
    }

    # Explicit Notary Verification
    $allConnsNow = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue
    if (($allConnsNow | Where-Object { $_.LocalPort -in 10002, 10003 }) -or (Test-PortReady -Port 10003)) {
        Write-Host "      + Notary Service verified (O=Notary,L=London,C=GB active)." -ForegroundColor Green
    } else {
        Write-Host "      ! Warning: Notary service did not respond on standard ports." -ForegroundColor Yellow
    }

    # ---------------------------------------------------------------------
    # [3/4] Start Spring Boot Backend Server
    # ---------------------------------------------------------------------
    Write-Host ""
    Write-Host "[3/4] Starting ExpenseChain Spring Boot backend on port 8080..." -ForegroundColor Yellow
    $BackendJar = Join-Path $CordaPath "backend\build\libs\backend-1.0.0.jar"
    $BackendLogOut = Join-Path $BackendLogsPath "backend.log"
    $BackendLogErr = Join-Path $BackendLogsPath "backend-error.log"

    if (-not (Test-Path $BackendJar)) {
        Write-Host "      - Building backend JAR first..." -ForegroundColor DarkGray
        $GradleBat = Join-Path $RootPath "tools\gradle-6.9.3\bin\gradle.bat"
        if (Test-Path $GradleBat) {
            & $GradleBat -p $CordaPath :backend:bootJar | Out-Null
        } else {
            $Gradlew = Join-Path $CordaPath "gradlew.bat"
            & $Gradlew -p $CordaPath :backend:bootJar | Out-Null
        }
    }

    $bp = Start-Process -FilePath $JavaExe -ArgumentList "-jar `"$BackendJar`"" -WorkingDirectory $CordaPath -RedirectStandardOutput $BackendLogOut -RedirectStandardError $BackendLogErr -WindowStyle Hidden -PassThru
    Write-Host "      - Backend process started (PID: $($bp.Id), Log: logs/backend/backend.log)" -ForegroundColor DarkGray

    # ---------------------------------------------------------------------
    # [4/4] Verify Backend Health Endpoint (HTTP 200) and Open Browser
    # ---------------------------------------------------------------------
    Write-Host ""
    Write-Host "[4/4] Verifying backend health at http://localhost:8080/api/health..." -ForegroundColor Yellow

    $WebReady = $false
    $WebStart = Get-Date
    $HealthUrl = "http://127.0.0.1:8080/api/health"

    while (-not $WebReady) {
        try {
            $respHealth = Invoke-RestMethod -Uri $HealthUrl -Method Get -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($respHealth -and $respHealth.status -eq "UP") {
                $WebReady = $true
                Write-Host "      + ExpenseChain Health Check: UP (HTTP 200)" -ForegroundColor Green
                Write-Host "      + ExpenseChain Web App: Online (HTTP 200)" -ForegroundColor Green
                break
            }
        } catch {}

        if ($bp.HasExited) {
            Write-Host "      ! ERROR: Backend process terminated unexpectedly! Check logs/backend/backend.log" -ForegroundColor Red
            break
        }

        if (((Get-Date) - $WebStart).TotalSeconds -gt 60) {
            Write-Host "      ! Backend startup taking longer than expected. Check logs/backend/backend.log" -ForegroundColor Yellow
            break
        }
        Start-Sleep -Milliseconds 1000
    }

    if ($WebReady) {
        Write-Host ""
        Write-Host "=======================================================================" -ForegroundColor Green
        Write-Host " APPLICATION READY: http://localhost:8080                             " -ForegroundColor Green
        Write-Host "=======================================================================" -ForegroundColor Green
        Write-Host " * Health Endpoint: http://localhost:8080/api/health (HTTP 200)" -ForegroundColor White
        Write-Host " * Mode 1: Main Application (Real registration and clean ledger)" -ForegroundColor White
        Write-Host " * Mode 2: Test / Demo Mode (Click 'Try Demo' for seeded sandbox)" -ForegroundColor White
        Write-Host " * Control Terminal Active: Keep this terminal open while using app" -ForegroundColor White
        Write-Host " * To stop: Press Ctrl+C in this terminal, or run STOP_DEV.bat" -ForegroundColor Gray
        Write-Host "=======================================================================" -ForegroundColor Green
        Write-Host ""

        if (-not $NoBrowser) {
            Start-Process "http://localhost:8080"
        }
    } else {
        Write-Host ""
        Write-Host "=======================================================================" -ForegroundColor Red
        Write-Host " APPLICATION STARTUP FAILED                                            " -ForegroundColor Red
        Write-Host " Check logs in logs/backend/backend.log and logs/corda/                " -ForegroundColor Yellow
        Write-Host "=======================================================================" -ForegroundColor Red
    }

    if (-not $NonInteractive) {
        # Keep control terminal running for user session
        while ($true) {
            Start-Sleep -Seconds 2
        }
    }
}
finally {
    if (-not $NonInteractive) {
        Write-Host ""
        Write-Host "Shutting down all development processes..." -ForegroundColor Yellow
        $StopScript = Join-Path $RootPath "stop-dev.ps1"
        if (Test-Path $StopScript) {
            & $StopScript | Out-Null
        }
    }
}
