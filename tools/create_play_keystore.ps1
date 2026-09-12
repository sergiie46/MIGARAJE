$ErrorActionPreference = 'Stop'
$Out = Join-Path $PSScriptRoot '..\migaraje-upload.jks'
if (Test-Path $Out) { throw "Ya existe $Out. No se sobrescribe una clave de publicación." }
Write-Host "Creando upload key de Google Play en: $Out"
keytool -genkeypair -v -keystore $Out -alias migaraje -keyalg RSA -keysize 2048 -validity 10000
Write-Host "\nCreada. GUARDA este archivo y las contraseñas en un lugar seguro."
Write-Host "Para copiarlo como Base64 al portapapeles:"
Write-Host "[Convert]::ToBase64String([IO.File]::ReadAllBytes('$Out')) | Set-Clipboard"
