@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=%~dp0..\tools\jdk8"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================================
echo Starting ExpenseChain Spring Boot Backend (Port 8080)
echo Serving Web UI at http://localhost:8080
echo ========================================================

if exist "%~dp0backend\build\libs\backend-1.0.0.jar" (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0backend\build\libs\backend-1.0.0.jar"
) else (
    call "%~dp0gradlew.bat" :backend:bootRun
)
