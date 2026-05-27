# ============================================================
# LevelUp Arcade - Restaurar un backup (Windows)
# Uso:  .\restaurar.ps1 <fichero.sql.gz>
# ============================================================
param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupFile)) {
    Write-Error "No existe el fichero $BackupFile"
    exit 1
}

# Leer .env
$envFile = ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
            Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
        }
    }
}

$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "levelup" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "levelup_user" }

Write-Host "ATENCION: esto va a SOBRESCRIBIR la BD '$dbName' del contenedor 'levelup-postgres'." -ForegroundColor Yellow
Write-Host "Fichero a restaurar: $BackupFile"
$confirm = Read-Host "Continuar? (escribe SI)"

if ($confirm -ne "SI") {
    Write-Host "Cancelado."
    exit 0
}

Write-Host "[*] Restaurando $BackupFile..." -ForegroundColor Cyan

# Descomprimir y meter por stdin al psql del contenedor
# En Windows necesitamos 7zip o gzip. Probamos con gzip de Git for Windows.
$gzipPath = "C:\Program Files\Git\usr\bin\gzip.exe"
if (-not (Test-Path $gzipPath)) {
    Write-Error "Necesito gzip.exe. Instala Git for Windows o anade gzip al PATH."
    exit 1
}

& $gzipPath -dc $BackupFile | docker exec -i levelup-postgres psql -U $dbUser -d $dbName

Write-Host "[OK] BD restaurada." -ForegroundColor Green
Write-Host "Recuerda reiniciar la app:"
Write-Host "  docker compose restart app"