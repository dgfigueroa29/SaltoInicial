package com.boa.saltoinicial.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.boa.saltoinicial.domain.repository.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Implementación de [NetworkMonitor] sobre [ConnectivityManager].
 *
 * Requiere el permiso `android.permission.ACCESS_NETWORK_STATE`, ya declarado en el manifest.
 *
 * Criterio: hay conexión cuando existe una red activa con [NetworkCapabilities.NET_CAPABILITY_INTERNET].
 * En modo avión (o sin ninguna red conectada) `activeNetwork` es `null` y el resultado es `false`.
 *
 * A propósito **no** se exige [NetworkCapabilities.NET_CAPABILITY_VALIDATED]: esa capacidad puede
 * tardar en confirmarse o quedar en `false` con VPNs y portales cautivos, y reportar "sin conexión"
 * a un dispositivo que sí tiene red es justo el falso positivo que este monitor evita.
 *
 * Ante cualquier duda el monitor falla hacia "online", porque el costo de un falso "sin conexión"
 * (un diálogo a pantalla completa sobre un sitio que carga bien) es mayor que el de omitirlo.
 *
 * @param connectivityManager Servicio del sistema, o `null` si no está disponible.
 */
class AndroidNetworkMonitor(
    private val connectivityManager: ConnectivityManager?
) : NetworkMonitor {

    constructor(context: Context) : this(
        ContextCompat.getSystemService(context.applicationContext, ConnectivityManager::class.java)
    )

    override fun isOnline(): Boolean {
        val manager = connectivityManager
        if (manager == null) {
            Timber.w("ConnectivityManager no disponible; se asume que hay conexión.")
            return true
        }
        return runCatching {
            val activeNetwork = manager.activeNetwork
            if (activeNetwork == null) {
                // Sin red activa: modo avión, o Wi-Fi y datos apagados.
                false
            } else {
                // Si el sistema no informa las capacidades no se puede afirmar que no haya red.
                val capabilities = manager.getNetworkCapabilities(activeNetwork)
                capabilities == null ||
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }.getOrElse { error ->
            Timber.w(error, "No se pudo consultar la conectividad; se asume que hay conexión.")
            true
        }
    }

    /**
     * Observa la red por defecto del sistema con un [ConnectivityManager.NetworkCallback].
     *
     * Emite el estado actual al suscribirse para que el consumidor no tenga que consultarlo por
     * separado, y después uno por cada cambio. Si el registro del callback falla, deja de emitir en
     * lugar de mentir sobre la conectividad: la consulta puntual de [isOnline] sigue disponible.
     */
    override fun observeOnline(): Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            Timber.w("ConnectivityManager no disponible; no se observan cambios de red.")
            send(true)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrentState()
            override fun onLost(network: Network) = emitCurrentState()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = emitCurrentState()

            private fun emitCurrentState() {
                trySend(isOnline())
            }
        }

        send(isOnline())
        val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }
            .onFailure { error ->
                Timber.w(error, "No se pudo observar la conectividad.")
                close()
            }
            .isSuccess

        awaitClose {
            if (registered) {
                runCatching { manager.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()
}
