@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-dev.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Startup encountered an issue. Check logs directory.
    pause
)
