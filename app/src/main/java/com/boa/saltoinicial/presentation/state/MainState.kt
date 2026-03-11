package com.boa.saltoinicial.presentation.state

/**
 * UI State for the main screen
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorTitle: String = "",
    val errorDescription: String = "",
    val canGoBack: Boolean = false
)

/**
 * UI Events for the main screen
 */
sealed class MainUiEvent {
    data object LoadWebsite : MainUiEvent()
    data object DismissErrorDialog : MainUiEvent()
    data object NavigateBack : MainUiEvent()
    data class ShowError(val title: String, val description: String) : MainUiEvent()
}
