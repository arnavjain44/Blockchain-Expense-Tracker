# =======================================================================
# ExpenseChain DA2 - Development Environment Reset Script
# =======================================================================
$ErrorActionPreference = "SilentlyContinue"

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host " Resetting ExpenseChain Dev Environment and State...                   " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan

$RootPath = $PSScriptRoot
$StopScript = Join-Path $RootPath "stop-dev.ps1"
$LogsPath = Join-Path $RootPath "logs"

# 1. Stop all running dev processes
Write-Host "[1/2] Stopping any active dev processes..." -ForegroundColor Yellow
if (Test-Path $StopScript) {
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File $StopScript
}

# 2. Clean logs
Write-Host "[2/2] Clearing stale logs..." -ForegroundColor Yellow
if (Test-Path $LogsPath) {
    Get-ChildItem -Path $LogsPath -Recurse -Include "*.log" | Remove-Item -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host " DEV ENVIRONMENT RESET COMPLETE                                        " -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host " * In-memory backend store reset to 100% clean state on next launch." -ForegroundColor White
Write-Host " * Demo environment will generate a brand new randomized dataset." -ForegroundColor White
Write-Host " * Run START_DEV.bat or .\start-dev.ps1 to start the application." -ForegroundColor Gray
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host ""
