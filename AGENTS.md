# SaltoInicial Android App - AI Agent Guidelines

## Project Overview

Single-module Android app that wraps `https://www.saltoinicial.com.ar/` in a native WebView using
Jetpack Compose. Now implements Clean Architecture with domain, data, and presentation layers.

## Architecture Overview (Clean Architecture)

### Domain Layer (`domain/`)

- **Models** (`domain/models/`): Core business models like `WebViewState`, `WebViewError`,
  `WebViewConfig`
- **Repository Interfaces** (`domain/repository/`): `WebViewRepository` interface defining data
  operations
- **Use Cases** (`domain/usecase/`): Business logic classes like `LoadWebsiteUseCase`,
  `NavigateBackUseCase`, `HideElementsUseCase`

### Data Layer (`data/`)

- **Repository Implementations** (`data/repository/`): `WebViewRepositoryImpl` containing actual
  WebView operations and state management

### Presentation Layer (`presentation/`)

- **ViewModels** (`presentation/viewmodel/`): `MainViewModel` with `MainViewModelFactory` for
  dependency injection
- **UI State & Events** (`presentation/state/`): `MainUiState` and `MainUiEvent` following MVI
  pattern
- **UI Components** (`presentation/ui/`): Stateless composables like `LoadingDialog`,
  `MainWebViewClient`
- **Analytics** (`presentation/analytics/`): `MultiAnalyticsTracker` supporting Firebase, AppsFlyer,
  Amplitude, and Meta (Facebook) tracking; `AnalyticsEvents` and `AnalyticsParams` define event names and parameter
  constants

## Key Implementation Details

### WebView Integration

```kotlin
// Repository manages WebView state and operations
class WebViewRepositoryImpl(private var webView: WebView? = null) : WebViewRepository

// ViewModel orchestrates business logic
class MainViewModel(
    private val repository: WebViewRepository,
    private val loadWebsiteUseCase: LoadWebsiteUseCase,
    // ... other use cases
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
- **Compose BOM**: `2026.02.01` for UI components
- **Firebase BOM**: `34.10.0` for Crashlytics/Analytics

### Key Dependencies

- **Error Tracking**: Sentry 8.29.0 (initialized in `SaltoInicialApp`)
- **Logging**: Timber 5.0.1 (debug tree in development)
- **Analytics**: Multi-provider tracking:
  - Firebase Analytics 23.2.0
  - AppsFlyer 6.18.0
  - Amplitude 1.27.0
  - **Meta (Facebook)**: App Events 17.0.0 for analytics, Audience Network 6.18.0 for ads
- **Debug Tools**: LeakCanary 2.14 (debugImplementation only)
- **Testing**: MockK 1.14.9, Turbine 1.2.1, kotlinx-coroutines-test 1.10.2

### Build Configuration

- **minSdk**: 23 (Android 6.0)
- **compileSdk**: 36 (Android 12)
- **JVM Target**: 17
- **ProGuard**: Enabled for release builds
- **BuildConfig Fields**: 
  - `APPSFLYER_DEV_KEY` (from `local.properties` or gradle properties)
  - `AMPLITUDE_API_KEY` (from `local.properties` or gradle properties)
  - `SENTRY_DSN` (from `local.properties` or gradle properties)
  - `FACEBOOK_APP_ID` (from `local.properties` or gradle properties)
- **Code Quality**: Detekt 1.23.8 configured with HTML, XML, TXT, and SARIF reports

## Development Workflow

### Environment Configuration

Required keys in `local.properties` or gradle properties:
```properties
appsFlyerDevKey=YOUR_APPSFLYER_KEY
amplitudeApiKey=YOUR_AMPLITUDE_KEY
sentryDsn=YOUR_SENTRY_DSN
facebookAppId=YOUR_FACEBOOK_APP_ID
```

These are injected into `BuildConfig` at compile time and consumed by:
- `SaltoInicialApp.onCreate()` for Sentry initialization
- `MainActivity` for Analytics tracker initialization

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
app/src/main/java/com/boa/saltoinicial/
├── domain/
│   ├── models/WebViewModels.kt             # Domain entities
│   ├── repository/WebViewRepository.kt     # Repository contracts
│   └── usecase/WebViewUseCases.kt          # Business logic
├── data/
│   └── repository/WebViewRepositoryImpl.kt # Data implementations
├── presentation/
│   ├── analytics/
│   │   └── AnalyticsTracker.kt            # Multi-provider analytics (Firebase, AppsFlyer, Amplitude, Meta)
│   ├── viewmodel/MainViewModel.kt          # State management
│   ├── viewmodel/MainViewModelFactory.kt   # DI factory
│   ├── state/MainState.kt                  # UI state/events
│   └── ui/                                # UI components
│       ├── LoadingDiaTimber.kt
│       ├── MainWebViewClient.kt
│       └── InfoDiaTimber.kt
├── MainActivity.kt                         # App entry point
├── SaltoInicialApp.kt                     # Application class with Sentry, Timber, StrictMode init
└── ui/theme/                               # Material3 theming
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
