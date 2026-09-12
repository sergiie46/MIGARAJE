# GitHub Actions — Mi Garaje

## 1. Sube el proyecto
Sube **el contenido de esta carpeta** a la raíz de un repositorio GitHub. Debes ver `.github/workflows/android-build.yml` en la raíz.

En cada push a `main` o `master`, Actions ejecuta tests, lint y genera el APK Debug. Se descarga desde la sección **Artifacts** como `MiGaraje-debug-apk`.

## 2. Release para Google Play
Ve a `Settings > Secrets and variables > Actions` y crea:

- `GOOGLE_SERVICES_JSON_B64`
- `RELEASE_KEYSTORE_B64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
- `DEVELOPER_CONTACT` (obligatorio para el release; tu correo público de soporte)
- `FIREBASE_STORAGE_ENABLED` (`true` o `false`, opcional)

No necesitas crear secrets para AdMob: el proyecto ya incorpora para **Release**:
- App ID: `ca-app-pub-4959997187423595~3743834650`
- Banner: `ca-app-pub-4959997187423595/9627937913`

Debug siempre utiliza anuncios oficiales de prueba de Google.

### Convertir archivos a Base64 en PowerShell
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("google-services.json")) | Set-Clipboard
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Clipboard
```

Ejecuta `Actions > Build Mi Garaje Android > Run workflow` y marca **Compilar también APK/AAB Release firmado**.

Artifacts finales:
- `MiGaraje-release-apk`
- `MiGaraje-PlayStore-AAB`

El AAB es el archivo que se sube a Google Play Console.
