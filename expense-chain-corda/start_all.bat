@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=%~dp0..\tools\jdk8"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo =======================================================================
echo          EXPENSECHAIN - Corda Enterprise DLT & Spring Boot
echo =======================================================================
echo.
echo [1/3] Starting 4 Corda 4.11 Nodes in separate consoles...
echo       - Notary Node (P2P: 10002, RPC: 10003)
echo       - Garvit Node (P2P: 10005, RPC: 10006)
echo       - Arnav Node  (P2P: 10008, RPC: 10009)
echo       - Mridul Node (P2P: 10011, RPC: 10012)
echo.
start "Corda Nodes - ExpenseChain" cmd /k "cd /d %~dp0build\nodes && call runnodes.bat"

echo [2/3] Starting ExpenseChain Web Server & RPC Bridge (Port 8080)...
echo.
start "ExpenseChain Web Server (Port 8080)" cmd /k "cd /d %~dp0 && call start_backend.bat"

echo [3/3] Waiting 6 seconds for Web UI to initialize...
timeout /t 6 /nobreak >nul
start "" "http://localhost:8080"

echo.
echo =======================================================================
echo ExpenseChain launched at http://localhost:8080
echo Keep the node and backend terminal windows open while using the app.
echo =======================================================================
