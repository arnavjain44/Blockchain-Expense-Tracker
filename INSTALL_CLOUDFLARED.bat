@echo off
setlocal enabledelayedexpansion

echo =======================================================================
echo          Cloudflare Tunnel (cloudflared) Installer for Windows        
echo =======================================================================
echo.

:: 1. Check if cloudflared is already installed and available on PATH
where cloudflared >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] cloudflared is already installed and available in PATH:
    cloudflared --version
    echo.
    echo Everything is ready! You can now run START_PUBLIC_DEMO.bat
    echo.
    goto :END
)

:: Also check default winget installation location in case PATH is not updated in current session
set "WINGET_DEFAULT_PATH=%LOCALAPPDATA%\Microsoft\WinGet\Links\cloudflared.exe"
if exist "%WINGET_DEFAULT_PATH%" (
    echo [INFO] cloudflared found at: %WINGET_DEFAULT_PATH%
    "%WINGET_DEFAULT_PATH%" --version
    echo.
    echo Note: If 'cloudflared' command is not recognized, please open a NEW command prompt or PowerShell window.
    goto :END
)

echo [INFO] cloudflared is not currently detected on PATH.
echo [1/2] Attempting installation via Windows Package Manager (winget)...
echo.

where winget >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Executing: winget install --id Cloudflare.cloudflared --accept-source-agreements --accept-package-agreements
    winget install --id Cloudflare.cloudflared --accept-source-agreements --accept-package-agreements
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo =======================================================================
        echo [SUCCESS] cloudflared was installed successfully!
        echo =======================================================================
        echo.
        echo IMPORTANT: If this window was already open prior to installation,
        echo Windows environment PATH changes require opening a NEW Command Prompt
        echo or PowerShell window.
        echo.
        echo Verifying in current session...
        where cloudflared >nul 2>nul
        if %ERRORLEVEL% EQU 0 (
            cloudflared --version
        ) else (
            echo Please open a NEW PowerShell/CMD window and run:
            echo   cloudflared --version
            echo   START_PUBLIC_DEMO.bat
        )
        goto :END
    ) else (
        echo [WARNING] winget installation failed or was cancelled.
    )
) else (
    echo [WARNING] winget is not available on this system.
)

echo.
echo =======================================================================
echo [MANUAL INSTALLATION FALLBACK]
echo =======================================================================
echo Please download and install Cloudflare's official cloudflared for Windows:
echo.
echo Option A (Official MSI Installer - Recommended):
echo   https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.msi
echo.
echo Option B (Official Standalone Executable):
echo   https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
echo.
echo After installation, open a NEW terminal and verify:
echo   cloudflared --version
echo =======================================================================
echo.

:END
pause
