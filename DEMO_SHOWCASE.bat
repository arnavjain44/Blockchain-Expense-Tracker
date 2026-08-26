@echo off
setlocal
title ExpenseChain DA2 - Interactive Demo & Presentation Showcase

echo =======================================================================
echo          EXPENSECHAIN DA2 - LIVE DEMO & PRESENTATION LAUNCHER         
echo =======================================================================
echo.
echo Checking development environment status...

powershell -NoProfile -ExecutionPolicy Bypass -Command "$c = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if (-not $c) { exit 1 } else { exit 0 }"
if %ERRORLEVEL% NEQ 0 (
    echo [!] Development environment is not running. Launching start-dev.ps1...
    echo.
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-dev.ps1"
) else (
    echo [+] Application and Corda nodes are already active and healthy!
    echo [+] Opening live showcase in your browser...
    start http://localhost:8080
    echo.
    echo =======================================================================
    echo  DEMO APPLICATION RUNNING AT: http://localhost:8080
    echo  HEALTH STATUS:               http://localhost:8080/api/health
    echo =======================================================================
    echo.
    echo  TIPS FOR PRESENTING THE DEMO:
    echo  1. Click 'Try Demo' on the homepage to explore the interactive sandbox.
    echo  2. Switch between users (Garvit, Arnav, Mridul) in the top navigation.
    echo  3. Add a group expense to trigger Corda smart contracts.
    echo  4. Click 'Blockchain' tab to inspect SHA-256 TxIDs and London Notary.
    echo  5. Click 'Verify Ledger' to demonstrate tamper-proof DLT consensus.
    echo.
    echo  Press any key to open the interactive Demo Guide, or close this window.
    pause >nul
    if exist "%~dp0DEMO_GUIDE.md" (
        start notepad "%~dp0DEMO_GUIDE.md"
    )
)
