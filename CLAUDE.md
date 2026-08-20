# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Descripción

**SaltoInicial** es una app Android que envuelve el sitio web `https://www.saltoinicial.com.ar/` en
un WebView nativo con Jetpack Compose. Es un proyecto de módulo único (`:app`).

> Repositorio **público**: nunca commitear claves, tokens, DSNs ni keystores. Ver
> [Configuración local](#configuración-local).

## Comandos

```bash
# Compilar debug
./gradlew assembleDebug

# Compilar release (con R8 + shrinkResources)
./gradlew assembleRelease

# Ejecutar tests unitarios
./gradlew test

# Ejecutar tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Ejecutar un test específico
./gradlew testDebugUnitTest --tests "com.boa.saltoinicial.presentation.viewmodel.MainViewModelTest"

# Análisis estático (config en config/detekt/detekt.yml, autoCorrect activado)
./gradlew detekt

# Limpiar build
./gradlew clean

# Instalar en dispositivo conectado
./gradlew installDebug
```

## Configuración local

Los SDKs de terceros se configuran con claves inyectadas en `BuildConfig` en tiempo de compilación.
Van en `local.properties` (ignorado por git) o como propiedades de Gradle (`-PclaveX=valor`):

```properties
appsFlyerDevKey=
amplitudeApiKey=
sentryDsn=
newRelicAppToken=
mixpanelProjectToken=
facebookAppId=
facebookClientToken=
```

Cada clave ausente degrada con gracia: el SDK correspondiente no se inicializa y se loguea una
advertencia con Timber. La app compila y corre sin ninguna de ellas.

`facebookAppId` y `facebookClientToken` además se inyectan como `manifestPlaceholders`.

**`local.properties` nunca debe commitearse.** `app/google-services.json` sí está versionado: la API
key de Firebase que contiene no es un secreto (es extraíble de cualquier APK), pero la seguridad
real depende de las Firebase Security Rules y de App Check, no de ocultar ese archivo.

## Arquitectura

Clean Architecture en tres capas dentro del módulo `:app`, con patrón MVI en presentación y DI
manual vía `ViewModelProvider.Factory` (sin Hilt).

```
app/src/main/java/com/boa/
├── saltoinicial/
│   ├── MainActivity.kt                       # Entry point: inicializa SDKs y monta Compose
│   ├── SaltoInicialApp.kt                    # Application: StrictMode (debug) + Sentry
│   ├── domain/
│   │   ├── models/WebViewModels.kt           # WebViewState, WebViewError, WebViewConfig
│   │   ├── repository/WebViewRepository.kt   # Contrato
│   │   └── usecase/WebViewUseCases.kt        # LoadWebsite, NavigateBack, HideElements, HandleError
│   ├── data/
│   │   └── repository/WebViewRepositoryImpl.kt
│   ├── presentation/
│   │   ├── analytics/AnalyticsTracker.kt     # Contrato + MultiAnalyticsTracker + eventos/params
│   │   ├── state/MainState.kt                # MainUiState + MainUiEvent
│   │   ├── ui/                               # InfoDialog, LoadingDialog, MainWebViewClient
│   │   └── viewmodel/                        # MainViewModel + MainViewModelFactory
│   └── ui/theme/                             # Tema Material3 (Color, Theme, Type)
└── utils/Common.kt                           # URL del sitio (WEB) y permisos
```

### Flujo principal

1. `SaltoInicialApp.onCreate()` configura StrictMode en debug e inicializa Sentry si hay DSN.
2. `MainActivity.onCreate()` arranca New Relic (si hay token) y Firebase Analytics, y llama a
   `setupTracking()`, que inicializa AppsFlyer, Amplitude, Meta SDK + Audience Network y Mixpanel, y
   construye el `MultiAnalyticsTracker`.
3. `WebViewPage` embebe un `WebView` vía `AndroidView` con JS y DOM storage habilitados, y le asigna
   un `MainWebViewClient`.
4. `MainWebViewClient` delega `onPageStarted` / `onPageFinished` / `onReceivedError` al
   `MainViewModel`, que actualiza `MainUiState` y registra eventos de analítica.
5. `onPageStarted` abre un trace de Firebase Performance (`webview_page_load`); `onPageFinished` lo
   cierra y ejecuta `HideElementsUseCase`, que inyecta JavaScript para ocultar la paginación
   (`#blog-pager`) y los primeros siete elementos `.btn` del blog.
6. En error de red se muestra `InfoDialog`; durante la carga, `LoadingDialog`.
7. `BackHandler` gestiona la navegación hacia atrás dentro del WebView.

### Notas de implementación

- La inicialización de SDKs hace I/O en el hilo principal, así que va envuelta en
  `StrictMode.allowThreadDiskReads()` con restauración en `finally`.
- `WebViewRepositoryImpl` tiene métodos del contrato sin implementar (`goBack()`, `canGoBack()`,
  `hideElements()`) porque necesitan la instancia de `WebView`; los use cases castean a la
  implementación para usar las sobrecargas que la reciben por parámetro.

## Dependencias principales

- **AGP 9.2.1** / **Kotlin 2.4.0** / **compileSdk 37** / **minSdk 24** / **targetSdk 37** / **JVM 17**
- **Firebase BOM 34.14.1**: Analytics, Crashlytics, Performance Monitoring
- **Compose BOM 2026.05.01**: UI, Material3
- **Analítica y atribución**: AppsFlyer 7.0.0 (+ Install Referrer 2.2), Amplitude 1.29.0,
  Mixpanel 8.8.0, Meta SDK 18.2.3 + Audience Network 6.21.0
- **Monitoreo**: Sentry 8.43.2, New Relic 7.7.6
- **Logging**: Timber 5.0.1
- **Calidad**: Detekt 1.23.8
- **Debug**: LeakCanary 2.14
- **Tests**: JUnit 4.13.2, MockK 1.14.11, Turbine 1.2.1, kotlinx-coroutines-test 1.11.0

## Privacidad y Seguridad de datos de Play

La app integra diez SDKs de analítica, atribución y monitoreo que recolectan identificadores de
dispositivo (GAID, Android ID, installation IDs). Google Play revisa que eso esté declarado en la
sección **Seguridad de datos** de Play Console, y rechaza las versiones que sub-declaran.

**[`docs/play-data-safety.md`](docs/play-data-safety.md)** mantiene el inventario de qué recolecta
cada SDK y la declaración exacta a cargar en Play Console.

Reglas al trabajar en este repo:

- **Al agregar, quitar o reconfigurar un SDK de terceros, actualizar `docs/play-data-safety.md` y
  revisar la declaración en Play Console antes de publicar.**
- El permiso `com.google.android.gms.permission.AD_ID` está declarado a propósito en
  `app/src/main/AndroidManifest.xml`. No borrarlo sin leer primero ese documento: además no
  alcanzaría con borrar la línea, porque `firebase-analytics` y AppsFlyer lo reinyectan por manifest
  merge.
- Las claves válidas de meta-data del SDK de Meta son las constantes de `FacebookSdk`
  (`AutoInitEnabled`, `AutoLogAppEventsEnabled`, `AdvertiserIDCollectionEnabled`,
  `CodelessDebugLogEnabled`, `MonitorEnabled`). Cualquier otra es un no-op silencioso.
