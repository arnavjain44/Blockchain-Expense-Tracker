@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=%~dp0..\tools\jdk8"
set "PATH=%JAVA_HOME%\bin;%~dp0..\tools\gradle-6.9.3\bin;%PATH%"

echo ========================================================
echo Building ExpenseChain CorDapps (Java 8 + Gradle 6.9.3)
echo ========================================================
call "%~dp0..\tools\gradle-6.9.3\bin\gradle.bat" wrapper --gradle-version 6.9.3
if %ERRORLEVEL% NEQ 0 (
    echo Error generating Gradle wrapper!
    exit /b %ERRORLEVEL%
)

echo Building and compiling CorDapps...
call "%~dp0gradlew.bat" clean build -x test
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo Deploying Corda 4.11 Nodes...
call "%~dp0gradlew.bat" deployNodes
if %ERRORLEVEL% NEQ 0 (
    echo Node deployment failed!
    exit /b %ERRORLEVEL%
)

echo ========================================================
echo Nodes deployed successfully to build\nodes\ !
echo ========================================================
