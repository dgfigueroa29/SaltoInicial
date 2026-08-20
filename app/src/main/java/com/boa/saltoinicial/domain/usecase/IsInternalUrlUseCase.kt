package com.boa.saltoinicial.domain.usecase

import com.boa.utils.Common
import java.net.URI

/**
 * Decide si una URL pertenece al sitio que la app envuelve.
 *
 * Solo lo interno se abre dentro del WebView. El resto —otro dominio, o esquemas como `mailto:`,
 * `tel:` y `whatsapp:`, que el WebView no sabe cargar— sale a la app del sistema que corresponda.
 *
 * Se considera interno el host del sitio y cualquier subdominio suyo, ignorando el `www.` y las
 * mayúsculas. Una URL malformada o sin host se trata como externa.
 *
 * @param siteUrl URL del sitio envuelto. Por defecto [Common.WEB].
 */
class IsInternalUrlUseCase(siteUrl: String = Common.WEB) {

    private val siteHost: String? = hostOf(siteUrl)

    operator fun invoke(url: String): Boolean {
        val parsed = runCatching { URI(url) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        val host = parsed?.host?.lowercase()?.removePrefix("www.")
        val site = siteHost
        return site != null &&
            host != null &&
            (scheme == "http" || scheme == "https") &&
            (host == site || host.endsWith(".$site"))
    }

    private fun hostOf(url: String): String? =
        runCatching { URI(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()
}
