package org.openjwc.client.viewmodels

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowSnackBar(val message: String) : UiEvent()
}

sealed class NavEvent {
    class ToLogin() : NavEvent()
    class ToRegister() : NavEvent()
    class ToBack() : NavEvent()
}
