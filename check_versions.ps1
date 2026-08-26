$versions = @('5.0.12', '5.0.11', '5.0.10', '5.0.9', '5.0.8', '5.0.7', '5.0.6', '5.0.5', '5.0.4', '5.0.3', '5.0.2', '5.0.1', '5.0.0', '4.0.45', '4.0.44', '4.0.40', '4.0.35')
foreach ($v in $versions) {
    $url = "https://download.corda.net/maven/corda-releases/net/corda/plugins/cordformation/$v/cordformation-$v.pom"
    try {
        $resp = Invoke-WebRequest -Uri $url -Method Head -UseBasicParsing -ErrorAction Stop
        Write-Host "FOUND: $v -> Status: $($resp.StatusCode)"
    } catch {
        # ignore
    }
}
