# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Descripción

**SaltoInicial** es una app Android que envuelve el sitio web `https://www.saltoinicial.com.ar/` en
un WebView nativo con Jetpack Compose. Es un single-module Android project.

## Comandos

```bash
# Compilar debug
./gradlew assembleDebug

# Compilar release
./gradlew assembleRelease

# Ejecutar tests unitarios
./gradlew test

# Ejecutar tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Ejecutar un test específico
./gradlew testDebugUnitTest --tests "com.boa.saltoinicial.ExampleUnitTest"

# Limpiar build
./gradlew clean

# Instalar en dispositivo conectado
./gradlew installDebug
```

## Arquitectura

Proyecto de módulo único (`:app`). No usa ViewModel, Room, ni arquitectura MVVM/MVI. La lógica vive
directamente en los Composables.

### Archivos clave

- `app/src/main/java/com/boa/saltoinicial/MainActivity.kt` — Entry point. Inicializa Firebase
  Crashlytics y renderiza `WebViewPage`.
- `app/src/main/java/com/boa/saltoinicial/InfoDialog.kt` — Diálogo Composable que se muestra cuando
  hay error de red (sin conexión).
- `app/src/main/java/com/boa/utils/Common.kt` — Contiene la URL del sitio (
  `WEB = "https://www.saltoinicial.com.ar/"`) y permisos.
- `app/src/main/java/com/boa/saltoinicial/ui/theme/` — Tema Material3 (Color, Theme, Type).

### Flujo principal

1. `MainActivity` carga `WebViewPage(WEB)` en Compose.
2. `WebViewPage` embebe un `WebView` via `AndroidView` con JS habilitado.
3. Al iniciar carga (`onPageStarted`) muestra un loading dialog fullscreen.
4. Al terminar (`onPageFinished`) oculta el dialog y llama `removeElement()` para esconder elementos
   HTML específicos del blog (paginación, botones `.btn`).
5. En error de red (`onReceivedError`) muestra `InfoDialog`.
6. `BackHandler` gestiona la navegación hacia atrás dentro del WebView.

### Dependencias principales

- **Firebase BOM 34.10.0**: Crashlytics + Analytics
- **Compose BOM 2026.02.01**: UI, Material3
- **AGP 9.1.0** / **Kotlin 2.3.10** / **compileSdk 36** / **minSdk 23** / **JVM 17**
