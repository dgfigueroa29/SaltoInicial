package com.boa.saltoinicial.presentation.analytics

import android.os.Bundle
import android.content.Context
import com.amplitude.android.Amplitude
import com.google.firebase.analytics.FirebaseAnalytics
import com.appsflyer.AppsFlyerLib
import com.facebook.appevents.AppEventsLogger

interface AnalyticsTracker {
    fun trackEvent(
        name: String,
        params: Map<String, Any?> = emptyMap()
    )
}

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

class MultiAnalyticsTracker(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val appsFlyer: AppsFlyerLib,
    private val amplitude: Amplitude?,
    private val facebookLogger: AppEventsLogger? = null
) : AnalyticsTracker {

    override fun trackEvent(name: String, params: Map<String, Any?>) {
        // Firebase
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putString(key, value.toString())
                    null -> Unit
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(name, bundle)

        // AppsFlyer
        val afParams = params.mapValues { it.value?.toString().orEmpty() }
        appsFlyer.logEvent(context, name, afParams)

        // Amplitude
        amplitude?.track(
            name,
            params.toMutableMap()
        )

        // Facebook
        facebookLogger?.logEvent(name, params["valueToSum"] as? Double ?: 0.0)
    }
}
