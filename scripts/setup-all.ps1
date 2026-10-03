# Setup All: Bootstrap Keycloak & Kong in one go
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "   BOOTSTRAPPING KEYCLOAK SSO..." -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
& "$scriptDir\setup-keycloak.ps1"

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "   BOOTSTRAPPING KONG API GATEWAY..." -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
& "$scriptDir\setup-kong.ps1"

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "   ALL SETUP COMPLETED SUCCESSFULLY! 🎉" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "You can now open the Web Dashboard at: http://localhost:5173"
Write-Host "Log in with: admin / admin123"
