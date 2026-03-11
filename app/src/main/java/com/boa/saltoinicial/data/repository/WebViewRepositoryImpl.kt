package com.boa.saltoinicial.data.repository

import android.webkit.WebView
import com.boa.saltoinicial.domain.models.WebViewConfig
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.models.WebViewState
import com.boa.saltoinicial.domain.repository.WebViewRepository
import com.boa.utils.Common

/**
 * Implementation of WebViewRepository
 */
class WebViewRepositoryImpl : WebViewRepository {

    private var currentState = WebViewState()

    override fun getWebViewConfig(): WebViewConfig {
        return WebViewConfig(
            url = Common.WEB,
            enableJavaScript = true,
            enableWideViewPort = true
        )
    }

    override fun goBack() {
        // This method is now handled by the ViewModel directly
        // as it needs access to the WebView instance
    }

    override fun canGoBack(): Boolean {
        // This method is now handled by the ViewModel directly
        // as it needs access to the WebView instance
        return false
    }

    override fun hideElements() {
        // This method is now handled by the ViewModel directly
        // as it needs access to the WebView instance
    }

    override fun getCurrentState(): WebViewState {
        return currentState
    }

    override fun updateState(state: WebViewState) {
        currentState = state
    }

    override fun handleError(error: WebViewError) {
        updateState(currentState.copy(error = error))
    }

    // Helper methods that can be called with WebView instance
    fun loadUrl(webView: WebView, url: String) {
        webView.loadUrl(url)
    }

    fun goBack(webView: WebView) {
        webView.goBack()
    }

    fun canGoBack(webView: WebView): Boolean {
        return webView.canGoBack()
    }

    fun hideElements(webView: WebView) {
        removeElement(webView)
    }

    private fun removeElement(webView: WebView) {
        // hide element by id
        webView.loadUrl("javascript:(function() { document.getElementById('blog-pager').style.display='none';})()")

        // we can also hide class name
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[0].style.display='none';})()")
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[1].style.display='none';})()")
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[2].style.display='none';})()")
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[3].style.display='none';})()")
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[4].style.display='none';})()")
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[5].style.display='none';})()")
        webView.loadUrl("javascript:(function() { document.getElementsByClassName('btn')[6].style.display='none';})()")
    }
}
