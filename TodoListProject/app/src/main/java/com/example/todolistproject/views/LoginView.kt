package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.example.todolistproject.viewmodels.AuthViewModel

@Composable
fun LoginView(model: AuthViewModel, onLogin: () -> Unit, onRegister: () -> Unit) {
    val loading by model.loading.collectAsStateWithLifecycle()
    val email by model.email.collectAsStateWithLifecycle()
    val password by model.password.collectAsStateWithLifecycle()
    val message by model.loginMessage.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler(enabled = loading) {}
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model, message)
        AuthFields(register = false, enabled = !loading, email = email, onEmailChange = model::setEmail,
            password = password, onPasswordChange = model::setPassword, onSubmit = onLogin, onSwitch = { model.clearFields(); onRegister() })
    }
}
