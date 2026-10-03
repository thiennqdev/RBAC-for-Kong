# Setup Kong Service and Route for RBAC Backend
$ErrorActionPreference = "Stop"

$kongAdminUrl = if ($env:KONG_ADMIN_URL) { $env:KONG_ADMIN_URL } else { "http://localhost:8001" }
$backendUrl = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { "http://backend:8080" }
$serviceName = "rbac-backend"
$routeName = "rbac-backend-route"
$routePath = "/api/rbac"

Write-Host "1. Waiting for Kong Admin API to become ready..." -ForegroundColor Cyan
$maxRetries = 30
$retries = 0
while ($retries -lt $maxRetries) {
    try {
        $status = Invoke-RestMethod -Uri "$kongAdminUrl/status" -Method Get -TimeoutSec 3 -ErrorAction Stop
        Write-Host "Kong Admin API is online!" -ForegroundColor Green
        break
    } catch {
        $retries++
        Write-Host "Waiting for Kong migrations and service to boot... ($retries/$maxRetries)" -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}
if ($retries -ge $maxRetries) {
    Write-Host "Kong Admin API failed to become ready in time!" -ForegroundColor Red
    exit 1
}

Write-Host "2. Configuring Service '$serviceName'..." -ForegroundColor Cyan
$services = (Invoke-RestMethod -Uri "$kongAdminUrl/services" -Method Get).data
$existingService = $services | Where-Object { $_.name -eq $serviceName }

if (-not $existingService) {
    $serviceBody = @{
        name = $serviceName
        url = $backendUrl
    }
    $createdService = Invoke-RestMethod -Uri "$kongAdminUrl/services" -Method Post -Body $serviceBody
    $serviceId = $createdService.id
    Write-Host "Service '$serviceName' created (ID: $serviceId) pointing to $backendUrl" -ForegroundColor Green
} else {
    $serviceId = $existingService.id
    Write-Host "Service '$serviceName' already exists (ID: $serviceId)." -ForegroundColor Yellow
}

Write-Host "3. Configuring Route '$routeName'..." -ForegroundColor Cyan
$routes = (Invoke-RestMethod -Uri "$kongAdminUrl/services/$serviceId/routes" -Method Get).data
$existingRoute = $routes | Where-Object { $_.name -eq $routeName }

if (-not $existingRoute) {
    $routeBody = @{
        name = $routeName
        "paths[]" = $routePath
        strip_path = "true"
    }
    $createdRoute = Invoke-RestMethod -Uri "$kongAdminUrl/services/$serviceId/routes" -Method Post -Body $routeBody
    Write-Host "Route '$routeName' created with path '$routePath' (strip_path: true)" -ForegroundColor Green
} else {
    Write-Host "Route '$routeName' already exists." -ForegroundColor Yellow
}

Write-Host "`n>>> KONG SETUP COMPLETED! <<<" -ForegroundColor Green
Write-Host "Requests to http://localhost:8000$routePath/<endpoint> will forward to $backendUrl/<endpoint>"
