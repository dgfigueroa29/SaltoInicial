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
 *
 * @property description Motivo real informado por el WebView. Es el texto que se registra en
 * analítica; el diálogo de "sin conexión" usa un mensaje propio.
 */
sealed class WebViewError {
    abstract val description: String

    data class NetworkError(override val description: String) : WebViewError()
    data class GenericError(override val description: String) : WebViewError()
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
