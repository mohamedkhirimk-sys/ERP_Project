Write-Host "=== Starting ERP System ===" -ForegroundColor Cyan

$services = @(
    # Tier 1: Infrastructure (must start first)
    @{ Name = "eureka-server"; Port = 8761 },
    
    # Tier 2: Core services (no inter-dependencies)
    @{ Name = "identity-service"; Port = 8086 },
    @{ Name = "product-service"; Port = 8083 },
    @{ Name = "inventory-service"; Port = 8091 },
    
    # Tier 3: Business services (depend on core)
    @{ Name = "order-service"; Port = 8093 },
    @{ Name = "payment-service"; Port = 8094 },
    @{ Name = "sales-service"; Port = 8095 },
    @{ Name = "procurement-service"; Port = 8096 },
    
    # Tier 4: HR & Finance
    @{ Name = "hrm-service"; Port = 8097 },
    @{ Name = "finance-service"; Port = 8098 },

    # Tier 5: Reporting (aggregates from all services)
    @{ Name = "reporting-service"; Port = 8099 },

    # Tier 6: Gateway (last, routes to all)
    @{ Name = "gateway-service"; Port = 8092 }
)

$root = $PSScriptRoot

foreach ($svc in $services) {
    $name = $svc.Name
    $port = $svc.Port
    $path = Join-Path (Join-Path $root "backend") $name
    
    Write-Host "Starting $name on port $port..." -ForegroundColor Yellow
    
    $job = Start-Job -ScriptBlock {
        param($p, $n)
        Set-Location $p
        Start-Process -WindowStyle Hidden -FilePath "cmd.exe" -ArgumentList "/c mvn spring-boot:run"
    } -ArgumentList $path, $name
    
    Write-Host "  $name started (PID: $($job.Id))" -ForegroundColor Green
    
    Start-Sleep -Seconds 10
}

Write-Host "=== All services starting ===" -ForegroundColor Cyan
Write-Host "Check http://localhost:8761 for Eureka Dashboard" -ForegroundColor Cyan
