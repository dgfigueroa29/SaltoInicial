package com.boa.saltoinicial.presentation.ui

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import timber.log.Timber

/**
 * WebChromeClient de la app.
 *
 * Sin un [WebChromeClient] asignado el WebView ignora las APIs de UI del navegador: un
 * `<input type="file">` no abre nada y `alert()`, `confirm()` y `prompt()` son no-ops. Basta con
 * asignar uno para recuperar los diálogos de JavaScript; el selector de archivos, en cambio, hay
 * que implementarlo.
 *
 * No implementa `onShowCustomView`, así que el video embebido sigue sin poder ir a pantalla
 * completa. Eso necesita manipular la jerarquía de vistas de la Activity y verificarse en un
 * dispositivo real.
 *
 * @param fileChooser Puente que guarda el callback del WebView hasta que el usuario elige.
 * @param launchChooser Lanza el selector del sistema. Devuelve `false` si no se pudo abrir.
 */
class MainWebChromeClient(
    private val fileChooser: FileChooserBridge,
    private val launchChooser: (Intent) -> Boolean
) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null || fileChooserParams == null) {
            return false
        }
        fileChooser.await(filePathCallback)
        val launched = launchChooser(fileChooserParams.createIntent())
        if (!launched) {
            Timber.w("No se pudo abrir el selector de archivos del sistema.")
            fileChooser.cancel()
        }
        return launched
    }
}
