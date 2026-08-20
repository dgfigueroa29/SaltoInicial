package com.boa.saltoinicial.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.boa.saltoinicial.data.repository.WebViewRepositoryImpl
import com.boa.saltoinicial.domain.repository.NetworkMonitor
import com.boa.saltoinicial.domain.usecase.HandleWebViewErrorUseCase
import com.boa.saltoinicial.domain.usecase.HideElementsUseCase
import com.boa.saltoinicial.domain.usecase.IsDeviceOfflineUseCase
import com.boa.saltoinicial.domain.usecase.LoadWebsiteUseCase
import com.boa.saltoinicial.domain.usecase.NavigateBackUseCase
import com.boa.saltoinicial.presentation.analytics.AnalyticsTracker

/**
 * Factory for creating MainViewModel with dependencies
 *
 * @param analyticsTracker Tracker al que el ViewModel envía los eventos.
 * @param networkMonitor Monitor de conectividad usado para decidir si un error de carga
 * corresponde a un dispositivo sin conexión.
 */
class MainViewModelFactory(
    private val analyticsTracker: AnalyticsTracker,
    private val networkMonitor: NetworkMonitor
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
            val isDeviceOfflineUseCase = IsDeviceOfflineUseCase(networkMonitor)

            return MainViewModel(
                loadWebsiteUseCase = loadWebsiteUseCase,
                handleWebViewErrorUseCase = handleWebViewErrorUseCase,
                navigateBackUseCase = navigateBackUseCase,
                hideElementsUseCase = hideElementsUseCase,
                isDeviceOfflineUseCase = isDeviceOfflineUseCase,
                analyticsTracker = analyticsTracker
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
