package com.boa.saltoinicial.domain.usecase

import com.boa.saltoinicial.domain.repository.NetworkMonitor

/**
 * Use case that reports whether the device is currently without connectivity.
 *
 * Es la única condición que habilita el diálogo de "sin conexión": un error del sitio
 * (HTTP 5xx, recurso roto, JavaScript con errores) no debe mostrarlo.
 */
class IsDeviceOfflineUseCase(
    private val networkMonitor: NetworkMonitor
) {
    operator fun invoke(): Boolean = !networkMonitor.isOnline()
}
