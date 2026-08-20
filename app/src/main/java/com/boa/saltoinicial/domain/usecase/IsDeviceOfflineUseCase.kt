package com.boa.saltoinicial.domain.usecase

import com.boa.saltoinicial.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case that reports whether the device is currently without connectivity.
 *
 * Es la única condición que habilita el diálogo de "sin conexión": un error del sitio
 * (HTTP 5xx, recurso roto, JavaScript con errores) no debe mostrarlo.
 */
class IsDeviceOfflineUseCase(
    private val networkMonitor: NetworkMonitor
) {
    /** Estado de conectividad en este instante. */
    operator fun invoke(): Boolean = !networkMonitor.isOnline()

    /** Emite `true` cuando el dispositivo queda sin red y `false` cuando la recupera. */
    fun observe(): Flow<Boolean> = networkMonitor.observeOnline().map { online -> !online }
}
