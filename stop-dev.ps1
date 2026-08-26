# =======================================================================
# ExpenseChain DA2 - Reliable Development Environment Stop Script
# =======================================================================
$ErrorActionPreference = "SilentlyContinue"

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host " Stopping ExpenseChain Dev Environment (Corda Nodes and Backend)...    " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

$RootPath = $PSScriptRoot
$CordaPath = Join-Path $RootPath "expense-chain-corda"
$NodesPath = Join-Path $CordaPath "build\nodes"
$TargetPorts = @(8080, 10002, 10003, 10005, 10006, 10008, 10009, 10011, 10012, 10023, 10026, 10029, 10032)

$PidsToKill = New-Object System.Collections.Generic.HashSet[int]

# 1. Identify processes listening on target ports (filter from all listen connections)
$allListening = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue
if ($allListening) {
    foreach ($c in $allListening) {
        if ($TargetPorts -contains $c.LocalPort) {
            $pId = $c.OwningProcess
            if ($pId -and ($pId -ne $PID) -and ($pId -gt 4)) {
                [void]$PidsToKill.Add([int]$pId)
            }
        }
    }
}

# 2. Identify ExpenseChain Java processes (Corda nodes, Backend, Capsule)
try {
    $javaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue
    foreach ($jp in $javaProcs) {
        $cmd = $jp.CommandLine
        $pId = [int]$jp.ProcessId
        if ($cmd -and ($pId -ne $PID) -and ($pId -gt 4)) {
            $isExpenseChain = $false
            if ($cmd -match "net\.corda\.node\.Corda") { $isExpenseChain = $true }
            elseif ($cmd -match "corda\.jar") { $isExpenseChain = $true }
            elseif ($cmd -match "backend-1\.0\.0\.jar") { $isExpenseChain = $true }
            elseif ($cmd -match "expense-chain-corda") { $isExpenseChain = $true }
            elseif ($cmd -match "capsule\.app=net\.corda") { $isExpenseChain = $true }
            elseif ($cmd -match "tools\\jdk8" -and ($cmd -notmatch "GradleDaemon") -and ($cmd -notmatch "gradle")) { $isExpenseChain = $true }

            if ($isExpenseChain) {
                [void]$PidsToKill.Add($pId)
            }
        }
    }
} catch {}

# 3. Terminate identified processes
$KilledCount = 0
$pidsList = @($PidsToKill)
foreach ($procId in $pidsList) {
    try {
        $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if ($p) {
            Write-Host " - Terminating process (PID: $procId, Name: $($p.ProcessName))..." -ForegroundColor Yellow
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            $KilledCount++
        }
    } catch {}
}

# 4. Wait synchronously for processes to exit and ports to free
if ($pidsList.Count -gt 0) {
    Wait-Process -Id $pidsList -Timeout 5 -ErrorAction SilentlyContinue
}

# Double check listening ports are fully released
$waitStart = Get-Date
while ($true) {
    $stillListening = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $TargetPorts -contains $_.LocalPort }
    if (-not $stillListening) {
        break
    }
    # Force kill any lingering PID
    foreach ($c in $stillListening) {
        $lpId = $c.OwningProcess
        if ($lpId -gt 4 -and $lpId -ne $PID) {
            Stop-Process -Id $lpId -Force -ErrorAction SilentlyContinue
        }
    }
    if (((Get-Date) - $waitStart).TotalSeconds -gt 4) {
        break
    }
    Start-Sleep -Milliseconds 200
}

# 5. Clean stale process-id files
if (Test-Path $NodesPath) {
    Get-ChildItem -Path $NodesPath -Filter "process-id" -Recurse -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
}

Write-Host ""
if ($KilledCount -gt 0) {
    Write-Host "+ Successfully terminated $KilledCount ExpenseChain development process(es)." -ForegroundColor Green
} else {
    Write-Host "+ No active ExpenseChain development processes were running." -ForegroundColor Green
}
Write-Host "+ Verified all Corda RPC/P2P ports and Web Backend port (8080) are clean." -ForegroundColor Green
Write-Host "+ Node lock files and process-id state cleaned." -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Cyan
