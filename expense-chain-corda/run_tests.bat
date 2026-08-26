@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=%~dp0..\tools\jdk8"
set "PATH=%JAVA_HOME%\bin;%~dp0..\tools\gradle-6.9.3\bin;%PATH%"

echo ========================================================
echo Running Corda Contract and Flow JUnit Tests
echo ========================================================
call "%~dp0gradlew.bat" test --info
if %ERRORLEVEL% NEQ 0 (
    echo Tests failed!
    exit /b %ERRORLEVEL%
)

echo ========================================================
echo ALL TESTS PASSED SUCCESSFULLY!
echo ========================================================
