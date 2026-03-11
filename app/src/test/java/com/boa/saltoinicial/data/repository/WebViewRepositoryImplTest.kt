package com.boa.saltoinicial.data.repository

import android.webkit.WebView
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.models.WebViewState
import com.boa.utils.Common
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebViewRepositoryImplTest {

    private lateinit var repository: WebViewRepositoryImpl
    private lateinit var mockWebView: WebView

    @Before
    fun setup() {
        repository = WebViewRepositoryImpl()
        mockWebView = mockk()
    }

    @Test
    fun `getWebViewConfig returns correct config with Common WEB URL`() {
        // When
        val config = repository.getWebViewConfig()

        // Then
        assertEquals(Common.WEB, config.url)
        assertTrue(config.enableJavaScript)
        assertTrue(config.enableWideViewPort)
        assertEquals(System.getProperty("http.agent").orEmpty(), config.userAgent)
    }

    @Test
    fun `loadUrl calls webView loadUrl with correct URL`() {
        // Given
        val testUrl = "https://example.com"
        every { mockWebView.loadUrl(testUrl) } returns Unit

        // When
        repository.loadUrl(mockWebView, testUrl)

        // Then
        verify { mockWebView.loadUrl(testUrl) }
    }

    @Test
    fun `goBack calls webView goBack`() {
        // Given
        every { mockWebView.goBack() } returns Unit

        // When
        repository.goBack(mockWebView)

        // Then
        verify { mockWebView.goBack() }
    }

    @Test
    fun `canGoBack returns webView canGoBack result when webView exists`() {
        // Given
        every { mockWebView.canGoBack() } returns true

        // When
        val result = repository.canGoBack(mockWebView)

        // Then
        assertTrue(result)
        verify { mockWebView.canGoBack() }
    }

    @Test
    fun `getCurrentState returns current state`() {
        // When
        val state = repository.getCurrentState()

        // Then
        assertEquals(WebViewState(), state)
    }

    @Test
    fun `updateState changes current state`() {
        // Given
        val newState = WebViewState(isLoading = true, currentUrl = "https://example.com")

        // When
        repository.updateState(newState)
        val result = repository.getCurrentState()

        // Then
        assertEquals(newState, result)
    }

    @Test
    fun `handleError updates state with error`() {
        // Given
        val error = WebViewError.NetworkError("Connection failed")

        // When
        repository.handleError(error)
        val state = repository.getCurrentState()

        // Then
        assertEquals(error, state.error)
    }

    @Test
    fun `hideElements calls webView loadUrl with JavaScript to hide elements`() {
        // Given
        every { mockWebView.loadUrl(any()) } returns Unit

        // When
        repository.hideElements(mockWebView)

        // Then
        verify(exactly = 8) { mockWebView.loadUrl(any()) }
        // Verifies that 8 JavaScript calls are made to hide elements
    }
}
