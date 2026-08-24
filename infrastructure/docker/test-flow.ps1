# Exercises the core FoodLoop "food rescue" flow end-to-end against the
# live docker-compose stack: register donor -> create org -> list food ->
# publish -> search -> register receiver -> create org -> claim.
# Run from anywhere; requires the stack to already be up (docker compose ps).

$ErrorActionPreference = "Stop"
$gw = "http://localhost:8080"
$kc = "http://localhost:8081/realms/foodloop/protocol/openid-connect/token"
$tenantId = "00000000-0000-0000-0000-000000000001"
$suffix = Get-Random -Maximum 99999

function Get-Token($email, $password) {
    $resp = Invoke-RestMethod -Method Post -Uri $kc -ContentType "application/x-www-form-urlencoded" `
        -Body "grant_type=password&client_id=foodloop-web&username=$email&password=$password"
    return $resp.access_token
}

Write-Host "=== 1. Register donor ===" -ForegroundColor Cyan
$donorEmail = "donor$suffix@example.com"
$donorBody = @{ tenantId = $tenantId; email = $donorEmail; password = "changeme12345"; displayName = "Donor $suffix"; locale = "en" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$gw/api/v1/auth/register" -ContentType "application/json" -Body $donorBody | Format-List

$donorToken = Get-Token $donorEmail "changeme12345"

Write-Host "=== 2. Create donor org ===" -ForegroundColor Cyan
$orgBody = @{ name = "Test Restaurant $suffix"; type = "DONOR_RESTAURANT"; latitude = 12.9716; longitude = 77.5946 } | ConvertTo-Json
$org = Invoke-RestMethod -Method Post -Uri "$gw/api/v1/organizations" -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $donorToken" } -Body $orgBody
$org | Format-List
$donorOrgId = $org.id

Write-Host "=== 3. Create food listing ===" -ForegroundColor Cyan
$expiry = (Get-Date).ToUniversalTime().AddHours(6).ToString("yyyy-MM-ddTHH:mm:ssZ")
$pickupStart = (Get-Date).ToUniversalTime().AddHours(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$pickupEnd = (Get-Date).ToUniversalTime().AddHours(4).ToString("yyyy-MM-ddTHH:mm:ssZ")
$listingBody = @{
    donorOrgId = $donorOrgId; title = "20 Veg Meals"; description = "Surplus catering trays"
    foodCategory = "COOKED_MEAL"; quantityValue = 20; quantityUnit = "SERVINGS"; estimatedServings = 20
    expiryTime = $expiry; pickupStartTime = $pickupStart; pickupEndTime = $pickupEnd
    latitude = 12.9716; longitude = 77.5946
} | ConvertTo-Json
$listing = Invoke-RestMethod -Method Post -Uri "$gw/api/v1/food-listings" -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $donorToken" } -Body $listingBody
$listing | Format-List
$listingId = $listing.id

Write-Host "=== 4. Publish listing ===" -ForegroundColor Cyan
Invoke-RestMethod -Method Post -Uri "$gw/api/v1/food-listings/$listingId/publish" -Headers @{ Authorization = "Bearer $donorToken" } | Format-List

Write-Host "=== 5. Search nearby ===" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$gw/api/v1/food-listings?lat=12.9716&lng=77.5946&radiusKm=10" -Headers @{ Authorization = "Bearer $donorToken" } |
    Select-Object -ExpandProperty content | Format-Table title, status, quantityValue

Write-Host "=== 6. Register receiver ===" -ForegroundColor Cyan
$receiverEmail = "receiver$suffix@example.com"
$receiverBody = @{ tenantId = $tenantId; email = $receiverEmail; password = "changeme12345"; displayName = "Receiver $suffix"; locale = "en" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$gw/api/v1/auth/register" -ContentType "application/json" -Body $receiverBody | Format-List

$receiverToken = Get-Token $receiverEmail "changeme12345"

Write-Host "=== 7. Create receiver org ===" -ForegroundColor Cyan
$receiverOrgBody = @{ name = "Test NGO $suffix"; type = "NGO"; latitude = 12.9720; longitude = 77.5950 } | ConvertTo-Json
$receiverOrg = Invoke-RestMethod -Method Post -Uri "$gw/api/v1/organizations" -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $receiverToken" } -Body $receiverOrgBody
$receiverOrg | Format-List
$receiverOrgId = $receiverOrg.id

Write-Host "=== 8. Claim the listing ===" -ForegroundColor Cyan
$claimBody = @{ receiverOrgId = $receiverOrgId } | ConvertTo-Json
$claim = Invoke-RestMethod -Method Post -Uri "$gw/api/v1/food-listings/$listingId/claim" -ContentType "application/json" `
    -Headers @{ Authorization = "Bearer $receiverToken"; "Idempotency-Key" = [guid]::NewGuid().ToString() } -Body $claimBody
$claim | Format-List

Write-Host "DONE — full donor-to-receiver flow completed." -ForegroundColor Green
