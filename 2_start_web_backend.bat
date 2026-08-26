@echo off
setlocal
cd /d "%~dp0expense-chain-corda"
start "" "http://localhost:8080"
call start_backend.bat
