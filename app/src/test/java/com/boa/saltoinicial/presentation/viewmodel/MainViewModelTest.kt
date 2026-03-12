package com.boa.saltoinicial.presentation.viewmodel

import android.webkit.WebView
import app.cash.turbine.test
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.repository.WebViewRepository
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
import com.boa.saltoinicial.domain.usecase.LoadWebsiteUseCase
import com.boa.saltoinicial.domain.usecase.NavigateBackUseCase
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
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var mockRepository: WebViewRepository
    private lateinit var mockLoadWebsiteUseCase: LoadWebsiteUseCase
    private lateinit var mockHandleWebViewErrorUseCase: HandleWebViewErrorUseCase
    private lateinit var mockNavigateBackUseCase: NavigateBackUseCase
    private lateinit var mockHideElementsUseCase: HideElementsUseCase
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

        mockRepository = mockk()
        mockLoadWebsiteUseCase = mockk()
        mockHandleWebViewErrorUseCase = mockk()
        mockNavigateBackUseCase = mockk()
        mockHideElementsUseCase = mockk()
        mockAnalyticsTracker = mockk(relaxed = true)
        mockWebView = mockk(relaxed = true)

        // Configure webView mock to return values for canGoBack()
        every { mockWebView.canGoBack() } returns false

        every { mockLoadWebsiteUseCase(any()) } returns Unit
        every { mockHandleWebViewErrorUseCase(any()) } returns Unit
        every { mockNavigateBackUseCase(any()) } returns Unit
        every { mockHideElementsUseCase(any()) } returns Unit

        viewModel = MainViewModel(
            webViewRepository = mockRepository,
            loadWebsiteUseCase = mockLoadWebsiteUseCase,
            handleWebViewErrorUseCase = mockHandleWebViewErrorUseCase,
            navigateBackUseCase = mockNavigateBackUseCase,
            hideElementsUseCase = mockHideElementsUseCase,
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
        viewModel.onEvent(MainUiEvent.ShowError("Test Title", "Test Description"))
    }

    @Test
    fun `onPageStarted does not throw exception`() = runTest {
        // When & Then - Should not throw any exception
        viewModel.onPageStarted("https://example.com")
    }
}
