package com.boa.saltoinicial

import android.app.Application
import android.os.StrictMode
import io.sentry.android.core.SentryAndroid
import timber.log.Timber

/**
 * Application class para SaltoInicial.
 *
 * Planta el árbol de Timber que corresponda al build ([Timber.DebugTree] en debug,
 * [CrashReportingTree] en release), inicializa StrictMode en debug para detectar accesos al
 * disco/red en el hilo principal, y configura Sentry para el monitoreo de errores en producción
 * si el DSN está disponible en [BuildConfig.SENTRY_DSN].
 */
class SaltoInicialApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber descarta hasta que se le planta un árbol, así que esto va primero: sin
        // esta llamada ningún log de la app llega a Logcat ni a Crashlytics.
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashReportingTree())

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
            // Sentry initialization may perform disk I/O on the main thread
            val oldPolicy = StrictMode.allowThreadDiskWrites()
            try {
                SentryAndroid.init(this) { options ->
                    options.dsn = dsn
                    options.isEnableAutoSessionTracking = true
                    options.isEnableNdk = true
                    options.tracesSampleRate = 1.0
                }
            } finally {
                StrictMode.setThreadPolicy(oldPolicy)
            }
            Timber.i("Sentry initialized")
        }
    }
}

