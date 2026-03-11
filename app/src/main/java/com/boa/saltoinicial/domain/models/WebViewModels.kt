package com.boa.saltoinicial.domain.models

/**
 * Represents the current state of the WebView
 */
data class WebViewState(
    val isLoading: Boolean = false,
    val currentUrl: String? = null,
    val canGoBack: Boolean = false,
    val error: WebViewError? = null
)

/**
 * Represents different types of WebView errors
 */
sealed class WebViewError {
    data class NetworkError(val description: String) : WebViewError()
    data class GenericError(val description: String) : WebViewError()
}

/**
 * Configuration for WebView settings
 */
data class WebViewConfig(
    val url: String,
    val enableJavaScript: Boolean = true,
    val enableWideViewPort: Boolean = true,
    val userAgent: String = System.getProperty("http.agent").orEmpty()
)
