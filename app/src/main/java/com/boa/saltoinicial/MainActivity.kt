package com.boa.saltoinicial

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amplitude.android.Amplitude
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
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber
import com.amplitude.android.Configuration as AmplitudeConfiguration

/**
 * Activity principal de la aplicación.
 *
 * Inicializa Firebase Analytics, Crashlytics, AppsFlyer y Amplitude mediante [setupTracking],
 * luego renderiza [WebViewPage] dentro del tema [SaltoInicialTheme].
 * Los errores durante la inicialización de la UI se capturan y se reportan a Crashlytics
 * para evitar crashes silenciosos en producción.
 */
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var analyticsTracker: MultiAnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        setupTracking()
        val crashlytics: FirebaseCrashlytics = Firebase.crashlytics
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
     * Configura e inicializa los SDKs de analítica: AppsFlyer y Amplitude.
     * Si alguna clave de API está en blanco (no configurada en [BuildConfig]),
     * el SDK correspondiente no se inicializa y se registra una advertencia en Timber.
     * Finalmente crea [MultiAnalyticsTracker] y envía el evento [AnalyticsEvents.APP_OPEN].
     */
    private fun setupTracking() {
        val appsFlyerDevKey = BuildConfig.APPSFLYER_DEV_KEY
        val appsFlyer = AppsFlyerLib.getInstance()
        if (appsFlyerDevKey.isNotBlank()) {
            appsFlyer.init(appsFlyerDevKey, null, applicationContext)
            appsFlyer.start(this)
            Timber.i("AppsFlyer initialized")
        }

        val amplitudeApiKey = BuildConfig.AMPLITUDE_API_KEY
        val amplitude = if (amplitudeApiKey.isBlank()) {
            Timber.w("Amplitude API key is missing; Amplitude will not be initialized.")
            null
        } else {
            Timber.i("Amplitude initialized")
            Amplitude(
                AmplitudeConfiguration(
                    apiKey = amplitudeApiKey,
                    context = applicationContext
                )
            )
        }

        analyticsTracker = MultiAnalyticsTracker(
            context = this,
            firebaseAnalytics = firebaseAnalytics,
            appsFlyer = appsFlyer,
            amplitude = amplitude
        )

        analyticsTracker.trackEvent(
            AnalyticsEvents.APP_OPEN,
            mapOf(
                AnalyticsParams.SOURCE to "cold_start",
                AnalyticsParams.PLATFORM to "android"
            )
        )
    }
}

/**
 * Composable principal que embebe el [WebView] de la app.
 *
 * Observa el [MainUiState] del [viewModel] para mostrar el [LoadingDialog] durante la carga
 * y el [InfoDialog] ante errores de red. En modo preview (LayoutLib) muestra un placeholder
 * ya que [WebView] no es compatible con Compose Preview.
 *
 * @param viewModel ViewModel que gestiona el estado del WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPage(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Configure WebView settings
                    settings.javaScriptEnabled = true
                    settings.userAgentString = System.getProperty("http.agent")
                    settings.useWideViewPort = true

                    // Set custom WebViewClient
                    webViewClient = MainWebViewClient(viewModel)

                    // Set WebView in ViewModel
                    viewModel.setWebView(this)
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
