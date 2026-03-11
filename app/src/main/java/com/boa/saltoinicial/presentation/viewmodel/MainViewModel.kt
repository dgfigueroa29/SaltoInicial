package com.boa.saltoinicial.presentation.viewmodel

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boa.saltoinicial.domain.models.WebViewError
import com.boa.saltoinicial.domain.repository.WebViewRepository
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
import com.boa.saltoinicial.domain.usecase.LoadWebsiteUseCase
import com.boa.saltoinicial.domain.usecase.NavigateBackUseCase
import com.boa.saltoinicial.presentation.state.MainUiEvent
import com.boa.saltoinicial.presentation.state.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen following MVI pattern
 */
class MainViewModel(
    private val webViewRepository: WebViewRepository,
    private val loadWebsiteUseCase: LoadWebsiteUseCase,
    private val handleWebViewErrorUseCase: HandleWebViewErrorUseCase,
    private val navigateBackUseCase: NavigateBackUseCase,
    private val hideElementsUseCase: HideElementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentWebView: WebView? = null

    fun setWebView(webView: WebView) {
        currentWebView = webView
        loadWebsite()
    }

    fun onEvent(event: MainUiEvent) {
        when (event) {
            MainUiEvent.LoadWebsite -> loadWebsite()
            MainUiEvent.DismissErrorDialog -> dismissErrorDialog()
            MainUiEvent.NavigateBack -> navigateBack()
            is MainUiEvent.ShowError -> showError(event.title, event.description)
        }
    }

    private fun loadWebsite() {
        currentWebView?.let { webView ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                loadWebsiteUseCase(webView)
            }
        }
    }

    private fun dismissErrorDialog() {
        _uiState.value = _uiState.value.copy(showErrorDialog = false)
    }

    private fun navigateBack() {
        currentWebView?.let { webView ->
            navigateBackUseCase(webView)
            updateBackNavigationState()
        }
    }

    private fun showError(title: String, description: String) {
        _uiState.value = _uiState.value.copy(
            showErrorDialog = true,
            errorTitle = title,
            errorDescription = description,
            isLoading = false
        )
    }

    fun onPageStarted() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        updateBackNavigationState()
    }

    fun onPageFinished() {
        _uiState.value = _uiState.value.copy(isLoading = false)
        currentWebView?.let { webView ->
            viewModelScope.launch {
                hideElementsUseCase(webView)
            }
        }
    }

    fun onError(error: WebViewError) {
        handleWebViewErrorUseCase(error)
        when (error) {
            is WebViewError.NetworkError -> showError("Offline", "No internet connection")
            is WebViewError.GenericError -> showError("Error", error.description)
        }
    }

    private fun updateBackNavigationState() {
        val canGoBack = currentWebView?.let { webView ->
            (webViewRepository as? com.boa.saltoinicial.data.repository.WebViewRepositoryImpl)
                ?.canGoBack(webView) ?: false
        } ?: false
        _uiState.value = _uiState.value.copy(canGoBack = canGoBack)
    }
}
