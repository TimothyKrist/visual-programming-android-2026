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
    val state by model.uiState.collectAsStateWithLifecycle()
    val requestState by model.requestState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler(enabled = requestState.loading) {}
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model, state.loginMessage)
        AuthFields(register = false, enabled = !requestState.loading, email = state.email, onEmailChange = model::setEmail,
            password = state.password, onPasswordChange = model::setPassword, onSubmit = onLogin, onSwitch = { model.clearFields(); onRegister() })
    }
}
