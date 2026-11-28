param(
    [string]$Version = "2.0.0",
    [switch]$SkipFrontendBuild
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RepoRoot

Write-Host "=== Building GymSync v$Version Windows Release ==="

if (-not $SkipFrontendBuild) {
    Write-Host "[1/6] Building React frontend..."
    npm --prefix (Join-Path $RepoRoot "frontend") run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed with exit code $LASTEXITCODE" }
} else {
    Write-Host "[1/6] Skipping npm build (-SkipFrontendBuild); using existing frontend/dist..."
}

$StageDir = Join-Path $RepoRoot "build-stage"
$ClassesDir = Join-Path $StageDir "classes"
$InputDir = Join-Path $StageDir "input"
$AppImageDir = Join-Path $StageDir "app-image"
$ReleaseDir = Join-Path $RepoRoot "release"

if (Test-Path $StageDir) { Remove-Item -Recurse -Force $StageDir }
if (Test-Path $ReleaseDir) { Remove-Item -Recurse -Force $ReleaseDir }

New-Item -ItemType Directory -Force -Path $ClassesDir, $InputDir, $AppImageDir, $ReleaseDir | Out-Null

Write-Host "[2/6] Compiling Java backend..."
javac -Xlint:deprecation -d $ClassesDir -cp "lib/*" GymsyncServer.java gymsyncapp.java
if ($LASTEXITCODE -ne 0) { throw "Java compilation failed with exit code $LASTEXITCODE" }

Write-Host "[3/6] Embedding frontend/dist inside JAR resources..."
$DistSrc = Join-Path $RepoRoot "frontend\dist"
if (-not (Test-Path (Join-Path $DistSrc "index.html"))) {
    throw "frontend/dist/index.html missing. Run without -SkipFrontendBuild."
}
$DistDest = Join-Path $ClassesDir "dist"
New-Item -ItemType Directory -Force -Path $DistDest | Out-Null
Copy-Item -Path (Join-Path $DistSrc "*") -Destination $DistDest -Recurse -Force

Write-Host "[4/6] Creating runnable GymSync.jar..."
Copy-Item -Path (Join-Path $RepoRoot "lib\*.jar") -Destination $InputDir -Force
$JarPath = Join-Path $InputDir "GymSync.jar"
jar --create --file $JarPath --main-class GymsyncServer -C $ClassesDir .
if ($LASTEXITCODE -ne 0) { throw "jar creation failed with exit code $LASTEXITCODE" }

$ReleaseJar = Join-Path $ReleaseDir "GymSync-v$Version-Runnable.jar"
Copy-Item -Path $JarPath -Destination $ReleaseJar -Force

Write-Host "[5/6] Packaging portable Windows app (GymSync.exe + bundled JRE) via jpackage..."
jpackage --type app-image `
    --name GymSync `
    --app-version $Version `
    --vendor "DIT 2-3 Group 3" `
    --description "GymSync Front Desk & Workout Operations Console" `
    --input $InputDir `
    --main-jar GymSync.jar `
    --main-class GymsyncServer `
    --add-modules java.base,java.desktop,java.logging,java.naming,java.sql,java.xml,jdk.httpserver,jdk.crypto.ec `
    --dest $AppImageDir
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

$BundledDir = Join-Path $AppImageDir "GymSync"
$SchemaFile = Join-Path $RepoRoot "gymsync_database_schema_and_seed.sql"
if (Test-Path $SchemaFile) {
    Copy-Item -Path $SchemaFile -Destination $BundledDir -Force
}

$ReadmeContent = @"
GymSync v$Version — Front Desk & Workout Operations Console
=========================================================

Quick Start (Windows):
1. Extract this entire folder anywhere on your PC.
2. Double-click GymSync.exe (Java is already bundled — no install needed).
3. Wait 1-2 seconds for the desktop window to appear.

Demo Operator Credentials:
- Admin Desk: admin / admin123
- Staff Desk: staff / staff123
(Member accounts are roster-only and blocked from operator console login.)

Optional MySQL Database Setup:
- Import gymsync_database_schema_and_seed.sql into MySQL.
- Without a MySQL JDBC driver configured, GymSync runs automatically in instant In-Memory Demo mode.
"@
Set-Content -Path (Join-Path $BundledDir "README.txt") -Value $ReadmeContent -Encoding UTF8

Write-Host "[6/6] Compressing release archive..."
$ReleaseZip = Join-Path $ReleaseDir "GymSync-v$Version-Windows-x64.zip"
Compress-Archive -Path $BundledDir -DestinationPath $ReleaseZip -Force

Write-Host ""
Write-Host "Release artifacts built cleanly:"
Get-ChildItem $ReleaseDir | Select-Object Name, @{Name='SizeMB';Expression={[math]::Round($_.Length / 1MB, 2)}} | Format-Table -AutoSize
