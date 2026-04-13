@file:Suppress("USELESS_IS_CHECK")

package com.boa.saltoinicial.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewModelsTest {

    @Test
    fun `WebViewState default values are correct`() {
        val state = WebViewState()

        assertEquals(false, state.isLoading)
        assertEquals(null, state.currentUrl)
        assertEquals(false, state.canGoBack)
        assertEquals(null, state.error)
    }

    @Test
    fun `WebViewState copy works correctly`() {
        val originalState = WebViewState()
        val newState = originalState.copy(
            isLoading = true,
            currentUrl = "https://example.com",
            canGoBack = true
        )

        assertEquals(true, newState.isLoading)
        assertEquals("https://example.com", newState.currentUrl)
        assertEquals(true, newState.canGoBack)
        assertEquals(null, newState.error)
    }

    @Test
    fun `WebViewConfig default values are correct`() {
        val config = WebViewConfig(url = "https://example.com")

        assertEquals("https://example.com", config.url)
        assertEquals(true, config.enableJavaScript)
        assertEquals(true, config.enableWideViewPort)
        assertEquals(System.getProperty("http.agent").orEmpty(), config.userAgent)
    }

    @Test
    fun `WebViewError NetworkError contains correct message`() {
        val error = WebViewError.NetworkError("Connection failed")

        assert(error is WebViewError.NetworkError)
        if (error is WebViewError.NetworkError) {
            assertEquals("Connection failed", error.description)
        }
    }

    @Test
    fun `WebViewError GenericError contains correct message`() {
        val error = WebViewError.GenericError("Something went wrong")

        assert(error is WebViewError.GenericError)
        if (error is WebViewError.GenericError) {
            assertEquals("Something went wrong", error.description)
        }
    }
}
