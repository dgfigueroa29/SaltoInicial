package com.boa.saltoinicial.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.usecase.IsInternalUrlUseCase
import com.boa.saltoinicial.presentation.viewmodel.MainViewModel
import timber.log.Timber

/**
 * Custom WebViewClient that integrates with the ViewModel
 *
 * @param viewModel ViewModel al que se delegan los eventos de carga.
 * @param isInternalUrl Política que decide qué URLs se abren dentro del WebView.
 */
class MainWebViewClient(
    private val viewModel: MainViewModel,
    private val isInternalUrl: IsInternalUrlUseCase = IsInternalUrlUseCase()
) : WebViewClient() {

    /**
     * Mantiene dentro del WebView solo lo que pertenece al sitio envuelto.
     *
     * Con un [WebViewClient] asignado, el WebView intenta cargar él mismo **cualquier** esquema,
     * así que un `mailto:`, un `tel:` o un botón de compartir terminaban en
     * `ERR_UNKNOWN_URL_SCHEME` sin abrir nada. Ahora esos enlaces, y los de otros dominios, se
     * derivan a la app del sistema que corresponda: además de funcionar, evita que un sitio de
     * terceros se muestre dentro de la app sin barra de direcciones.
     */
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString().orEmpty()
        val context = view?.context
        return when {
            url.isEmpty() || isInternalUrl(url) -> false
            context == null -> false
            else -> openExternally(context, url)
        }
    }

    /**
     * Abre [url] con la app del sistema que la maneje.
     *
     * Devuelve siempre `true`: si no hay ninguna app capaz de abrirla, el WebView tampoco puede,
     * y dejarlo intentar solo produciría una página de error.
     */
    private fun openExternally(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            Timber.i("Enlace externo derivado al sistema: %s", url)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "Ninguna app instalada puede abrir %s", url)
        }
        return true
    }

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
