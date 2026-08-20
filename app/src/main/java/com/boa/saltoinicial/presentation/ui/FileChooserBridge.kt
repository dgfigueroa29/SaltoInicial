package com.boa.saltoinicial.presentation.ui

import android.net.Uri
import android.webkit.ValueCallback
import timber.log.Timber

/**
 * Conecta el selector de archivos del sistema con el WebView.
 *
 * El WebView entrega un [ValueCallback] al que hay que responder **exactamente una vez**: con los
 * archivos elegidos, o con `null` si el usuario canceló. Si nunca se responde, el `<input
 * type="file">` queda bloqueado y ningún intento posterior abre el selector.
 *
 * Existe como pieza aparte para romper el ciclo entre el `WebChromeClient` —que necesita lanzar el
 * selector— y el launcher de Compose —que necesita saber a quién entregarle el resultado—.
 */
class FileChooserBridge {

    private var pending: ValueCallback<Array<Uri>>? = null

    /** Registra el callback del WebView, cancelando cualquier selección anterior sin resolver. */
    fun await(callback: ValueCallback<Array<Uri>>) {
        pending?.let {
            Timber.w("Selección de archivos anterior sin resolver; se cancela.")
            it.onReceiveValue(null)
        }
        pending = callback
    }

    /** Entrega el resultado al WebView. [uris] en `null` significa que el usuario canceló. */
    fun resolve(uris: Array<Uri>?) {
        pending?.onReceiveValue(uris)
        pending = null
    }

    /** Cancela la selección pendiente, si la hay. */
    fun cancel() = resolve(null)
}
