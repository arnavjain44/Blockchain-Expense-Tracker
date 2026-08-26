# ==============================================================================
# EXPENSECHAIN DA2 - AUTOMATED ENVIRONMENT SETUP SCRIPT
# ==============================================================================
# Automatically installs portable Java 8, Gradle 6.9.3, deploys Corda 4.11
# nodes, and builds the Spring Boot backend JAR.
# ==============================================================================

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$ScriptDir = $PSScriptRoot
if (-not $ScriptDir) { $ScriptDir = Get-Location }

$ToolsDir = Join-Path $ScriptDir "tools"
$JdkDir = Join-Path $ToolsDir "jdk8"
$GradleDir = Join-Path $ToolsDir "gradle-6.9.3"
$CordaDir = Join-Path $ScriptDir "expense-chain-corda"

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "          EXPENSECHAIN DA2 - AUTOMATED ENVIRONMENT SETUP               " -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Create tools directory
if (-not (Test-Path $ToolsDir)) {
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
}

# 2. Download & Extract OpenJDK 8 if missing
$JavaExe = Join-Path $JdkDir "bin\java.exe"
if (-not (Test-Path $JavaExe)) {
    Write-Host "[1/4] Downloading Adoptium Temurin OpenJDK 8 x64 (Portable)..." -ForegroundColor Yellow
    $JdkZip = Join-Path $ToolsDir "jdk8.zip"
    $JdkUrl = "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u412-b08/OpenJDK8U-jdk_x64_windows_hotspot_8u412b08.zip"
    
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        & curl.exe -L -o $JdkZip $JdkUrl
    } else {
        Invoke-WebRequest -Uri $JdkUrl -OutFile $JdkZip -UseBasicParsing
    }

    Write-Host "      - Extracting OpenJDK 8..." -ForegroundColor DarkGray
    $JdkTmp = Join-Path $ToolsDir "jdk8_tmp"
    Expand-Archive -Path $JdkZip -DestinationPath $JdkTmp -Force
    $innerDir = (Get-ChildItem -Path $JdkTmp | Where-Object { $_.PSIsContainer })[0].FullName
    Move-Item -Path $innerDir -Destination $JdkDir -Force
    Remove-Item -Path $JdkTmp -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -Path $JdkZip -Force -ErrorAction SilentlyContinue
    Write-Host "      + OpenJDK 8 installed successfully!" -ForegroundColor Green
} else {
    Write-Host "[1/4] OpenJDK 8 already installed in tools\jdk8." -ForegroundColor Green
}

# 3. Download & Extract Gradle 6.9.3 if missing
$GradleBat = Join-Path $GradleDir "bin\gradle.bat"
if (-not (Test-Path $GradleBat)) {
    Write-Host "[2/4] Downloading Gradle 6.9.3 (Portable)..." -ForegroundColor Yellow
    $GradleZip = Join-Path $ToolsDir "gradle.zip"
    $GradleUrl = "https://services.gradle.org/distributions/gradle-6.9.3-bin.zip"

    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        & curl.exe -L -o $GradleZip $GradleUrl
    } else {
        Invoke-WebRequest -Uri $GradleUrl -OutFile $GradleZip -UseBasicParsing
    }

    Write-Host "      - Extracting Gradle 6.9.3..." -ForegroundColor DarkGray
    Expand-Archive -Path $GradleZip -DestinationPath $ToolsDir -Force
    Remove-Item -Path $GradleZip -Force -ErrorAction SilentlyContinue
    Write-Host "      + Gradle 6.9.3 installed successfully!" -ForegroundColor Green
} else {
    Write-Host "[2/4] Gradle 6.9.3 already installed in tools\gradle-6.9.3." -ForegroundColor Green
}

# 4. Set Environment Variables for Build
$env:JAVA_HOME = $JdkDir
$env:PATH = "$JdkDir\bin;$GradleDir\bin;$env:PATH"

# 5. Deploy Corda Nodes if not yet deployed
$NotaryJar = Join-Path $CordaDir "build\nodes\Notary\corda.jar"
if (-not (Test-Path $NotaryJar)) {
    Write-Host "[3/4] Deploying Corda 4.11 Nodes (Notary, Garvit, Arnav, Mridul)..." -ForegroundColor Yellow
    & $GradleBat -p $CordaDir deployNodes
    Write-Host "      + Corda Nodes deployed successfully!" -ForegroundColor Green
} else {
    Write-Host "[3/4] Corda nodes already deployed in expense-chain-corda\build\nodes." -ForegroundColor Green
}

# 6. Build Spring Boot Backend JAR
$BackendJar = Join-Path $CordaDir "backend\build\libs\backend-1.0.0.jar"
Write-Host "[4/4] Building ExpenseChain Spring Boot backend..." -ForegroundColor Yellow
& $GradleBat -p $CordaDir :backend:bootJar
Write-Host "      + Spring Boot backend JAR built successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host " ENVIRONMENT SETUP COMPLETE & READY!                                   " -ForegroundColor Green
Write-Host "=======================================================================" -ForegroundColor Green
Write-Host " You can now start or demo ExpenseChain:" -ForegroundColor White
Write-Host "  1. Run DEMO_SHOWCASE.bat  -> Launches app and demo presentation guide" -ForegroundColor Cyan
Write-Host "  2. Run START_DEV.bat      -> Starts development environment" -ForegroundColor Cyan
Write-Host "  3. Run STOP_DEV.bat       -> Stops all processes cleanly" -ForegroundColor Cyan
Write-Host "=======================================================================" -ForegroundColor Green
