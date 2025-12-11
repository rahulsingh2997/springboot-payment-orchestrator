# MP12 docker verification script - assumes docker-compose stack is running and app reachable at http://localhost:8080
$root = (Get-Location).Path
$logs = Join-Path $root "logs"
if (!(Test-Path $logs)) { New-Item -ItemType Directory -Path $logs | Out-Null }

# wait for actuator/health
$maxWait = 120
$ok = $false
for ($i=0; $i -lt $maxWait; $i++) {
    try {
        $r = Invoke-RestMethod -Uri http://localhost:8080/actuator/health -UseBasicParsing -TimeoutSec 5
        $r | ConvertTo-Json | Out-File (Join-Path $logs 'mp12_health_after_docker.json') -Encoding utf8
        if ($r.status -eq 'UP') { $ok = $true; break }
    } catch { Start-Sleep -Seconds 2 }
    Start-Sleep -Seconds 1
}
if (-not $ok) { Write-Output "[mp12] Application did not become ready within $maxWait seconds."; exit 2 }
Write-Output "[mp12] Application ready. Proceeding with exercise flows."

# fetch dev token
$token = $null
try {
    $req = @{ username = 'mp12'; roles = @('ROLE_USER') } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/dev/token -Body $req -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $token = $resp.token
    $token | Out-File -FilePath (Join-Path $logs 'mp12_token.txt') -Encoding UTF8
} catch {
    Write-Output "[mp12] Failed to retrieve token: $_"
}
$headers = @{}
if ($token) { $headers = @{ Authorization = "Bearer $token" } }

# create orders and perform ops
function TryCall($scriptblock, $outfile) {
    try { $res = & $scriptblock; $res | ConvertTo-Json | Out-File $outfile -Encoding utf8; return $res } catch { "ERROR: $_" | Out-File $outfile -Encoding utf8; return $null }
}

$oc = TryCall { Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/orders -Headers $headers -Body (@{ externalOrderId='mp12-order-1'; customerId='cust-1'; amountCents=1000; currency='USD' } | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_create_order1_resp.txt')
if ($oc -and $oc.id) { $orderCaptureId = $oc.id } else { $orderCaptureId = $null }

$ov = TryCall { Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/orders -Headers $headers -Body (@{ externalOrderId='mp12-order-2'; customerId='cust-1'; amountCents=500; currency='USD' } | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_create_order2_resp.txt')
if ($ov -and $ov.id) { $orderVoidId = $ov.id } else { $orderVoidId = $null }

if ($orderCaptureId) {
    TryCall { Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/authorize" -f $orderCaptureId) -Headers $headers -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_authorize1.txt')
    Start-Sleep -Seconds 1
    TryCall { Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/capture" -f $orderCaptureId) -Headers $headers -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_capture1.txt')
    Start-Sleep -Seconds 1
    TryCall { Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/refund" -f $orderCaptureId) -Headers $headers -Body (@{ amountCents = 1000 } | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_refund1.txt')
}

if ($orderVoidId) {
    TryCall { Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/authorize" -f $orderVoidId) -Headers $headers -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_authorize2.txt')
    Start-Sleep -Seconds 1
    TryCall { Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/void" -f $orderVoidId) -Headers $headers -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_void2.txt')
}

# subscription
$sub = TryCall { Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/subscriptions -Headers $headers -Body (@{ id = (New-Guid).Guid; customerId='cust-1'; planId='plan-monthly'; amountCents=500; currency='USD'; intervalDays=30 } | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_create_subscription_resp.txt')
if ($sub -and $sub.id) { TryCall { Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/subscriptions/{0}/renew" -f $sub.id) -Headers $headers -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_renew.txt') }

# send webhook (use same HMAC key as previous script)
$payload = '{"event":"mp12-test","id":"mp12-webhook-1"}'
$signatureKey = "FD5E029A9561CF54CCB14075AA22DC5257591D0BA9C01C3BE57E993E42833C3B37165E089EBBDA5576FC63A9FD8D6FB25178CF18BCC9434D3EF04C08BF890BA0"
$keyBytes = for ($i=0; $i -lt $signatureKey.Length; $i+=2) { [Convert]::ToByte($signatureKey.Substring($i,2),16) }
$keyBytes = [byte[]]$keyBytes
$hmac = New-Object System.Security.Cryptography.HMACSHA512
$hmac.Key = $keyBytes
$signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($payload))
$sigHex = ($signatureBytes | ForEach-Object { $_.ToString('x2') }) -join ''
$headerSig = "SHA512=$sigHex"

$hdrs = @{ 'X-ANET-Signature' = $headerSig; 'X-Source' = 'mp12'; 'X-Correlation-ID' = (New-Guid).Guid }
TryCall { Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/webhooks -Headers $hdrs -Body $payload -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10 } (Join-Path $logs 'mp12_webhook_resp.txt')

# scrape prometheus and parse metrics (same metrics set)
try { $prom = Invoke-WebRequest -Uri http://localhost:8080/actuator/prometheus -UseBasicParsing -TimeoutSec 10; $prom.Content | Out-File (Join-Path $logs 'mp12_prometheus.txt') -Encoding utf8 } catch { Write-Output "[mp12] prometheus scrape failed: $_" }

$metricsToCheck = @('payment_authorized_total','payment_captured_total','payment_voided_total','payment_refunded_total','subscription_created_total','subscription_renewed_total','webhook_received_total','webhook_processed_total')
$promText = ''
if (Test-Path (Join-Path $logs 'mp12_prometheus.txt')) { $promText = Get-Content (Join-Path $logs 'mp12_prometheus.txt') -Raw }
$parsed = @{}
foreach ($m in $metricsToCheck) {
    $found = $null
    if ($promText -ne '') {
        $pattern = "^" + [regex]::Escape($m) + "\s+(\S+)"
        $matches = [regex]::Matches($promText, $pattern, 'Multiline')
        if ($matches.Count -gt 0) { $found = $matches[$matches.Count-1].Groups[1].Value }
    }
    if ($found) { $parsed[$m] = $found } else { $parsed[$m] = 'MISSING' }
}
$parsed | ConvertTo-Json | Out-File (Join-Path $logs 'mp12_prometheus_parsed.json') -Encoding utf8

# final verdict
$pass = $true
foreach ($k in $parsed.Keys) {
    $v = $parsed[$k]
    if ($v -eq 'MISSING') { $pass = $false; break }
    $num = 0
    try { $num = [double]$v } catch { $num = 0 }
    if ($num -lt 1) { $pass = $false; break }
}
$result = @{ mp12_pass = $pass; parsed = $parsed; timestamp = (Get-Date).ToString('o') }
$result | ConvertTo-Json | Out-File (Join-Path $logs 'mp12_final_result.json') -Encoding utf8
Write-Output "[mp12] Verification complete. Pass=$pass. Results written to logs/mp12_final_result.json"
