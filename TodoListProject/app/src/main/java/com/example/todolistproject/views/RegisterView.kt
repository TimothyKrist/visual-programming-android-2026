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
fun RegisterView(model: AuthViewModel, onRegister: () -> Unit, onLogin: () -> Unit) {
    val loading by model.loading.collectAsStateWithLifecycle()
    val email by model.email.collectAsStateWithLifecycle()
    val password by model.password.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler { if (!loading) { model.clearFields(); onLogin() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        AuthFields(register = true, enabled = !loading, email = email, onEmailChange = model::setEmail,
            password = password, onPasswordChange = model::setPassword, onSubmit = onRegister, onSwitch = { model.clearFields(); onLogin() })
    }
}
