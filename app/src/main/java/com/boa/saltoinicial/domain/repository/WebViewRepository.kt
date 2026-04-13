@file:Suppress("SameReturnValue")

package com.boa.saltoinicial.domain.repository

import com.boa.saltoinicial.domain.models.WebViewConfig
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.models.WebViewState

/**
 * Repository interface for WebView operations
 */
@Suppress("EmptyMethod")
interface WebViewRepository {

    /**
     * Get the WebView configuration
     */
    fun getWebViewConfig(): WebViewConfig

    /**
     * Navigate back in WebView history
     */
    fun goBack()

    /**
     * Check if WebView can go back
     */
    fun canGoBack(): Boolean

    /**
     * Hide specific elements using JavaScript
     */
    fun hideElements()

    /**
     * Get current WebView state
     */
    fun getCurrentState(): WebViewState

    /**
     * Update WebView state
     */
    fun updateState(state: WebViewState)

    /**
     * Handle WebView error
     */
    fun handleError(error: WebViewError)
}
