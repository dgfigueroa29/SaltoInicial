package com.boa.saltoinicial.presentation.viewmodel

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.repository.WebViewRepository
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
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
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen following MVI pattern
 */
class MainViewModel(
    private val webViewRepository: WebViewRepository,
    private val loadWebsiteUseCase: LoadWebsiteUseCase,
    private val handleWebViewErrorUseCase: HandleWebViewErrorUseCase,
    private val navigateBackUseCase: NavigateBackUseCase,
    private val hideElementsUseCase: HideElementsUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentWebView: WebView? = null
    private var pageLoadTrace: Trace? = null

    fun setWebView(webView: WebView) {
        currentWebView = webView
        loadWebsite()
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            MainUiEvent.LoadWebsite -> loadWebsite()
            MainUiEvent.DismissErrorDialog -> dismissErrorDialog()
            MainUiEvent.NavigateBack -> navigateBack()
            is MainUiEvent.ShowError -> showError(event.title, event.description)
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

    private fun showError(title: String, description: String) {
        _uiState.value = _uiState.value.copy(
            showErrorDialog = true,
            errorTitle = title,
            errorDescription = description,
            isLoading = false
        )
        analyticsTracker.trackEvent(
            AnalyticsEvents.ERROR_DIALOG_SHOWN,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                "title" to title
            )
        )
    }

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

    fun onError(error: WebViewError, failingUrl: String?) {
        handleWebViewErrorUseCase(error)
        val (title, description, type) = when (error) {
            is WebViewError.NetworkError -> Triple(
                "Offline",
                "No internet connection",
                "network_error"
            )

            is WebViewError.GenericError -> Triple("Error", error.description, "generic_error")
        }
        analyticsTracker.trackEvent(
            AnalyticsEvents.WEBVIEW_ERROR,
            mapOf(
                AnalyticsParams.SCREEN to "webview",
                AnalyticsParams.URL to (failingUrl ?: "unknown"),
                AnalyticsParams.ERROR_TYPE to type,
                AnalyticsParams.ERROR_MESSAGE to description
            )
        )
        showError(title, description)
    }

    private fun updateBackNavigationState() {
        val canGoBack = currentWebView?.let { webView ->
            (webViewRepository as? com.boa.saltoinicial.data.repository.WebViewRepositoryImpl)
                ?.canGoBack(webView) ?: false
        } ?: false
        _uiState.value = _uiState.value.copy(canGoBack = canGoBack)
    }
}
