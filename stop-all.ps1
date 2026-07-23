Write-Host "=== Stopping all ERP backend processes ===" -ForegroundColor Cyan

$processes = Get-Process -Name "java" -ErrorAction SilentlyContinue
$stopped = 0

foreach ($proc in $processes) {
    try {
        $cmdLine = (Get-WmiObject Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
        if ($cmdLine -match "spring-boot|erp|eureka|identity|product|inventory|order|payment|sales|procurement|hrm|finance|gateway") {
            Write-Host "  Stopping PID $($proc.Id): $($proc.ProcessName)" -ForegroundColor Yellow
            $proc.Kill()
            $stopped++
        }
    } catch {
        # skip
    }
}

if ($stopped -eq 0) {
    Write-Host "  No matching Java processes found." -ForegroundColor Red
} else {
    Write-Host "  Stopped $stopped process(es)." -ForegroundColor Green
}
