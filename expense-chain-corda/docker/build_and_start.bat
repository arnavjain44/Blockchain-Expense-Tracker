@echo off
setlocal
cd /d "%~dp0"

echo ========================================================
echo  ExpenseChain: Building CorDapps and Docker Nodes
echo ========================================================

REM Locate JDK 8
if exist "%~dp0..\..\tools\jdk8" (
    set "JAVA_HOME=%~dp0..\..\tools\jdk8"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo Building CorDapps and deploying Docker node configs via Gradle...
call "%~dp0..\gradlew.bat" -p "%~dp0.." deployDockerNodes -x test
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Gradle deployDockerNodes failed!
    exit /b %ERRORLEVEL%
)

echo ========================================================
echo  Starting Corda 4.11 Network via Docker Compose
echo ========================================================
docker compose -f "%~dp0docker-compose.yml" up -d
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker compose failed to start!
    exit /b %ERRORLEVEL%
)

echo ========================================================
echo  Corda Nodes Status:
echo ========================================================
docker compose -f "%~dp0docker-compose.yml" ps
echo.
echo Network started successfully!
echo To view logs: docker compose -f docker-compose.yml logs -f
