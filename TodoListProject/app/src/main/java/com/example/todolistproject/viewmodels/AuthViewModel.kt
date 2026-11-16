package com.example.todolistproject.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loginMessage: String? = null
)

class AuthViewModel(private val app: AppViewModel) : RequestViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun setEmail(value: String) { _uiState.update { it.copy(email = value) } }
    fun setPassword(value: String) { _uiState.update { it.copy(password = value) } }
    fun clearFields() { _uiState.update { it.copy(email = "", password = "") } }
    fun clearLoginMessage() { _uiState.update { it.copy(loginMessage = null) } }

    fun logout(expired: Boolean = false) {
        clearFields()
        clearError()
        _uiState.update {
            it.copy(loginMessage = if (expired) "Your session expired. Please log in again." else null)
        }
        app.clearSession()
    }

    fun login(onSuccess: () -> Unit) {
        clearLoginMessage()
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isEmpty()) {
            showError("Enter your email and password.")
            return
        }
        request {
            val token = app.authRepository.login(state.email, state.password).accessToken
            app.setSession(token)
            clearFields()
            onSuccess()
        }
    }

    fun register(onSuccess: () -> Unit) {
        clearLoginMessage()
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isEmpty()) {
            showError("Enter your email and password.")
            return
        }
        if (state.password.length < 9) {
            showError("Registration passwords must be at least 9 characters.")
            return
        }
        request {
            val token = app.authRepository.register(state.email, state.password).accessToken
            app.setSession(token)
            clearFields()
            onSuccess()
        }
    }
}
