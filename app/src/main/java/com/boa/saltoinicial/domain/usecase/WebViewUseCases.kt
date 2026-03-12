package com.boa.saltoinicial.domain.usecase

import android.webkit.WebView
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.repository.WebViewRepository

/**
 * Use case for loading the website
 */
class LoadWebsiteUseCase(
    private val repository: WebViewRepository
) {
    operator fun invoke(webView: WebView) {
        val config = repository.getWebViewConfig()
        (repository as? com.boa.saltoinicial.data.repository.WebViewRepositoryImpl)?.loadUrl(
            webView,
            config.url
        )
    }
}

/**
 * Use case for handling WebView errors
 */
class HandleWebViewErrorUseCase(
    private val repository: WebViewRepository
) {
    operator fun invoke(error: WebViewError) {
        repository.handleError(error)
    }
}

/**
 * Use case for navigating back
 */
class NavigateBackUseCase(
    private val repository: WebViewRepository
) {
    operator fun invoke(webView: WebView) {
        (repository as? com.boa.saltoinicial.data.repository.WebViewRepositoryImpl)?.goBack(webView)
    }
}

/**
 * Use case for hiding elements
 */
class HideElementsUseCase(
    private val repository: WebViewRepository
) {
    operator fun invoke(webView: WebView) {
        (repository as? com.boa.saltoinicial.data.repository.WebViewRepositoryImpl)?.hideElements(
            webView
        )
    }
}
