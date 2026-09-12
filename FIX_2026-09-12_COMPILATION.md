# Fix de compilación 2026-09-12

Se corrigieron los errores reportados por GitHub Actions en compileDebugKotlin:

- BillingManager: offerToken nullable tratado de forma segura.
- PhotoRepository: FirebaseStorage nullable capturado antes de usarlo dentro de runCatching.
- VehicleCardGenerator: drawBitmap usa Rect como source y RectF como destination.
- GarageScreens / RecordScreens: import correcto de KeyboardOptions desde androidx.compose.foundation.text.
- GarageScreens: import correcto de CropImageView.Guidelines.
- Pantallas Material3: opt-in a ExperimentalMaterial3Api donde se usan APIs experimentales.

El stack se mantiene en AGP 8.13.2, Gradle 8.13, Kotlin 2.3.21, JDK 17 y SDK 36.
