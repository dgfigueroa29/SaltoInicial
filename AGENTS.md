# SaltoInicial Android App - AI Agent Guidelines

## Project Overview

Single-module Android app that wraps `https://www.saltoinicial.com.ar/` in a native WebView using
Jetpack Compose. Now implements Clean Architecture with domain, data, and presentation layers.

## Architecture Overview (Clean Architecture)

### Domain Layer (`domain/`)

- **Models** (`domain/models/`): Core business models like `WebViewState`, `WebViewError`,
  `WebViewConfig`
- **Repository Interfaces** (`domain/repository/`): `WebViewRepository` interface defining data
  operations; `NetworkMonitor` interface for device connectivity
- **Use Cases** (`domain/usecase/`): Business logic classes like `LoadWebsiteUseCase`,
  `NavigateBackUseCase`, `HideElementsUseCase`, `IsDeviceOfflineUseCase`

### Data Layer (`data/`)

- **Repository Implementations** (`data/repository/`): `WebViewRepositoryImpl` containing actual
  WebView operations and state management
- **Connectivity** (`data/network/`): `AndroidNetworkMonitor` backed by `ConnectivityManager`

### Presentation Layer (`presentation/`)

- **ViewModels** (`presentation/viewmodel/`): `MainViewModel` with `MainViewModelFactory` for
  dependency injection
- **UI State & Events** (`presentation/state/`): `MainUiState` and `MainUiEvent` following MVI
  pattern
- **UI Components** (`presentation/ui/`): Stateless composables like `LoadingDialog`,
  `MainWebViewClient`
- **Analytics** (`presentation/analytics/`): `MultiAnalyticsTracker` supporting Firebase, AppsFlyer,
  Amplitude, Meta (Facebook) and Mixpanel tracking; `AnalyticsEvents` and `AnalyticsParams` define
  event names and parameter constants. Providers whose key is missing are passed as `null` and
  skipped

## Key Implementation Details

### WebView Integration

```kotlin
// Repository manages WebView state and operations
class WebViewRepositoryImpl(private var webView: WebView? = null) : WebViewRepository

// ViewModel orchestrates business logic
class MainViewModel(
    private val loadWebsiteUseCase: LoadWebsiteUseCase,
    // ... other use cases
    private val isDeviceOfflineUseCase: IsDeviceOfflineUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel()


```

### State Management (MVI Pattern)

- **State**: `MainUiState` with loading, error dialog, and navigation states
- **Events**: `MainUiEvent` sealed class for user interactions
- **ViewModel**: Single source of truth with immutable state updates

### Dependency Injection

Manual DI through ViewModel factory pattern (no Hilt for simplicity):

```kotlin
class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = WebViewRepositoryImpl()
        val useCases = // ... create use cases
        return MainViewModel(repository, useCases)
    }
}
```

## Build & Dependencies

### Version Management

- **Version Catalogs**: `gradle/libs.versions.toml` for all dependencies
- **AGP**: `9.3.1` / **Kotlin**: `2.4.0`
- **Compose BOM**: `2026.08.00` for UI components
- **Firebase BOM**: `34.18.0` for Analytics/Crashlytics/Performance

### Key Dependencies

- **Error Tracking**: Sentry 8.53.0 (initialized in `SaltoInicialApp`), New Relic 7.8.1 (initialized
  in `MainActivity`)
- **Logging**: Timber 5.0.1 (debug tree in development)
- **Analytics**: Multi-provider tracking:
    - Firebase Analytics 23.2.0, Crashlytics 20.1.0, Performance Monitoring (from BOM)
    - AppsFlyer 7.0.1 + Install Referrer 2.2
    - Amplitude 1.30.1
    - Mixpanel 8.9.0
    - **Meta (Facebook)**: SDK 18.3.0 for App Events, Audience Network 6.22.0 for ads
- **Debug Tools**: LeakCanary 2.14 (debugImplementation only)
- **Testing**: MockK 1.14.11, Turbine 1.2.1, kotlinx-coroutines-test 1.11.0

### Build Configuration

- **minSdk**: 24 (Android 7.0)
- **compileSdk** / **targetSdk**: 37
- **versionCode / versionName**: 6 / `1.6`
- **JVM Target**: 17
- **R8**: `isMinifyEnabled` + `isShrinkResources` enabled for release builds
- **BuildConfig Fields** (all from `local.properties` or gradle properties):
    - `APPSFLYER_DEV_KEY`
    - `AMPLITUDE_API_KEY`
    - `SENTRY_DSN`
    - `NEW_RELIC_APP_TOKEN`
    - `MIXPANEL_PROJECT_TOKEN`
    - `FACEBOOK_APP_ID`, `FACEBOOK_CLIENT_TOKEN` (also injected as `manifestPlaceholders`)
- **Code Quality**: Detekt 1.23.8 (`config/detekt/detekt.yml`, `autoCorrect` on) with HTML, XML,
  TXT, and SARIF reports

## Development Workflow

### Environment Configuration

> **This is a public repository.** Never commit API keys, tokens, DSNs or keystores. `.gitignore`
> excludes `local.properties`; keep it that way and use placeholder values in any documentation or
> example you write.

Keys go in `local.properties` or gradle properties (`-PappsFlyerDevKey=...`):

```properties
appsFlyerDevKey=YOUR_APPSFLYER_KEY
amplitudeApiKey=YOUR_AMPLITUDE_KEY
sentryDsn=YOUR_SENTRY_DSN
facebookAppId=YOUR_FACEBOOK_APP_ID
facebookClientToken=YOUR_FACEBOOK_CLIENT_TOKEN
newRelicAppToken=YOUR_NEW_RELIC_APP_TOKEN
mixpanelProjectToken=YOUR_MIXPANEL_PROJECT_TOKEN
```

These are injected into `BuildConfig` at compile time and consumed by:

- `SaltoInicialApp.onCreate()` for Sentry initialization
- `MainActivity` for New Relic and Analytics tracker initialization

Every key is optional at build time: a blank value means the corresponding SDK is skipped and a
Timber warning is logged. The app compiles and runs with none of them set.

`app/google-services.json` **is** versioned. The Firebase API key it contains is not a secret — it
is extractable from any APK — and real protection comes from Firebase Security Rules and App Check,
not from hiding the file.

### Testing Commands

```bash
./gradlew test                              # Unit tests
./gradlew connectedAndroidTest              # Instrumented tests
./gradlew testDebugUnitTest --tests "com.boa.saltoinicial.ExampleUnitTest"
./gradlew detekt                            # Code quality analysis
```

### Build Commands

```bash
./gradlew assembleDebug                     # Debug APK
./gradlew assembleRelease                   # Release APK (with ProGuard)
./gradlew installDebug                      # Install on device
```

### Debug Features (DEBUG builds only)

- **StrictMode**: Detects disk I/O, networking on main thread, activity leaks
- **Timber**: Logging via `Timber.d()`, `Timber.w()`, `Timber.e()`
- **LeakCanary**: Memory leak detection in debug flavor

## Code Style Conventions

### File Structure

```
app/src/main/java/com/boa/
├── saltoinicial/
│   ├── domain/
│   │   ├── models/WebViewModels.kt             # Domain entities
│   │   ├── repository/WebViewRepository.kt     # Repository contracts
│   │   ├── repository/NetworkMonitor.kt        # Connectivity contract
│   │   ├── usecase/WebViewUseCases.kt          # Business logic
│   │   └── usecase/IsDeviceOfflineUseCase.kt   # Gates the offline dialog
│   ├── data/
│   │   ├── network/AndroidNetworkMonitor.kt    # ConnectivityManager
│   │   └── repository/WebViewRepositoryImpl.kt # Data implementations
│   ├── presentation/
│   │   ├── analytics/
│   │   │   └── AnalyticsTracker.kt             # Multi-provider analytics
│   │   ├── viewmodel/MainViewModel.kt          # State management
│   │   ├── viewmodel/MainViewModelFactory.kt   # DI factory
│   │   ├── state/MainState.kt                  # UI state/events
│   │   └── ui/                                 # UI components
│   │       ├── LoadingDialog.kt
│   │       ├── MainWebViewClient.kt
│   │       └── InfoDialog.kt              # Only shown when the device is offline
│   ├── MainActivity.kt                         # App entry point
│   ├── SaltoInicialApp.kt                      # Application class: Sentry + StrictMode init
│   └── ui/theme/                               # Material3 theming
└── utils/Common.kt                             # Site URL (WEB) and permissions
```

### Naming Patterns

- **Domain Models**: PascalCase with descriptive names (`WebViewState`, `WebViewError`)
- **Use Cases**: Verb + UseCase suffix (`LoadWebsiteUseCase`, `NavigateBackUseCase`)
- **ViewModels**: Feature + ViewModel (`MainViewModel`)
- **UI State**: Feature + UiState (`MainUiState`)
- **UI Events**: Feature + UiEvent (`MainUiEvent`)
- **Composables**: PascalCase with descriptive names (`WebViewPage`, `LoadingDialog`)

### Error Handling

- **WebView Errors**: Converted to domain `WebViewError` types
- **UI State**: Error dialogs managed through immutable state
- **Crashlytics**: Exception logging in `MainActivity.onCreate()`
- **Offline dialog**: `InfoDialog` means one thing only — the device has no connection. Subresource
  failures and HTTP errors (including 500s) from the wrapped site are logged, never shown. See
  "Diálogo de sin conexión" in `CLAUDE.md` before touching `MainWebViewClient.onReceivedError` or
  `MainViewModel.onError`
- **WebView lifecycle**: the WebView is hoisted with `remember` in `FullWebViewPage` and bound to
  the lifecycle — paused, state-saved and destroyed. `setWebView(loadInitialUrl = false)` after a
  `restoreState`, and `detachWebView()` on dispose. See "Ciclo de vida del WebView" in `CLAUDE.md`
- **External links**: `shouldOverrideUrlLoading` keeps only the wrapped site inside the WebView
  (`IsInternalUrlUseCase`); other domains and non-http schemes go out through `Intent.ACTION_VIEW`
- **Logging**: Timber is planted in `SaltoInicialApp.onCreate()` — `DebugTree` in debug,
  `CrashReportingTree` in release. Never add a Timber call assuming it is planted elsewhere
- **Localized copy**: dialog text lives in string resources — Spanish is the default
  (`res/values/strings.xml`), English is the translation (`res/values-en/strings.xml`).
  `MainUiState` carries `@StringRes` ids, never resolved `String`s, and analytics gets a stable
  `error_type` instead of translated copy. Add every new user-facing string to both files

## Common Tasks

### Adding New Features

1. Define domain models in `domain/models/`
2. Create use case in `domain/usecase/`
3. Implement in repository `data/repository/`
4. Add to ViewModel state/events
5. Create/update UI components

### Modifying WebView Behavior

1. Update `WebViewRepository` interface
2. Implement in `WebViewRepositoryImpl`
3. Create/modify use case
4. Update ViewModel and UI state
5. Modify composables as needed

### Adding Dependencies

1. Add to `gradle/libs.versions.toml` with version reference
2. Use alias in `app/build.gradle.kts` dependencies block
3. Follow existing Firebase/Compose BOM patterns
4. If the dependency is a third-party SDK that collects user data, update
   `docs/play-data-safety.md` and review the Play Console declaration — see below

## Privacy & Play Data Safety

The app ships ten third-party SDKs for analytics, attribution and monitoring. Several of them read
the Advertising ID (GAID) or generate persistent installation identifiers, and several inject
`com.google.android.gms.permission.AD_ID` into the merged manifest on their own. Google Play checks
this against the **Data safety** declaration in Play Console and rejects releases that
under-declare.

**[`docs/play-data-safety.md`](docs/play-data-safety.md)** is the source of truth: it holds the
per-SDK inventory of what is collected and the exact declaration to fill in Play Console.

Rules when working in this repo:

- **Adding, removing or reconfiguring any third-party SDK means updating
  `docs/play-data-safety.md` and reviewing the Play Console declaration before publishing.** The gap
  between what the app does and what the form declares is exactly what Play penalizes.
- `com.google.android.gms.permission.AD_ID` is declared on purpose in
  `app/src/main/AndroidManifest.xml`. Do not remove it without reading that document first — and
  note that deleting the line alone would not stop collection, since `firebase-analytics` and
  AppsFlyer re-inject it through manifest merge.
- Valid Meta SDK manifest meta-data keys are the `FacebookSdk` constants only: `AutoInitEnabled`,
  `AutoLogAppEventsEnabled`, `AdvertiserIDCollectionEnabled`, `CodelessDebugLogEnabled`,
  `MonitorEnabled`. Anything else is a silent no-op — the manifest previously carried a misspelled
  `AdvertisingIdCollectionEnabled` that did nothing while appearing to enable ad ID collection.
