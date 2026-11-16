package com.example.todolistproject.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(private val app: AppViewModel) : RequestViewModel() {
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()
    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()
    private val _loginMessage = MutableStateFlow<String?>(null)
    val loginMessage = _loginMessage.asStateFlow()

    fun setEmail(value: String) { _email.value = value }
    fun setPassword(value: String) { _password.value = value }
    fun clearFields() { _email.value = ""; _password.value = "" }
    fun clearLoginMessage() { _loginMessage.value = null }

    fun logout(expired: Boolean = false) {
        clearFields()
        clearError()
        _loginMessage.value = if (expired) "Your session expired. Please log in again." else null
        app.clearSession()
    }

    fun login(onSuccess: () -> Unit) {
        clearLoginMessage()
        if (_email.value.isBlank() || _password.value.isEmpty()) {
            showError("Enter your email and password.")
            return
        }
        request {
            val token = app.authRepository.login(_email.value, _password.value).accessToken
            app.setSession(token)
            clearFields()
            onSuccess()
        }
    }

    fun register(onSuccess: () -> Unit) {
        clearLoginMessage()
        if (_email.value.isBlank() || _password.value.isEmpty()) {
            showError("Enter your email and password.")
            return
        }
        if (_password.value.length < 9) {
            showError("Registration passwords must be at least 9 characters.")
            return
        }
        request {
            val token = app.authRepository.register(_email.value, _password.value).accessToken
            app.setSession(token)
            clearFields()
            onSuccess()
        }
    }
}
