# Setup Keycloak for RBAC Management
$ErrorActionPreference = "Stop"

$keycloakUrl = "http://localhost:8180"
$masterRealm = "master"
$adminUser = "admin"
$adminPass = "admin"

$targetRealm = "rbac-management"
$clientId = "rbac-admin-frontend"
$frontendUrl = "http://localhost:5173"

Write-Host "1. Waiting for Keycloak service to become ready..." -ForegroundColor Cyan
$maxRetries = 30
$retries = 0
while ($retries -lt $maxRetries) {
    try {
        $test = Invoke-RestMethod -Uri "$keycloakUrl/realms/$masterRealm" -Method Get -TimeoutSec 3 -ErrorAction Stop
        Write-Host "Keycloak is online!" -ForegroundColor Green
        break
    } catch {
        $retries++
        Write-Host "Waiting for Keycloak to boot... ($retries/$maxRetries)" -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}
if ($retries -ge $maxRetries) {
    Write-Host "Keycloak failed to become ready in time!" -ForegroundColor Red
    exit 1
}

Write-Host "2. Getting Admin Token..." -ForegroundColor Cyan
$tokenResponse = Invoke-RestMethod -Uri "$keycloakUrl/realms/$masterRealm/protocol/openid-connect/token" `
    -Method Post `
    -Body @{
        client_id = "admin-cli"
        username = $adminUser
        password = $adminPass
        grant_type = "password"
    }
$adminToken = $tokenResponse.access_token
$headers = @{
    Authorization = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

Write-Host "2. Checking/Creating Realm '$targetRealm'..." -ForegroundColor Cyan
try {
    $realm = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm" -Headers $headers -Method Get
    Write-Host "Realm '$targetRealm' already exists." -ForegroundColor Yellow
} catch {
    $realmBody = @{
        realm = $targetRealm
        enabled = $true
        displayName = "RBAC Management Realm"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$keycloakUrl/admin/realms" -Headers $headers -Method Post -Body $realmBody
    Write-Host "Realm '$targetRealm' created successfully!" -ForegroundColor Green
}

Write-Host "3. Checking/Creating Client '$clientId'..." -ForegroundColor Cyan
$clients = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/clients" -Headers $headers -Method Get
$existingClient = $clients | Where-Object { $_.clientId -eq $clientId }

if (-not $existingClient) {
    $clientBody = @{
        clientId = $clientId
        name = "RBAC Admin Frontend"
        enabled = $true
        publicClient = $true
        standardFlowEnabled = $true
        directAccessGrantsEnabled = $true
        rootUrl = $frontendUrl
        baseUrl = "/"
        redirectUris = @("$frontendUrl/*", "$frontendUrl")
        webOrigins = @("*")
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/clients" -Headers $headers -Method Post -Body $clientBody
    Write-Host "Client '$clientId' created successfully!" -ForegroundColor Green
} else {
    Write-Host "Client '$clientId' already exists." -ForegroundColor Yellow
}

Write-Host "4. Creating Roles..." -ForegroundColor Cyan
$roles = @("SYSTEM_ADMIN", "PROJECT_ADMIN", "MAINTAINER", "DEVELOPER", "VIEWER")
$existingRoles = (Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/roles" -Headers $headers -Method Get).name

foreach ($r in $roles) {
    if ($r -notin $existingRoles) {
        $roleBody = @{ name = $r } | ConvertTo-Json
        Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/roles" -Headers $headers -Method Post -Body $roleBody
        Write-Host "Role '$r' created." -ForegroundColor Green
    } else {
        Write-Host "Role '$r' already exists." -ForegroundColor Yellow
    }
}

Write-Host "5. Creating Admin User..." -ForegroundColor Cyan
$users = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/users" -Headers $headers -Method Get
$existingUser = $users | Where-Object { $_.username -eq "admin" }

if (-not $existingUser) {
    $userBody = @{
        username = "admin"
        email = "admin@example.com"
        firstName = "System"
        lastName = "Admin"
        enabled = $true
        emailVerified = $true
        credentials = @(
            @{
                type = "password"
                value = "admin123"
                temporary = $false
            }
        )
    } | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/users" -Headers $headers -Method Post -Body $userBody
    $users = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/users" -Headers $headers -Method Get
    $existingUser = $users | Where-Object { $_.username -eq "admin" }
    Write-Host "User 'admin' created." -ForegroundColor Green
} else {
    Write-Host "User 'admin' already exists." -ForegroundColor Yellow
}

Write-Host "6. Assigning SYSTEM_ADMIN role to user 'admin'..." -ForegroundColor Cyan
$adminRole = Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/roles/SYSTEM_ADMIN" -Headers $headers -Method Get
$rolePayload = "[{""id"":""$($adminRole.id)"",""name"":""$($adminRole.name)""}]"
Invoke-RestMethod -Uri "$keycloakUrl/admin/realms/$targetRealm/users/$($existingUser.id)/role-mappings/realm" `
    -Headers $headers `
    -Method Post `
    -Body $rolePayload
Write-Host "Role 'SYSTEM_ADMIN' mapped to 'admin'!" -ForegroundColor Green

Write-Host "`n>>> KEYCLOAK SETUP COMPLETED! <<<" -ForegroundColor Green
Write-Host "Realm: $targetRealm"
Write-Host "Client: $clientId"
Write-Host "User: admin / admin123 (SYSTEM_ADMIN)"
Write-Host "Frontend URL: $frontendUrl"
