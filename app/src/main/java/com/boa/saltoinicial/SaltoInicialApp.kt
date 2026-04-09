package com.boa.saltoinicial

import android.app.Application
import android.os.StrictMode
import io.sentry.android.core.SentryAndroid
import timber.log.Timber

/**
 * Application class para SaltoInicial.
 *
 * Inicializa StrictMode en debug para detectar accesos al disco/red en el hilo principal,
 * y configura Sentry para el monitoreo de errores en producción si el DSN está disponible
 * en [BuildConfig.SENTRY_DSN].
 */
class SaltoInicialApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )

            Timber.d("StrictMode initialized in debug build")
        }

        val dsn = BuildConfig.SENTRY_DSN

        if (dsn.isNotBlank()) {
            SentryAndroid.init(this) { options ->
                options.dsn = dsn
                options.isEnableAutoSessionTracking = true
                options.isEnableNdk = true
                options.tracesSampleRate = 1.0
            }
            Timber.i("Sentry initialized")
        }
    }
}

