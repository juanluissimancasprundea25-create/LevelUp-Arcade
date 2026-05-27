$ErrorActionPreference = "Stop"

$certsDir = Join-Path $PSScriptRoot "..\certs"
New-Item -ItemType Directory -Force -Path $certsDir | Out-Null

$fullchain = Join-Path $certsDir "fullchain.pem"
$privkey   = Join-Path $certsDir "privkey.pem"

if (Test-Path $fullchain) {
    Write-Host "Ya existe $fullchain"
    Write-Host "Borralo a mano si quieres regenerarlo."
    exit 0
}

if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    $gitOpenssl = "C:\Program Files\Git\usr\bin"
    if (Test-Path "$gitOpenssl\openssl.exe") {
        $env:Path = "$gitOpenssl;$env:Path"
    } else {
        Write-Error "openssl no encontrado. Instala Git for Windows o anade openssl al PATH."
        exit 1
    }
}

openssl req -x509 -nodes -days 365 -newkey rsa:2048 `
    -keyout $privkey `
    -out    $fullchain `
    -subj "/C=ES/ST=Madrid/L=Madrid/O=LevelUp Arcade/CN=localhost" `
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

Write-Host ""
Write-Host "Certificado autofirmado generado en $certsDir" -ForegroundColor Green
Write-Host "Tu navegador avisara de inseguro: es normal en autofirmado."