package com.boa.saltoinicial

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import timber.log.Timber

/**
 * Árbol de Timber para builds de release.
 *
 * Descarta `VERBOSE`, `DEBUG` e `INFO` —ruido que no aporta en producción— y manda el resto a
 * Crashlytics: los `WARN` como breadcrumbs que acompañan al próximo reporte, y las excepciones de
 * nivel `ERROR` como eventos no fatales.
 *
 * Sin este árbol (o sin `DebugTree` en debug) Timber descarta lo que recibe, que es lo que
 * pasaba hasta ahora: los logs de la app no llegaban a ningún lado.
 */
class CrashReportingTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = Firebase.crashlytics
        crashlytics.log(if (tag.isNullOrBlank()) message else "$tag: $message")
        if (t != null && priority >= Log.ERROR) {
            crashlytics.recordException(t)
        }
    }
}
