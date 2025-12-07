# Supervisor script: keep restarting the Spring Boot jar if it exits
# Logs: each run app_stdout.log / app_stderr.log are appended

$jar = "target\springboot-payment-orchestrator-0.0.1-SNAPSHOT.jar"
$args = "--spring.profiles.active=local --payment.auto-purchase=false"

while ($true) {
    $ts = Get-Date -Format o
    Write-Output "[$ts] Starting app: java -jar $jar $args"
    # Run the jar in the foreground so we can capture its exit code; append outputs
    & java -jar $jar $args >> app_stdout.log 2>> app_stderr.log
    $code = $LASTEXITCODE
    $ts = Get-Date -Format o
    Write-Output "[$ts] App exited with code $code; sleeping 2s before restart"
    "$ts - App exited with code $code" | Out-File -FilePath run-forever.log -Append
    Start-Sleep -Seconds 2
}
