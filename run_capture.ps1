$procs = Get-Process java -ErrorAction SilentlyContinue
if ($procs) { $procs | ForEach-Object { Stop-Process -Id $_.Id -Force }}
$p = Start-Process -FilePath 'java' -ArgumentList '-jar','target\springboot-payment-orchestrator-0.0.1-SNAPSHOT.jar' -NoNewWindow -RedirectStandardOutput 'mp11_app_full.log' -RedirectStandardError 'mp11_app_full.err' -PassThru
Start-Sleep -Seconds 18
if (Test-Path 'mp11_app_full.log') { Get-Content 'mp11_app_full.log' -TotalCount 200 | Out-File 'mp11_app_full_head.txt' }
Stop-Process -Id $p.Id -Force
