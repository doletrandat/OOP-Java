# Build (nếu cần) rồi chạy CV Recruiter bằng Maven wrapper có sẵn trong dự án.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$jar = Join-Path $PSScriptRoot "target\cv-recruiter.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Chua co $jar - dang build..." -ForegroundColor Cyan
    & "$PSScriptRoot\mvnw.cmd" -q -DskipTests package
}
Write-Host "Khoi dong CV Recruiter..." -ForegroundColor Green
java -jar $jar
