@file:Suppress("GrazieInspection")

package com.boa.saltoinicial.presentation.analytics

import android.content.Context
import android.os.Bundle
import com.amplitude.core.Amplitude
import com.appsflyer.AppsFlyerLib
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

/**
 * Contrato para el registro de eventos de analítica.
 *
 * Permite desacoplar la lógica de negocio de implementaciones concretas de SDK
 * (Firebase, AppsFlyer, Amplitude). Ver [MultiAnalyticsTracker] para la implementación
 * que envía a múltiples proveedores simultáneamente.
 */
interface AnalyticsTracker {
    /**
     * Registra un evento con nombre y parámetros opcionales.
     *
     * @param name Nombre del evento. Usar las constantes de [AnalyticsEvents].
     * @param params Mapa de parámetros clave-valor. Usar las constantes de [AnalyticsParams].
     */
    fun trackEvent(
        name: String,
        params: Map<String, Any?> = emptyMap()
    )
}

/** Nombres de eventos de analítica registrados en la app. */
object AnalyticsEvents {
    const val APP_OPEN = "app_open"
    const val SCREEN_VIEW = "screen_view"
    const val WEBVIEW_LOAD_START = "webview_load_start"
    const val WEBVIEW_LOAD_COMPLETE = "webview_load_complete"
    const val WEBVIEW_ERROR = "webview_error"
    const val ERROR_DIALOG_SHOWN = "error_dialog_shown"
    const val ERROR_DIALOG_DISMISSED = "error_dialog_dismissed"
    const val NAVIGATION_BACK = "navigation_back"
}

/** Claves de parámetros para los eventos de analítica. */
object AnalyticsParams {
    const val SCREEN = "screen"
    const val URL = "url"
    const val SOURCE = "source"
    const val ERROR_TYPE = "error_type"
    const val ERROR_MESSAGE = "error_message"
    const val ACTION = "action"
    const val CAN_GO_BACK = "can_go_back"
    const val PLATFORM = "platform"
}

/**
 * Implementación de [AnalyticsTracker] que envía eventos a múltiples proveedores en paralelo:
 * Firebase Analytics, AppsFlyer, Amplitude (opcional) y Mixpanel (opcional).
 *
 * @param context Contexto de la aplicación requerido por AppsFlyer.
 * @param firebaseAnalytics Instancia de Firebase Analytics.
 * @param appsFlyer Instancia de AppsFlyer SDK.
 * @param amplitude Instancia de Amplitude. Si es `null`, no se envían eventos a Amplitude.
 * @param mixpanel Instancia de Mixpanel. Si es `null`, no se envían eventos a Mixpanel.
 */
class MultiAnalyticsTracker(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val appsFlyer: AppsFlyerLib,
    private val amplitude: Amplitude?,
    private val facebookLogger: AppEventsLogger? = null,
    private val mixpanel: MixpanelAPI? = null
) : AnalyticsTracker {

    override fun trackEvent(name: String, params: Map<String, Any?>) {
        // Firebase
        firebaseAnalytics.logEvent(name, params.toBundle())

        // AppsFlyer
        val afParams = params.mapValues { it.value?.toString().orEmpty() }
        appsFlyer.logEvent(context, name, afParams)

        // Amplitude
        amplitude?.track(
            eventType = name,
            eventProperties = params.toMutableMap()
        )

        // Facebook
        facebookLogger?.logEvent(
            eventName = name,
            valueToSum = params["valueToSum"] as? Double ?: 0.0,
            parameters = params.toBundle()
        )

        // Mixpanel
        mixpanel?.track(name, params.toJSONObject())
    }
}

private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
    forEach { (key, value) ->
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Double -> putDouble(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            null -> Unit
            else -> putString(key, value.toString())
        }
    }
}

private fun Map<String, Any?>.toJSONObject(): JSONObject = JSONObject().apply {
    forEach { (key, value) ->
        when (value) {
            null -> Unit
            is String, is Int, is Long, is Double, is Boolean -> put(key, value)
            is Float -> put(key, value.toDouble())
            else -> put(key, value.toString())
        }
    }
}
