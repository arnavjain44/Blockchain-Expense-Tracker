$p = Start-Process -FilePath "c:\Users\Garvit\Downloads\blokchaain\tools\jdk8\bin\java.exe" -ArgumentList "-jar", "c:\Users\Garvit\Downloads\blokchaain\expense-chain-corda\backend\build\libs\backend-1.0.0.jar" -PassThru
try {
    Start-Sleep -Seconds 6
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/groups" -TimeoutSec 5
    Write-Host "HTTP_STATUS: 200 OK"
    Write-Host "Groups count:" $res.Count
    $indexHtml = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 5
    Write-Host "Web UI Status:" $indexHtml.StatusCode
} finally {
    Stop-Process -Id $p.Id -Force
}
