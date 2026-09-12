# Mi Garaje build stack

Validated configuration target for GitHub Actions:
- JDK 17
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- compileSdk / targetSdk 36
- Kotlin 2.3.21
- Compose BOM 2025.12.00
- Coil 3.5.0
- AndroidX Core 1.17.0

CI installs Gradle 8.13 directly, so the repository intentionally does not include the previous custom `gradle-wrapper.jar` that GitHub rejected.
Release builds require the documented GitHub secrets, including `DEVELOPER_CONTACT`; no fake fallback email is used.
