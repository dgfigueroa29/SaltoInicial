package com.boa.utils

import android.content.Context
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

/**
 * Created by Boa (davo.figueroa14@gmail.com) on 18 oct 2017.
 */
object Utils {
    /**
     * Registra forzadamente una Excepción en Crashlytics
     *
     * @param context       Contexto de app
     * @param referenceName Etiqueta para ubicar el error detectado desde el panel de Fabric
     * @param e             Excepción capturada
     */
    fun logError(context: Context?, referenceName: String, e: Exception?) {
        try {
            if (Common.DEBUG && e != null) {
                println("$referenceName $e")
                e.printStackTrace()
            } else {
                if (context != null) {
                    Fabric.with(context, Crashlytics())
                    Crashlytics.getInstance()
                    if (e != null) {
                        Crashlytics.logException(e)
                    } else {
                        Crashlytics.log(referenceName)
                    }
                }
            }
        } catch (ex: Exception) {
            println("Utils:logError - Exception: $ex")
            if (Common.DEBUG) {
                ex.printStackTrace()
            }
        }
    }
}