package com.boa.saltoinicial.presentation.state

import androidx.annotation.StringRes

/**
 * UI State for the main screen
 *
 * Los textos del diálogo de error viajan como IDs de recurso y no como `String` ya resuelto: así
 * se traducen según el idioma del dispositivo (ver `res/values` y `res/values-en`) y siguen siendo
 * correctos si el locale cambia mientras la app está abierta. El ViewModel no necesita `Context`.
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val showErrorDialog: Boolean = false,
    @param:StringRes val errorTitleRes: Int? = null,
    @param:StringRes val errorDescriptionRes: Int? = null,
    val canGoBack: Boolean = false
)

/**
 * UI Events for the main screen
 */
sealed class MainUiEvent {
    data object LoadWebsite : MainUiEvent()
    data object DismissErrorDialog : MainUiEvent()
    data object NavigateBack : MainUiEvent()
    data class ShowError(
        @param:StringRes val titleRes: Int,
        @param:StringRes val descriptionRes: Int
    ) : MainUiEvent()
}
