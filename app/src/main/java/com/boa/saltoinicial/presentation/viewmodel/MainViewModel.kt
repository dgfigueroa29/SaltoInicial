package com.boa.saltoinicial.presentation.viewmodel

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boa.saltoinicial.R
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
import com.boa.saltoinicial.domain.usecase.IsDeviceOfflineUseCase
import com.boa.saltoinicial.domain.usecase.LoadWebsiteUseCase
import com.boa.saltoinicial.domain.usecase.NavigateBackUseCase
import com.boa.saltoinicial.presentation.analytics.AnalyticsEvents
import com.boa.saltoinicial.presentation.analytics.AnalyticsParams
import com.boa.saltoinicial.presentation.analytics.AnalyticsTracker
import com.boa.saltoinicial.presentation.state.MainUiEvent
import com.boa.saltoinicial.presentation.state.MainUiState
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the main screen following MVI pattern
 */
class MainViewModel(
    private val loadWebsiteUseCase: LoadWebsiteUseCase,
    private val handleWebViewErrorUseCase: HandleWebViewErrorUseCase,
    private val navigateBackUseCase: NavigateBackUseCase,
    private val hideElementsUseCase: HideElementsUseCase,
    private val isDeviceOfflineUseCase: IsDeviceOfflineUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private var currentWebView: WebView? = null
    private var pageLoadTrace: Trace? = null

    init {
        observeConnectivity()
    }

    /**
     * Asocia el [WebView] al ViewModel. Debe llamarse desde el Composable una vez que el [WebView]
     * es creado.
     *
     * @param webView Instancia del WebView a gestionar.
     * @param loadInitialUrl `false` cuando el WebView ya tiene su historial restaurado con
     * `restoreState`: en ese caso cargar la URL inicial pisaría la página donde estaba el usuario.
     */
    fun setWebView(webView: WebView, loadInitialUrl: Boolean = true) {
        currentWebView = webView
        if (loadInitialUrl) {
            loadWebsite()
        }
    }

    /**
     * Suelta la referencia al [WebView]. La llama el Composable antes de destruirlo, para que el
     * ViewModel —que sobrevive a la Activity— no quede reteniendo una vista muerta.
     */
    fun detachWebView() {
        currentWebView = null
    }

    /**
     * Recarga el sitio cuando la red vuelve, pero solo si el usuario está frente al diálogo de sin
     * conexión: si estaba leyendo con normalidad, una recarga sorpresiva le haría perder la página.
     */
    private fun observeConnectivity() {
        viewModelScope.launch {
            isDeviceOfflineUseCase.observe()
                .distinctUntilChanged()
                .collect { isOffline ->
                    if (!isOffline && _uiState.value.showErrorDialog) {
                        Timber.i("Volvió la conexión: se recarga el sitio.")
                        retryLoad()
                    }
                }
        }
    }

    /** Cierra el diálogo de error y vuelve a cargar el sitio. */
    private fun retryLoad() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false)
        loadWebsite()
    }

    /**
     * Punto de entrada para los eventos de UI siguiendo el patrón MVI.
     *
     * @param event Evento disparado desde la UI. Ver [MainUiEvent].
     */
    fun onEvent(event: MainUiEvent) {
        when (event) {
            MainUiEvent.LoadWebsite -> loadWebsite()
            MainUiEvent.DismissErrorDialog -> dismissErrorDialog()
            MainUiEvent.NavigateBack -> navigateBack()
            MainUiEvent.RetryLoad -> onRetry()
            is MainUiEvent.ShowError ->
                showError(event.titleRes, event.descriptionRes, ERROR_TYPE_CUSTOM)
        }
    }

    private fun loadWebsite() {
        currentWebView?.let { webView ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                loadWebsiteUseCase(webView)
            }
        }
    }

    private fun onRetry() {
        analyticsTracker.trackEvent(
            AnalyticsEvents.ERROR_DIALOG_DISMISSED,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.ACTION to "retry"
            )
        )
        retryLoad()
    }

    private fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false)
        analyticsTracker.trackEvent(
            AnalyticsEvents.ERROR_DIALOG_DISMISSED,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.ACTION to "dismiss"
            )
        )
    }

    private fun navigateBack() {
        currentWebView?.let { webView ->
            val canGoBackBefore = webView.canGoBack()
            navigateBackUseCase(webView)
            analyticsTracker.trackEvent(
                AnalyticsEvents.NAVIGATION_BACK,
                mapOf(
                    AnalyticsParams.SCREEN to "webview",
                    AnalyticsParams.ACTION to "back",
                    AnalyticsParams.CAN_GO_BACK to canGoBackBefore
                )
            )
            updateBackNavigationState()
        }
    }

    /**
     * Abre InfoDialog con textos localizados.
     *
     * @param titleRes Recurso del título.
     * @param descriptionRes Recurso de la descripción.
     * @param errorType Identificador estable del diálogo para analítica. No se envía el título
     * porque está traducido y fragmentaría los datos por idioma.
     */
    private fun showError(
        @StringRes titleRes: Int,
        @StringRes descriptionRes: Int,
        errorType: String
    ) {
        _uiState.value = _uiState.value.copy(
            showErrorDialog = true,
            errorTitleRes = titleRes,
            errorDescriptionRes = descriptionRes,
            isLoading = false
        )
        analyticsTracker.trackEvent(
            AnalyticsEvents.ERROR_DIALOG_SHOWN,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.ERROR_TYPE to errorType
            )
        )
    }

    /**
     * Llamado por MainWebViewClient cuando el WebView comienza a cargar una página.
     * Muestra el loading, inicia un trace de Firebase Performance y registra el evento en analítica.
     *
     * @param url URL de la página que comenzó a cargar.
     */
    fun onPageStarted(url: String?) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        pageLoadTrace?.stop()
        pageLoadTrace = FirebasePerformance.getInstance()
            .newTrace("webview_page_load").apply {
                putAttribute("url", url ?: "unknown")
                start()
            }
        analyticsTracker.trackEvent(
            AnalyticsEvents.WEBVIEW_LOAD_START,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.URL to (url ?: "unknown")
            )
        )
        updateBackNavigationState()
    }

    /**
     * Llamado por MainWebViewClient cuando el WebView finaliza de cargar una página.
     * Oculta el loading, detiene el trace de Firebase Performance, registra el evento en analítica
     * y ejecuta [HideElementsUseCase] para ocultar elementos HTML no deseados del blog.
     *
     * @param url URL de la página que terminó de cargar.
     */
    fun onPageFinished(url: String?) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        pageLoadTrace?.apply {
            putAttribute("final_url", url ?: "unknown")
            stop()
        }
        pageLoadTrace = null
        analyticsTracker.trackEvent(
            AnalyticsEvents.WEBVIEW_LOAD_COMPLETE,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.URL to (url ?: "unknown")
            )
        )
        analyticsTracker.trackEvent(
            AnalyticsEvents.SCREEN_VIEW,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.URL to (url ?: "unknown"),
                AnalyticsParams.SOURCE to "navigation"
            )
        )
        currentWebView?.let { webView ->
            viewModelScope.launch {
                hideElementsUseCase(webView)
            }
        }
    }

    /**
     * Llamado por MainWebViewClient cuando falla la carga del documento principal.
     *
     * Delega el manejo al [HandleWebViewErrorUseCase] y registra el evento en analítica siempre,
     * pero **solo muestra el diálogo si el dispositivo está sin conexión** (sin red o en modo
     * avión), que es lo único que ese diálogo comunica.
     *
     * Si hay conexión, el error es del sitio —HTTP 5xx, recursos rotos, JavaScript con errores,
     * un host de terceros caído— y no corresponde tapar la pantalla: se loguea, se registra en
     * analítica y se apaga el loading para no dejar al usuario con el diálogo de carga girando.
     *
     * @param error Error recibido del WebView. Ver [WebViewError].
     * @param failingUrl URL que causó el error, o `null` si no está disponible.
     */
    fun onError(error: WebViewError, failingUrl: String?) {
        handleWebViewErrorUseCase(error)
        val isDeviceOffline = isDeviceOfflineUseCase()
        val type = when (error) {
            is WebViewError.NetworkError -> "network_error"
            is WebViewError.GenericError -> "generic_error"
        }
        analyticsTracker.trackEvent(
            AnalyticsEvents.WEBVIEW_ERROR,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.URL to (failingUrl ?: "unknown"),
                AnalyticsParams.ERROR_TYPE to type,
                AnalyticsParams.ERROR_MESSAGE to error.description,
                AnalyticsParams.IS_OFFLINE to isDeviceOffline
            )
        )
        if (isDeviceOffline) {
            showError(R.string.offline, R.string.offline_desc, ERROR_TYPE_OFFLINE)
        } else {
            Timber.w(
                "Error de carga con conexión disponible en %s: %s. No se muestra el diálogo.",
                failingUrl ?: "unknown",
                error.description
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun updateBackNavigationState() {
        val canGoBack = currentWebView?.canGoBack() == true
        _uiState.value = _uiState.value.copy(canGoBack = canGoBack)
    }

    private companion object {
        /** Identificadores del diálogo para analítica: constantes, nunca texto traducido. */
        const val ERROR_TYPE_OFFLINE = "offline"
        const val ERROR_TYPE_CUSTOM = "custom"
    }
}
