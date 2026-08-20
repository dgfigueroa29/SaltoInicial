package com.boa.saltoinicial.domain.repository

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
}
