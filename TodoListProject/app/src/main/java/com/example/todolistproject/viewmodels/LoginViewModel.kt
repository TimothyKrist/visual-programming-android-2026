package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.repositories.AuthRepository

class LoginViewModel(private val repository: AuthRepository) : RequestViewModel() {
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
}
