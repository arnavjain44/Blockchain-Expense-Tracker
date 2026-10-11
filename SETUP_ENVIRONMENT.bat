@echo off
setlocal
title ExpenseChain - Automated Environment Setup

echo =======================================================================
echo          EXPENSECHAIN - AUTOMATED ENVIRONMENT SETUP               
echo =======================================================================
echo.
echo Setting up Java 8, Gradle 6.9.3, Corda 4.11 nodes, and Backend...
echo Please wait...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup_environment.ps1"

echo.
echo Press any key to exit setup...
pause >nul
