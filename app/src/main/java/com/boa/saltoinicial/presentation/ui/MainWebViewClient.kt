package com.boa.saltoinicial.presentation.ui

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.presentation.viewmodel.MainViewModel

/**
 * Custom WebViewClient that integrates with the ViewModel
 */
class MainWebViewClient(
    private val viewModel: MainViewModel
) : WebViewClient() {

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val webViewError = WebViewError.NetworkError(
            error?.description?.toString() ?: "Unknown network error"
        )
        viewModel.onError(webViewError)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        viewModel.onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        viewModel.onPageFinished()
    }
}
