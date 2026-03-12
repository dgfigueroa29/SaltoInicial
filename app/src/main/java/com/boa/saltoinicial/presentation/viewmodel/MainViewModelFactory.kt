package com.boa.saltoinicial.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.boa.saltoinicial.data.repository.WebViewRepositoryImpl
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
import com.boa.saltoinicial.domain.usecase.LoadWebsiteUseCase
import com.boa.saltoinicial.domain.usecase.NavigateBackUseCase
import com.boa.saltoinicial.presentation.analytics.AnalyticsTracker

/**
 * Factory for creating MainViewModel with dependencies
 */
class MainViewModelFactory(
    private val analyticsTracker: AnalyticsTracker
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            // Create dependencies
            val repository = WebViewRepositoryImpl()
            val loadWebsiteUseCase = LoadWebsiteUseCase(repository)
            val handleWebViewErrorUseCase = HandleWebViewErrorUseCase(repository)
            val navigateBackUseCase = NavigateBackUseCase(repository)
            val hideElementsUseCase = HideElementsUseCase(repository)

            return MainViewModel(
                webViewRepository = repository,
                loadWebsiteUseCase = loadWebsiteUseCase,
                handleWebViewErrorUseCase = handleWebViewErrorUseCase,
                navigateBackUseCase = navigateBackUseCase,
                hideElementsUseCase = hideElementsUseCase,
                analyticsTracker = analyticsTracker
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
