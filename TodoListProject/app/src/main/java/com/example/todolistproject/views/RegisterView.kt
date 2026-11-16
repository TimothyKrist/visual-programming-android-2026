package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.example.todolistproject.viewmodels.AuthViewModel

@Composable
fun RegisterView(model: AuthViewModel, onRegister: () -> Unit, onLogin: () -> Unit) {
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler { if (!model.loading) { model.clearFields(); onLogin() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        AuthFields(register = true, enabled = !model.loading, email = model.email, onEmailChange = { model.email = it },
            password = model.password, onPasswordChange = { model.password = it }, onSubmit = onRegister, onSwitch = { model.clearFields(); onLogin() })
    }
}
