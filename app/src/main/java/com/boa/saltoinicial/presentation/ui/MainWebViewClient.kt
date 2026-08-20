package com.boa.saltoinicial.presentation.ui

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.presentation.viewmodel.MainViewModel
import timber.log.Timber

/**
 * Custom WebViewClient that integrates with the ViewModel
 */
class MainWebViewClient(
    private val viewModel: MainViewModel
) : WebViewClient() {

    /**
     * Notifica al [MainViewModel] cuando falla la carga del documento principal.
     *
     * `onReceivedError` se dispara por **cada** recurso que falla (imágenes, CSS, píxeles de
     * tracking, publicidad bloqueada, hosts de terceros caídos), no solo por la página. Esos
     * fallos de subrecursos no impiden ver el sitio, así que solo se loguean y nunca llegan al
     * ViewModel: quien decide si corresponde mostrar el diálogo de "sin conexión" es
     * [MainViewModel.onError], y únicamente si el dispositivo está realmente sin red.
     */
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        val description = error?.description?.toString() ?: "Unknown network error"
        val failingUrl = request?.url?.toString()
        // Si no hay request no se puede saber de qué recurso viene: se trata como documento
        // principal, porque igual el ViewModel valida la conectividad antes de mostrar nada.
        val isForMainFrame = request?.isForMainFrame ?: true
        if (!isForMainFrame) {
            Timber.w("Fallo de subrecurso ignorado: %s (%s)", failingUrl, description)
            return
        }
        viewModel.onError(WebViewError.NetworkError(description), failingUrl)
    }

    /**
     * Registra los errores HTTP del sitio (4xx y 5xx) sin notificar al [MainViewModel].
     *
     * Un 500 del servidor significa que hay conexión, así que nunca debe abrir el diálogo de
     * "sin conexión". Se sobrescribe de forma explícita para dejar asentada esa decisión.
     */
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        Timber.w(
            "Error HTTP %d en %s; hay conexión, no se muestra el diálogo de sin conexión.",
            errorResponse?.statusCode ?: 0,
            request?.url?.toString().orEmpty()
        )
    }

    /** Notifica al [MainViewModel] que el WebView comenzó a cargar una página. */
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        viewModel.onPageStarted(url)
    }

    /** Notifica al [MainViewModel] que el WebView terminó de cargar una página. */
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        viewModel.onPageFinished(url)
    }
}
