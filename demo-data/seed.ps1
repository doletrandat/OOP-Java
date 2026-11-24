# Nạp bộ dữ liệu demo (vị trí tuyển dụng + CV) vào server CV Screener đang chạy.
#
#   pwsh demo-data/seed.ps1
#   pwsh demo-data/seed.ps1 -BaseUrl http://localhost:8080 -Password admin123
#
# Mỗi vị trí trong jobs.json được tạo qua API, sau đó toàn bộ PDF trong
# resumes/<folder> được tải lên. Không gửi họ tên: server tự đọc từ CV.
# Vị trí đã có (trùng tên) sẽ được bỏ qua, nên chạy lại nhiều lần vẫn an toàn.

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Password
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

if (-not $Password) {
    $Password = $env:ADMIN_PASSWORD
}
if (-not $Password) {
    $config = Join-Path $root "..\config.properties"
    if (Test-Path $config) {
        $line = Get-Content $config | Where-Object { $_ -match '^\s*ADMIN_PASSWORD\s*=' } | Select-Object -First 1
        if ($line) { $Password = ($line -split '=', 2)[1].Trim() }
    }
}
if (-not $Password) { $Password = "admin123" }

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
    -ContentType "application/json" -Body (@{ password = $Password } | ConvertTo-Json)
$headers = @{ "X-Auth-Token" = $login.token }

$existing = @(Invoke-RestMethod -Uri "$BaseUrl/api/admin/jobs" -Headers $headers | ForEach-Object { $_.title })
$jobs = Get-Content (Join-Path $root "jobs.json") -Raw -Encoding utf8 | ConvertFrom-Json

foreach ($job in $jobs) {
    if ($existing -contains $job.title) {
        Write-Host "Bo qua (da co): $($job.title)" -ForegroundColor DarkGray
        continue
    }
    $body = $job | Select-Object title, department, description, skills, criteria | ConvertTo-Json -Depth 5
    $created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/jobs" -Headers $headers `
        -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
    Write-Host "`n$($job.title)" -ForegroundColor Cyan

    $pdfs = Get-ChildItem (Join-Path $root "resumes\$($job.folder)") -Filter *.pdf | Sort-Object Name
    foreach ($pdf in $pdfs) {
        try {
            $app = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/jobs/$($created.id)/cv" `
                -Headers $headers -Form @{ file = $pdf }
            $score = if ($app.report) { [math]::Round($app.report.finalScore) } else { "-" }
            Write-Host ("  {0,-28} {1,4} diem   ({2})" -f $app.candidateName, $score, $pdf.Name)
        }
        catch {
            Write-Host "  Loi khi tai $($pdf.Name): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}
Write-Host "`nXong. Mo $BaseUrl/admin de xem." -ForegroundColor Green
