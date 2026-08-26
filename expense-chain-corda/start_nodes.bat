@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=%~dp0..\tools\jdk8"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================================
echo Cleaning stale node locks and process-id files...
echo ========================================================
for /r "%~dp0build\nodes" %%f in (process-id) do if exist "%%f" del /f /q "%%f"

echo ========================================================
echo Starting 4 Corda 4.11 Nodes:
echo   1. Notary  (P2P: 10002, RPC: 10003)
echo   2. Garvit  (P2P: 10005, RPC: 10006)
echo   3. Arnav   (P2P: 10008, RPC: 10009)
echo   4. Mridul  (P2P: 10011, RPC: 10012)
echo ========================================================
cd build\nodes
call runnodes.bat
