$ProgressPreference = 'SilentlyContinue'
Write-Host "Creating tools directory..."
New-Item -ItemType Directory -Force -Path 'tools' | Out-Null

if (-not (Test-Path 'tools\jdk8\bin\java.exe')) {
    Write-Host "Downloading OpenJDK 8..."
    curl.exe -L -o "tools\jdk8.zip" "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u412-b08/OpenJDK8U-jdk_x64_windows_hotspot_8u412b08.zip"
    Write-Host "Extracting OpenJDK 8..."
    Expand-Archive -Path "tools\jdk8.zip" -DestinationPath "tools\jdk8_tmp" -Force
    $inner = (Get-ChildItem "tools\jdk8_tmp" | Where-Object { $_.PSIsContainer })[0].FullName
    Move-Item "$inner" "tools\jdk8"
    Remove-Item "tools\jdk8_tmp" -Recurse -Force
    Remove-Item "tools\jdk8.zip" -Force
    Write-Host "OpenJDK 8 ready!"
}

if (-not (Test-Path 'tools\gradle-6.9.3\bin\gradle.bat')) {
    Write-Host "Downloading Gradle 6.9.3..."
    curl.exe -L -o "tools\gradle.zip" "https://services.gradle.org/distributions/gradle-6.9.3-bin.zip"
    Write-Host "Extracting Gradle 6.9.3..."
    Expand-Archive -Path "tools\gradle.zip" -DestinationPath "tools" -Force
    Remove-Item "tools\gradle.zip" -Force
    Write-Host "Gradle 6.9.3 ready!"
}

Write-Host "Testing Java:"
& "tools\jdk8\bin\java.exe" -version

Write-Host "Testing Gradle:"
& "tools\gradle-6.9.3\bin\gradle.bat" -v
