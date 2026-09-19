@echo off
setlocal
cd /d "%~dp0"
call "%~dp0docker\build_and_start.bat"
