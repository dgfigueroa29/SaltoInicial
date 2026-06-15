package com.boa.saltoinicial

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amplitude.core.Amplitude
import com.appsflyer.AppsFlyerLib
import com.boa.saltoinicial.presentation.analytics.AnalyticsEvents
import com.boa.saltoinicial.presentation.analytics.AnalyticsParams
import com.boa.saltoinicial.presentation.analytics.MultiAnalyticsTracker
import com.boa.saltoinicial.presentation.state.MainUiEvent
import com.boa.saltoinicial.presentation.ui.InfoDialog
import com.boa.saltoinicial.presentation.ui.LoadingDialog
import com.boa.saltoinicial.presentation.ui.MainWebViewClient
import com.boa.saltoinicial.presentation.viewmodel.MainViewModel
import com.boa.saltoinicial.presentation.viewmodel.MainViewModelFactory
import com.boa.saltoinicial.ui.theme.SaltoInicialTheme
import com.facebook.FacebookSdk
import com.facebook.ads.AudienceNetworkAds
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.crashlytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.newrelic.agent.android.NewRelic
import timber.log.Timber

/**
 * Activity principal de la aplicación.
 *
 * Inicializa New Relic (si hay token), Firebase Analytics, Crashlytics, AppsFlyer, Amplitude
 * y Mixpanel mediante [setupTracking],
 * luego renderiza [WebViewPage] dentro del tema [SaltoInicialTheme].
 * Los errores durante la inicialización de la UI se capturan y se reportan a Crashlytics
 * para evitar crashes silenciosos en producción.
 */
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var analyticsTracker: MultiAnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Permitir lectura de disco en el hilo principal para la inicialización de los SDK,
        // que de lo contrario lanza una violación de StrictMode en debug.
        val oldPolicy = StrictMode.allowThreadDiskReads()
        val crashlytics = Firebase.crashlytics
        try {
            val newRelicToken = BuildConfig.NEW_RELIC_APP_TOKEN
            if (newRelicToken.isNotBlank()) {
                NewRelic.withApplicationToken(newRelicToken).start(applicationContext)
                Timber.i("New Relic started")
            } else {
                Timber.w("New Relic app token is missing; agent will not start.")
            }
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
            setupTracking()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }

        try {
            setContent {
                SaltoInicialTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val viewModel: MainViewModel = viewModel(
                            factory = MainViewModelFactory(
                                analyticsTracker = analyticsTracker
                            )
                        )
                        WebViewPage(viewModel = viewModel)
                    }
                }
            }
        } catch (e: Exception) {
            // Catch any unexpected exceptions during UI initialization
            // This is intentionally broad to prevent app crashes during startup
            crashlytics.log("MainActivity OnCreate: ${e.message}")
        }
    }

    /**
     * Configura e inicializa los SDK de analítica: AppsFlyer, Amplitude y Mixpanel.
     * Si alguna clave de API está en blanco (no configurada en [BuildConfig]),
     * el SDK correspondiente no se inicializa y se registra una advertencia en Timber.
     * Finalmente, crea [MultiAnalyticsTracker] y envía el evento [AnalyticsEvents.APP_OPEN].
     */
    private fun setupTracking() {
        val appsFlyerDevKey = BuildConfig.APPSFLYER_DEV_KEY
        val appsFlyer = AppsFlyerLib.getInstance()
        if (appsFlyerDevKey.isNotBlank()) {
            appsFlyer.init(appsFlyerDevKey, null, applicationContext)
            appsFlyer.start()
            Timber.i("AppsFlyer initialized")
        }

        val amplitudeApiKey = BuildConfig.AMPLITUDE_API_KEY
        val amplitude = if (amplitudeApiKey.isBlank()) {
            Timber.w("Amplitude API key is missing; Amplitude will not be initialized.")
            null
        } else {
            Timber.i("Amplitude initialized")
            Amplitude(
                configuration = com.amplitude.core.Configuration(
                    apiKey = amplitudeApiKey,
                    offline = true,
                    useBatch = true
                )
            )
        }
        val facebookLogger = getFacebookLogger()
        val mixpanelToken = BuildConfig.MIXPANEL_PROJECT_TOKEN
        val mixpanel = if (mixpanelToken.isBlank()) {
            Timber.w("Mixpanel project token is missing; Mixpanel will not be initialized.")
            null
        } else {
            Timber.i("Mixpanel initialized")
            MixpanelAPI.getInstance(applicationContext, mixpanelToken, false)
        }

        analyticsTracker = MultiAnalyticsTracker(
            context = this,
            firebaseAnalytics = firebaseAnalytics,
            appsFlyer = appsFlyer,
            amplitude = amplitude,
            facebookLogger = facebookLogger,
            mixpanel = mixpanel
        )

        analyticsTracker.trackEvent(
            name = AnalyticsEvents.APP_OPEN,
            params = mapOf(
                AnalyticsParams.SOURCE to "cold_start",
                AnalyticsParams.PLATFORM to "android"
            )
        )
    }

    private fun getFacebookLogger(): AppEventsLogger? {
        // Initialize Facebook SDK
        val facebookAppId = BuildConfig.FACEBOOK_APP_ID
        val facebookClientToken = BuildConfig.FACEBOOK_CLIENT_TOKEN
        val facebookLogger = if (facebookAppId.isNotBlank()) {
            try {
                FacebookSdk.setApplicationId(facebookAppId)
                if (facebookClientToken.isNotBlank()) {
                    FacebookSdk.setClientToken(facebookClientToken)
                }
                FacebookSdk.fullyInitialize()
                AudienceNetworkAds.initialize(this)
                Timber.i("Facebook SDK and Audience Network initialized")
                AppEventsLogger.newLogger(this)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize Facebook SDK")
                null
            }
        } else {
            Timber.w("Facebook App ID is missing; Facebook SDK will not be initialized.")
            null
        }
        return facebookLogger
    }
}

/**
 * Composable principal que embebe el [WebView] de la app.
 *
 * Observa el MainUiState del [viewModel] para mostrar el [LoadingDialog] durante la carga
 * y el [InfoDialog] ante errores de red. En modo preview (LayoutLib) muestra un placeholder,
 * ya que [WebView] no es compatible con Compose Preview.
 *
 * @param viewModel ViewModel que gestiona el estado del WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPage(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // The Configuration object represents all the current configurations,
    // not just the ones that have changed.
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            println("landscape")
        }

        else -> {
            println("portrait")
        }
    }

    // Adding a WebView inside AndroidView
    // with layout as full screen
    if (LocalInspectionMode.current) {
        // Show a placeholder in the preview to avoid the WebView rendering issue
        // WebView is not fully supported in LayoutLib (Compose Preview)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "WebView Placeholder", color = MaterialTheme.colorScheme.primary)
        }
    } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val oldPolicy = StrictMode.allowThreadDiskReads()
                try {
                    WebView(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Configure WebView settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = System.getProperty("http.agent")
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        // Set custom WebViewClient
                        webViewClient = MainWebViewClient(viewModel)

                        // Set WebView in ViewModel
                        viewModel.setWebView(this)
                    }
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy)
                }
            }
        )
    }

    // Show loading dialog
    if (uiState.isLoading) {
        LoadingDialog(
            onDismiss = { /* Loading dialog cannot be dismissed */ }
        )
    }

    // Show error dialog
    if (uiState.showErrorDialog) {
        InfoDialog(
            title = uiState.errorTitle,
            desc = uiState.errorDescription,
            onDismiss = {
                viewModel.onEvent(MainUiEvent.DismissErrorDialog)
            }
        )
    }

    // Handle back navigation
    BackHandler(enabled = uiState.canGoBack) {
        viewModel.onEvent(MainUiEvent.NavigateBack)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SaltoInicialTheme {
        // For preview, we'll show a placeholder since ViewModel isn't available in preview
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "WebView Preview", color = MaterialTheme.colorScheme.primary)
        }
    }
}
