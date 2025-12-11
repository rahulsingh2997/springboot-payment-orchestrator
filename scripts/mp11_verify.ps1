# MP11 verification script
$root = (Get-Location).Path
$jar = Join-Path $root "target\springboot-payment-orchestrator-0.0.1-SNAPSHOT.jar"
$logs = Join-Path $root "logs"
if (!(Test-Path $logs)) { New-Item -ItemType Directory -Path $logs | Out-Null }

function Kill-AppInstances() {
    $procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine -like "*springboot-payment-orchestrator-0.0.1-SNAPSHOT.jar*" }
    foreach ($p in $procs) {
        try { Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue } catch {}
    }
}

Write-Output "[mp11] Cleaning previous app instances..."
Kill-AppInstances

Write-Output "[mp11] Starting application jar..."
$javaArgs = @('-Dspring.profiles.active=local', '-jar', "$jar")
$startInfo = Start-Process -FilePath "java" -ArgumentList $javaArgs -WorkingDirectory $root -PassThru
Start-Sleep -Seconds 1
try { $startInfo.Id | Out-File (Join-Path $logs 'mp11_started_pid.txt') -Encoding utf8 } catch {}

# wait for actuator/health
$maxWait = 60
$ok = $false
for ($i=0; $i -lt $maxWait; $i++) {
    try {
        $r = Invoke-RestMethod -Uri http://localhost:8080/actuator/health -UseBasicParsing -TimeoutSec 2
        if ($r) { $ok = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $ok) {
    Write-Output "[mp11] Application did not become ready within $maxWait seconds."
}
else { Write-Output "[mp11] Application ready." }

# fetch dev token
$token = $null
try {
    $req = @{ username = 'mp11'; roles = @('ROLE_USER') } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/dev/token -Body $req -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $token = $resp.token
    $token | Out-File -FilePath (Join-Path $logs 'mp11_token.txt') -Encoding UTF8
} catch {
    Write-Output "[mp11] Failed to retrieve token: $_"
}

$headers = @{}
if ($token) { $headers = @{ Authorization = "Bearer $token" } }

# create two orders: one for capture+refund, one for authorize+void
$orderCaptureId = $null
$orderVoidId = $null
try {
    $body1 = @{ externalOrderId = 'mp11-order-1'; customerId = 'cust-1'; amountCents = 1000; currency = 'USD' } | ConvertTo-Json
    $resp1 = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/orders -Headers $headers -Body $body1 -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $resp1 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_create_order1_resp.txt') -Encoding utf8
    if ($resp1.id) { $orderCaptureId = $resp1.id; $orderCaptureId | Out-File (Join-Path $logs 'mp11_order_capture_id.txt') -Encoding utf8 }
} catch { Write-Output "[mp11] create order1 failed: $_" }

try {
    $body2 = @{ externalOrderId = 'mp11-order-2'; customerId = 'cust-1'; amountCents = 500; currency = 'USD' } | ConvertTo-Json
    $resp2 = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/orders -Headers $headers -Body $body2 -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $resp2 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_create_order2_resp.txt') -Encoding utf8
    if ($resp2.id) { $orderVoidId = $resp2.id; $orderVoidId | Out-File (Join-Path $logs 'mp11_order_void_id.txt') -Encoding utf8 }
} catch { Write-Output "[mp11] create order2 failed: $_" }

# authorize & capture orderCaptureId (for refund)
if ($orderCaptureId) {
    try {
        $auth1 = Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/authorize" -f $orderCaptureId) -Headers $headers -UseBasicParsing -TimeoutSec 10
        $auth1 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_authorize1.txt') -Encoding utf8
    } catch { Write-Output "[mp11] authorize1 failed: $_" }
    Start-Sleep -Seconds 1
    try {
        $cap1 = Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/capture" -f $orderCaptureId) -Headers $headers -UseBasicParsing -TimeoutSec 10
        $cap1 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_capture1.txt') -Encoding utf8
    } catch { Write-Output "[mp11] capture1 failed: $_" }
    # refund orderCaptureId with JSON body
    Start-Sleep -Seconds 1
    try {
        $refundBody = @{ amountCents = 1000 } | ConvertTo-Json
        $refund1 = Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/refund" -f $orderCaptureId) -Headers $headers -Body $refundBody -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
        $refund1 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_refund1.txt') -Encoding utf8
    } catch { Write-Output "[mp11] refund1 failed: $_" }
}

# authorize then void orderVoidId (do NOT capture)
if ($orderVoidId) {
    try {
        $auth2 = Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/authorize" -f $orderVoidId) -Headers $headers -UseBasicParsing -TimeoutSec 10
        $auth2 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_authorize2.txt') -Encoding utf8
    } catch { Write-Output "[mp11] authorize2 failed: $_" }
    Start-Sleep -Seconds 1
    try {
        $void2 = Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/orders/{0}/void" -f $orderVoidId) -Headers $headers -UseBasicParsing -TimeoutSec 10
        $void2 | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_void2.txt') -Encoding utf8
    } catch { Write-Output "[mp11] void2 failed: $_" }
}

# create subscription
$subId = $null
try {
    $sub = @{ id = (New-Guid).Guid; customerId = 'cust-1'; planId = 'plan-monthly'; amountCents = 500; currency = 'USD'; intervalDays = 30 } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/subscriptions -Headers $headers -Body $sub -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $resp | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_create_subscription_resp.txt') -Encoding utf8
    if ($resp.id) { $subId = $resp.id; $subId | Out-File (Join-Path $logs 'mp11_subscription_id.txt') -Encoding utf8 }
} catch { Write-Output "[mp11] create subscription failed: $_" }

# renew subscription
if ($subId) {
    try {
        $ren = Invoke-RestMethod -Method Post -Uri ("http://localhost:8080/api/v1/subscriptions/{0}/renew" -f $subId) -Headers $headers -UseBasicParsing -TimeoutSec 10
        $ren | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_renew.txt') -Encoding utf8
    } catch { Write-Output "[mp11] renew failed: $_" }
}

# send webhook with retries and extra debug logging
$attempts = 5
$payload = '{"event":"test","id":"mp11-webhook-1"}'
$signatureKey = "FD5E029A9561CF54CCB14075AA22DC5257591D0BA9C01C3BE57E993E42833C3B37165E089EBBDA5576FC63A9FD8D6FB25178CF18BCC9434D3EF04C08BF890BA0"
$keyBytes = for ($i=0; $i -lt $signatureKey.Length; $i+=2) { [Convert]::ToByte($signatureKey.Substring($i,2),16) }
$keyBytes = [byte[]]$keyBytes
$hmac = New-Object System.Security.Cryptography.HMACSHA512
$hmac.Key = $keyBytes
$signatureBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($payload))
$sigHex = ($signatureBytes | ForEach-Object { $_.ToString('x2') }) -join ''
$headerSig = "SHA512=$sigHex"

$attemptLog = Join-Path $logs 'mp11_webhook_attempts.txt'
"Webhook attempts starting at $(Get-Date -Format o)" | Out-File $attemptLog -Encoding utf8

$success = $false
for ($a=1; $a -le $attempts; $a++) {
    $hdrs = @{ 'X-ANET-Signature' = $headerSig; 'X-Source' = 'mp11'; 'X-Correlation-ID' = (New-Guid).Guid }
    $debug = @{ payload = $payload; header = $headerSig; correlationId = $hdrs.'X-Correlation-ID'; attempt = $a } | ConvertTo-Json
    $debug | Out-File (Join-Path $logs ("mp11_webhook_request_debug_$a.txt")) -Encoding utf8
    try {
        $resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/webhooks -Headers $hdrs -Body $payload -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
        $resp | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_webhook_resp.txt') -Encoding utf8
        "Attempt ${a}: success" | Out-File $attemptLog -Append -Encoding utf8
        $success = $true
        break
    } catch {
        $err = $_ | Out-String
        "Attempt ${a}: failed - $err" | Out-File $attemptLog -Append -Encoding utf8
        Start-Sleep -Seconds 2
    }
}
if (-not $success) {
    Write-Output "[mp11] webhook failed after $attempts attempts (details in $attemptLog)"
}

Start-Sleep -Seconds 6

# scrape prometheus
try {
    $prom = Invoke-WebRequest -Uri http://localhost:8080/actuator/prometheus -UseBasicParsing -TimeoutSec 10
    $prom.Content | Out-File (Join-Path $logs 'mp11_prometheus.txt') -Encoding utf8
} catch { Write-Output "[mp11] prometheus scrape failed: $_" }

# parse metrics
$metricsToCheck = @(
    'payment_authorized_total',
    'payment_captured_total',
    'payment_voided_total',
    'payment_refunded_total',
    'subscription_created_total',
    'subscription_renewed_total',
    'webhook_received_total',
    'webhook_processed_total'
)
$promText = ''
if (Test-Path (Join-Path $logs 'mp11_prometheus.txt')) { $promText = Get-Content (Join-Path $logs 'mp11_prometheus.txt') -Raw }
$parsed = @{}
foreach ($m in $metricsToCheck) {
    $found = $null
    if ($promText -ne '') {
        $pattern = "^" + [regex]::Escape($m) + "\s+(\S+)"  # metric line begins with metric name
        $matches = [regex]::Matches($promText, $pattern, 'Multiline')
        if ($matches.Count -gt 0) { $found = $matches[$matches.Count-1].Groups[1].Value }
    }
    if ($found) { $parsed[$m] = $found } else { $parsed[$m] = 'MISSING' }
}
$parsed | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_prometheus_parsed.json') -Encoding utf8

# determine pass/fail: all metrics numeric and >=1
$pass = $true
foreach ($k in $parsed.Keys) {
    $v = $parsed[$k]
    if ($v -eq 'MISSING') { $pass = $false; break }
    $num = 0
    try { $num = [double]$v } catch { $num = 0 }
    if ($num -lt 1) { $pass = $false; break }
}
$result = @{ pass = $pass; parsed = $parsed; timestamp = (Get-Date).ToString('o') }
$result | ConvertTo-Json | Out-File (Join-Path $logs 'mp11_final_result.json') -Encoding utf8

Write-Output "[mp11] Verification complete. Pass=$pass. Results written to logs/mp11_final_result.json"
