package com.boa.saltoinicial.presentation.ui

import android.net.Uri
import android.webkit.ValueCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FileChooserBridgeTest {

    private val bridge = FileChooserBridge()

    @Test
    fun `resolve hands the chosen files to the WebView`() {
        val callback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)
        val chosen = arrayOf(mockk<Uri>())

        bridge.await(callback)
        bridge.resolve(chosen)

        verify(exactly = 1) { callback.onReceiveValue(chosen) }
    }

    @Test
    fun `cancel answers null so the file input does not stay blocked`() {
        val callback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)

        bridge.await(callback)
        bridge.cancel()

        verify(exactly = 1) { callback.onReceiveValue(null) }
    }

    @Test
    fun `a second request cancels the pending one`() {
        val first = mockk<ValueCallback<Array<Uri>>>(relaxed = true)
        val second = mockk<ValueCallback<Array<Uri>>>(relaxed = true)

        bridge.await(first)
        bridge.await(second)
        bridge.resolve(null)

        // El WebView exige exactamente una respuesta por callback: el primero se cierra al ser
        // reemplazado y el segundo recibe el resultado real.
        verify(exactly = 1) { first.onReceiveValue(null) }
        verify(exactly = 1) { second.onReceiveValue(null) }
    }

    @Test
    fun `resolving twice answers the WebView only once`() {
        val callback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)

        bridge.await(callback)
        bridge.resolve(null)
        bridge.resolve(null)

        verify(exactly = 1) { callback.onReceiveValue(null) }
    }
}
