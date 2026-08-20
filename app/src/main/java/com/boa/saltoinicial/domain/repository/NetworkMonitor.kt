package com.boa.saltoinicial.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato para consultar el estado de conectividad del dispositivo.
 *
 * Se usa para distinguir un fallo de carga del sitio (HTTP 5xx, recursos rotos, errores de
 * JavaScript, DNS de terceros caído) de una falta real de conexión del dispositivo. Solo el
 * segundo caso justifica mostrar el diálogo de "sin conexión".
 */
interface NetworkMonitor {

    /**
     * Indica si el dispositivo tiene una red activa capaz de salir a internet.
     *
     * @return `true` si hay conexión (o si no se pudo determinar el estado), `false` si el
     * dispositivo está sin red —por ejemplo, en modo avión—.
     */
    fun isOnline(): Boolean

    /**
     * Emite el estado de conectividad: el actual al suscribirse y uno nuevo en cada cambio.
     *
     * Permite recargar el sitio solo cuando la red vuelve, en lugar de dejar al usuario frente a
     * un diálogo que no se actualiza hasta que reabre la app.
     */
    fun observeOnline(): Flow<Boolean>
}
