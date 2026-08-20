package com.boa.saltoinicial.presentation.viewmodel

import android.webkit.WebView
import app.cash.turbine.test
import com.boa.saltoinicial.R
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
import com.boa.saltoinicial.domain.usecase.IsDeviceOfflineUseCase
import com.boa.saltoinicial.domain.usecase.LoadWebsiteUseCase
import com.boa.saltoinicial.domain.usecase.NavigateBackUseCase
import com.boa.saltoinicial.presentation.analytics.AnalyticsEvents
import com.boa.saltoinicial.presentation.analytics.AnalyticsParams
import com.boa.saltoinicial.presentation.analytics.AnalyticsTracker
import com.boa.saltoinicial.presentation.state.MainUiEvent
import com.boa.saltoinicial.presentation.state.MainUiState
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var mockLoadWebsiteUseCase: LoadWebsiteUseCase
    private lateinit var mockHandleWebViewErrorUseCase: HandleWebViewErrorUseCase
    private lateinit var mockNavigateBackUseCase: NavigateBackUseCase
    private lateinit var mockHideElementsUseCase: HideElementsUseCase
    private lateinit var mockIsDeviceOfflineUseCase: IsDeviceOfflineUseCase
    private lateinit var mockAnalyticsTracker: AnalyticsTracker
    private lateinit var mockWebView: WebView
    private lateinit var viewModel: MainViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock FirebasePerformance static method
        mockkStatic(FirebasePerformance::class)
        val mockFirebasePerformance = mockk<FirebasePerformance>(relaxed = true)
        val mockTrace = mockk<Trace>(relaxed = true)
        every { FirebasePerformance.getInstance() } returns mockFirebasePerformance
        every { mockFirebasePerformance.newTrace(any()) } returns mockTrace

        mockLoadWebsiteUseCase = mockk()
        mockHandleWebViewErrorUseCase = mockk()
        mockNavigateBackUseCase = mockk()
        mockHideElementsUseCase = mockk()
        mockIsDeviceOfflineUseCase = mockk()
        mockAnalyticsTracker = mockk(relaxed = true)
        mockWebView = mockk(relaxed = true)

        // Configure webView mock to return values for canGoBack()
        every { mockWebView.canGoBack() } returns false

        every { mockLoadWebsiteUseCase(any()) } returns Unit
        every { mockHandleWebViewErrorUseCase(any()) } returns Unit
        every { mockNavigateBackUseCase(any()) } returns Unit
        every { mockHideElementsUseCase(any()) } returns Unit
        // Por defecto el dispositivo tiene conexión: el diálogo de offline no debe aparecer.
        every { mockIsDeviceOfflineUseCase() } returns false

        viewModel = MainViewModel(
            loadWebsiteUseCase = mockLoadWebsiteUseCase,
            handleWebViewErrorUseCase = mockHandleWebViewErrorUseCase,
            navigateBackUseCase = mockNavigateBackUseCase,
            hideElementsUseCase = mockHideElementsUseCase,
            isDeviceOfflineUseCase = mockIsDeviceOfflineUseCase,
            analyticsTracker = mockAnalyticsTracker
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is correct`() = runTest {
        // Given
        val expectedInitialState = MainUiState()

        // When & Then
        viewModel.uiState.test {
            assertEquals(expectedInitialState, awaitItem())
        }
    }

    @Test
    fun `setWebView can be called without throwing exceptions`() = runTest {
        // When & Then - Should not throw any exception
        viewModel.setWebView(mockWebView)
    }

    @Test
    fun `onEvent LoadWebsite can be called without throwing exceptions when webView is set`() =
        runTest {
            // Given
            viewModel.setWebView(mockWebView)

            // When & Then - Should not throw any exception
            viewModel.onEvent(MainUiEvent.LoadWebsite)
        }

    @Test
    fun `onEvent NavigateBack can be called without throwing exceptions when webView is set`() =
        runTest {
            // Given
            viewModel.setWebView(mockWebView)

            // When & Then - Should not throw any exception
            viewModel.onEvent(MainUiEvent.NavigateBack)
        }

    @Test
    fun `onPageFinished can be called without throwing exceptions when webView is set`() = runTest {
        // Given
        viewModel.setWebView(mockWebView)

        // When & Then - Should not throw any exception
        viewModel.onPageFinished("https://example.com")
    }

    @Test
    fun `onError calls handleWebViewError use case with correct error`() = runTest {
        // Given
        val error = WebViewError.NetworkError("Connection failed")

        // When
        viewModel.onError(error, "https://example.com")

        // Then
        verify { mockHandleWebViewErrorUseCase(error) }
    }

    @Test
    fun `onEvent DismissErrorDialog does not throw exception`() = runTest {
        // When & Then - Should not throw any exception
        viewModel.onEvent(MainUiEvent.DismissErrorDialog)
    }

    @Test
    fun `onEvent ShowError does not throw exception`() = runTest {
        // When & Then - Should not throw any exception
        viewModel.onEvent(MainUiEvent.ShowError(R.string.offline, R.string.offline_desc))
    }

    @Test
    fun `onPageStarted does not throw exception`() = runTest {
        // When & Then - Should not throw any exception
        viewModel.onPageStarted("https://example.com")
    }

    @Test
    fun `onError shows the offline dialog when the device has no connection`() = runTest {
        // Given - el dispositivo está sin red (modo avión o sin datos)
        every { mockIsDeviceOfflineUseCase() } returns true

        // When
        viewModel.onError(WebViewError.NetworkError("net::ERR_INTERNET_DISCONNECTED"), null)

        // Then - textos como recursos, para que se traduzcan según el idioma del dispositivo
        val state = viewModel.uiState.value
        assertTrue(state.showErrorDialog)
        assertEquals(R.string.offline, state.errorTitleRes)
        assertEquals(R.string.offline_desc, state.errorDescriptionRes)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onError does not show the offline dialog when the device has connection`() = runTest {
        // Given - el sitio falla (500, recurso roto, JS con errores) pero hay conexión
        every { mockIsDeviceOfflineUseCase() } returns false

        // When
        viewModel.onError(WebViewError.NetworkError("net::ERR_FAILED"), "https://example.com")

        // Then
        assertFalse(viewModel.uiState.value.showErrorDialog)
    }

    @Test
    fun `onError stops the loading dialog when the error is suppressed`() = runTest {
        // Given - una carga en curso que termina fallando con conexión disponible
        every { mockIsDeviceOfflineUseCase() } returns false
        viewModel.onPageStarted("https://example.com")
        assertTrue(viewModel.uiState.value.isLoading)

        // When
        viewModel.onError(WebViewError.NetworkError("net::ERR_FAILED"), "https://example.com")

        // Then - sin diálogo de error, pero tampoco el de carga girando para siempre
        val state = viewModel.uiState.value
        assertFalse(state.showErrorDialog)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onError keeps reporting the error to analytics when there is connection`() = runTest {
        // Given
        every { mockIsDeviceOfflineUseCase() } returns false

        // When
        viewModel.onError(WebViewError.NetworkError("net::ERR_FAILED"), "https://example.com")

        // Then - el error del sitio se sigue registrando aunque no se muestre nada al usuario
        verify {
            mockAnalyticsTracker.trackEvent(
                AnalyticsEvents.WEBVIEW_ERROR,
                match { params ->
                    params[AnalyticsParams.URL] == "https://example.com" &&
                        params[AnalyticsParams.ERROR_MESSAGE] == "net::ERR_FAILED" &&
                        params[AnalyticsParams.IS_OFFLINE] == false
                }
            )
        }
        verify(exactly = 0) {
            mockAnalyticsTracker.trackEvent(AnalyticsEvents.ERROR_DIALOG_SHOWN, any())
        }
    }

    @Test
    fun `onEvent ShowError still opens the dialog regardless of connectivity`() = runTest {
        // Given - el diálogo pedido explícitamente por la UI no depende de la conectividad
        every { mockIsDeviceOfflineUseCase() } returns false

        // When
        viewModel.onEvent(MainUiEvent.ShowError(R.string.app_name, R.string.please_wait))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showErrorDialog)
        assertEquals(R.string.app_name, state.errorTitleRes)
        assertEquals(R.string.please_wait, state.errorDescriptionRes)
    }

    @Test
    fun `showing the offline dialog does not send translated copy to analytics`() = runTest {
        // Given
        every { mockIsDeviceOfflineUseCase() } returns true

        // When
        viewModel.onError(WebViewError.NetworkError("net::ERR_INTERNET_DISCONNECTED"), null)

        // Then - un identificador estable, no el título traducido
        verify {
            mockAnalyticsTracker.trackEvent(
                AnalyticsEvents.ERROR_DIALOG_SHOWN,
                match { params -> params[AnalyticsParams.ERROR_TYPE] == "offline" }
            )
        }
    }
}
