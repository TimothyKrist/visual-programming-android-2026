package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.repositories.AuthRepository

class AuthViewModel(private val repository: AuthRepository) : RequestViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")

    fun clearFields() { email = ""; password = "" }

    fun login(onSuccess: (String) -> Unit) {
        if (email.isBlank() || password.isEmpty()) {
            showError("Enter your email and password.")
            return
        }
        request {
            val token = repository.login(email, password).accessToken
            clearFields()
            onSuccess(token)
        }
    }

    fun register(onSuccess: (String) -> Unit) {
        if (email.isBlank() || password.isEmpty()) {
            showError("Enter your email and password.")
            return
        }
        if (password.length < 9) {
            showError("Registration passwords must be at least 9 characters.")
            return
        }
        request {
            val token = repository.register(email, password).accessToken
            clearFields()
            onSuccess(token)
        }
    }
}
