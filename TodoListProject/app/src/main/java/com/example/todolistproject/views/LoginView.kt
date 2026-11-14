package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.example.todolistproject.viewmodels.LoginViewModel

@Composable
fun LoginView(model: LoginViewModel, message: String?, onLogin: () -> Unit, onRegister: () -> Unit) {
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler(enabled = model.loading) {}
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model, message)
        AuthFields(register = false, enabled = !model.loading, email = model.email, onEmailChange = { model.email = it },
            password = model.password, onPasswordChange = { model.password = it }, onSubmit = onLogin, onSwitch = { model.clearFields(); onRegister() })
    }
}
