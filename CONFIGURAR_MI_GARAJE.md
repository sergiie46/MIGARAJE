# Mi Garaje — configuración final

## Identidad definitiva
- Package / Application ID: `com.noxforgestudios.mygarage`
- Desarrollador: NoxForge Studios
- Producto PRO: `migaraje_pro`
- AdMob App ID: `ca-app-pub-4959997187423595~3743834650`
- Banner: `ca-app-pub-4959997187423595/9627937913`

Los anuncios reales solo se usan en **Release**. Debug usa las unidades oficiales de prueba de Google.

## Firebase
1. Crea/abre tu proyecto Firebase.
2. Registra una app Android con package exacto `com.noxforgestudios.mygarage`.
3. Activa Authentication > Google.
4. Activa Cloud Firestore.
5. Añade SHA-1/SHA-256 de tu clave de subida/Play App Signing.
6. Descarga `google-services.json`.
7. Para GitHub Actions, conviértelo a Base64 y guárdalo en el secret `GOOGLE_SERVICES_JSON_B64`.
8. Publica `firestore.rules` e `firestore.indexes.json`.
9. Activa App Check con Play Integrity para producción.

## Google Play
Crea el producto de compra única `migaraje_pro`. El AAB release se genera firmado desde GitHub Actions cuando configures los secretos de firma.

## Fotos y generador de ficha
Cada vehículo admite foto principal y hasta 6 fotos extra. Desde el perfil puedes crear una ficha PNG con fotos, especificaciones y modificaciones, guardarla y compartirla. La generación es local y no necesita IA ni subir imágenes a un servidor.
