<#
  Starts InfraSight on Windows.
  Run from PowerShell:   .\run-infrasight.ps1
  If scripts are blocked: powershell -ExecutionPolicy Bypass -File .\run-infrasight.ps1
  Nothing is written to disk: the MySQL password only lives in this process.
#>
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

function Fail([string]$message) {
    Write-Host ""
    Write-Host "ERROR: $message" -ForegroundColor Red
    exit 1
}

# 1. Java 21 -----------------------------------------------------------------
$javaCmd = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $javaCmd = Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    $found = Get-Command java -ErrorAction SilentlyContinue
    if ($found) { $javaCmd = $found.Source }
}
if (-not $javaCmd) {
    Fail "Java was not found. Install JDK 21 (IntelliJ can download one under File > Project Structure > SDKs) and set JAVA_HOME, or add java to PATH."
}
$versionLine = (& $javaCmd -version 2>&1 | Select-Object -First 1).ToString()
if ($versionLine -match '"(\d+)') { $javaMajor = [int]$Matches[1] } else { $javaMajor = 0 }
if ($javaMajor -lt 21) {
    Fail "Java 21 or newer is required, but found: $versionLine. Point JAVA_HOME to a JDK 21 installation."
}
Write-Host "Java OK: $versionLine"

# 2. Database settings -------------------------------------------------------
if ([string]::IsNullOrWhiteSpace($env:DB_URL)) {
    $env:DB_URL = 'jdbc:mysql://localhost:3306/infrasight_db?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true'
    Write-Host "DB_URL not set. Using default: jdbc:mysql://localhost:3306/infrasight_db"
}
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    $entered = Read-Host "MySQL username [root]"
    if ([string]::IsNullOrWhiteSpace($entered)) { $entered = 'root' }
    $env:DB_USERNAME = $entered
}
if ($null -eq $env:DB_PASSWORD) {
    $secure = Read-Host "MySQL password for '$($env:DB_USERNAME)' (hidden)" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    Fail "DB_USERNAME is missing. Set it or enter it when asked."
}

# 3. Maven -------------------------------------------------------------------
$mvn = $null
$onPath = Get-Command mvn -ErrorAction SilentlyContinue
if ($onPath) {
    $mvn = $onPath.Source
} elseif (Test-Path (Join-Path $PSScriptRoot 'mvnw.cmd')) {
    $mvn = Join-Path $PSScriptRoot 'mvnw.cmd'
    Write-Host "Maven is not on PATH. Using the bundled Maven wrapper (downloads Maven on first run)."
} else {
    Fail "Maven was not found and mvnw.cmd is missing. Install Maven or open the project in IntelliJ and run InfraSightApplication."
}

# 4. Start -------------------------------------------------------------------
Write-Host ""
Write-Host "Starting InfraSight. The first start downloads dependencies and can take a few minutes."
Write-Host "When you see 'Started InfraSightApplication', open http://localhost:8080"
Write-Host "Press Ctrl+C to stop."
Write-Host ""
& $mvn spring-boot:run
if ($LASTEXITCODE -ne 0) {
    Fail "The application stopped with exit code $LASTEXITCODE. Read the messages above. Common causes: MySQL is not running, wrong username or password, or port 8080 is already in use."
}
