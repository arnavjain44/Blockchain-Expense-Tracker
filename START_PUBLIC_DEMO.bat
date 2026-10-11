@echo off
setlocal
cd /d "%~dp0"

echo =======================================================================
echo          ExpenseChain - Starting Public Demo (Cloudflare Tunnel)  
echo =======================================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-public-demo.ps1" %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Public demo launcher encountered an issue.
    pause
)
