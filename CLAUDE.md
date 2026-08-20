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
│   │   ├── repository/NetworkMonitor.kt      # Contrato de conectividad
│   │   ├── usecase/WebViewUseCases.kt        # LoadWebsite, NavigateBack, HideElements, HandleError
│   │   └── usecase/IsDeviceOfflineUseCase.kt # Única condición que habilita el diálogo de error
│   ├── data/
│   │   ├── network/AndroidNetworkMonitor.kt  # ConnectivityManager
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
3. `WebViewPage` embebe un `WebView` vía `AndroidView` con JS y DOM storage habilitados, y le
   asigna un `MainWebViewClient` y un `MainWebChromeClient`.
4. `MainWebViewClient` delega `onPageStarted` / `onPageFinished` / `onReceivedError` al
   `MainViewModel`, que actualiza `MainUiState` y registra eventos de analítica. Los errores de
   subrecursos (imágenes, CSS, píxeles de tracking) y los errores HTTP del sitio no se delegan:
   solo se loguean con Timber.
5. `onPageStarted` abre un trace de Firebase Performance (`webview_page_load`); `onPageFinished` lo
   cierra y ejecuta `HideElementsUseCase`, que inyecta JavaScript para ocultar la paginación
   (`#blog-pager`) y los primeros siete elementos `.btn` del blog.
6. `InfoDialog` se muestra **solo si el dispositivo está sin conexión**; durante la carga,
   `LoadingDialog`. Ver [Diálogo de sin conexión](#diálogo-de-sin-conexión).
7. `BackHandler` gestiona la navegación hacia atrás dentro del WebView.

### Diálogo de sin conexión

`InfoDialog` comunica una sola cosa: **el dispositivo no tiene conexión** (sin red o en modo avión).
El sitio que envuelve la app genera errores propios —HTTP 5xx, recursos rotos, JavaScript con
errores, hosts de terceros caídos— y ninguno de esos debe tapar la pantalla con ese diálogo.

Tres filtros, en orden, garantizan eso:

1. `MainWebViewClient.onReceivedError` descarta los fallos que no son del documento principal
   (`WebResourceRequest.isForMainFrame`). El callback se dispara por **cada** recurso que falla, y
   esa era la causa principal del popup espurio.
2. `MainWebViewClient.onReceivedHttpError` nunca llega al ViewModel: un 500 significa que hay
   conexión. Se sobrescribe solo para loguearlo y dejar la decisión asentada.
3. `MainViewModel.onError` consulta `IsDeviceOfflineUseCase` (sobre `AndroidNetworkMonitor` /
   `ConnectivityManager`) y muestra el diálogo únicamente si el dispositivo está sin red. Si hay
   conexión, loguea, registra el error en analítica con `is_offline = false` y apaga el loading.

`AndroidNetworkMonitor` falla hacia "online" ante cualquier duda (servicio no disponible, excepción,
capacidades desconocidas) y a propósito no exige `NET_CAPABILITY_VALIDATED`: un falso "sin conexión"
sobre un sitio que carga bien es peor que omitir el diálogo. Requiere `ACCESS_NETWORK_STATE`, ya
declarado en el manifest.

Al tocar el manejo de errores del WebView, mantener esa regla: **el popup es exclusivamente para
falta de conexión del dispositivo.**

### Enlaces externos

`MainWebViewClient.shouldOverrideUrlLoading` mantiene dentro del WebView **solo** lo que pertenece
al sitio: `IsInternalUrlUseCase` acepta el host de `Common.WEB` y sus subdominios sobre `http(s)`.
Todo lo demás —otro dominio, o esquemas como `mailto:`, `tel:` y `whatsapp:`— se deriva al sistema
con `Intent.ACTION_VIEW`.

Sin esto el WebView intentaba cargar cualquier esquema y terminaba en `ERR_UNKNOWN_URL_SCHEME`, así
que los enlaces de contacto y de compartir no hacían nada.

### Selector de archivos y diálogos de JavaScript

`MainWebChromeClient` habilita `<input type="file">` y, por el solo hecho de existir, los
`alert()` / `confirm()` / `prompt()` del sitio. El callback del WebView se responde exactamente una
vez a través de `FileChooserBridge`; si no se responde, el input queda bloqueado para siempre.

**No implementa `onShowCustomView`**: el video embebido todavía no puede ir a pantalla completa.

### Textos e idiomas

El idioma por defecto es **español**: `res/values/strings.xml` es el fallback para cualquier locale
del dispositivo, y `res/values-en/strings.xml` traduce al inglés. Un dispositivo en inglés ve la
traducción; cualquier otro idioma cae en español.

Los textos de los diálogos no viven en el ViewModel: `MainUiState` guarda **IDs de recurso**
(`errorTitleRes`, `errorDescriptionRes`) y el Composable los resuelve con `stringResource`. Así el
ViewModel no necesita `Context` y el texto acompaña al idioma del dispositivo aun si cambia con la
app abierta. Por el mismo motivo a analítica se envía un identificador estable (`error_type` =
`offline`), nunca el título traducido: mandarlo fragmentaría los datos por idioma.

Al agregar un texto visible, cargarlo en `res/values/strings.xml` (español) **y** en
`res/values-en/strings.xml` (inglés).

### Notas de implementación

- La inicialización de SDKs hace I/O en el hilo principal, así que va envuelta en
  `StrictMode.allowThreadDiskReads()` con restauración en `finally`.
- **Timber se planta en `SaltoInicialApp.onCreate()` antes que cualquier otra cosa**: `DebugTree` en
  debug y `CrashReportingTree` en release, que manda los `WARN` a Crashlytics como breadcrumbs y las
  excepciones `ERROR` como no fatales. Sin plantar un árbol, Timber descarta todo en silencio.
- `usesCleartextTraffic` está en `false` de forma explícita porque en API 24-27 el default del
  sistema es `true`. Si algún recurso del sitio dejara de cargar, revisar primero si viaja por
  `http`.
- `WebViewRepositoryImpl` tiene métodos del contrato sin implementar (`goBack()`, `canGoBack()`,
  `hideElements()`) porque necesitan la instancia de `WebView`; los use cases castean a la
  implementación para usar las sobrecargas que la reciben por parámetro.

## Dependencias principales

- **AGP 9.3.1** / **Kotlin 2.4.0** / **compileSdk 37** / **minSdk 24** / **targetSdk 37** / **JVM 17**
- **Firebase BOM 34.18.0**: Analytics, Crashlytics, Performance Monitoring
- **Compose BOM 2026.08.00**: UI, Material3
- **Analítica y atribución**: AppsFlyer 7.0.1 (+ Install Referrer 2.2), Amplitude 1.30.1,
  Mixpanel 8.9.0, Meta SDK 18.3.0 + Audience Network 6.22.0
- **Monitoreo**: Sentry 8.53.0, New Relic 7.8.1
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
