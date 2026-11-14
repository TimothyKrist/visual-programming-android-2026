package com.example.todolistproject.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthFields(register: Boolean, enabled: Boolean, email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit, onSubmit: () -> Unit, onSwitch: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (register) "Register" else "Log in", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(email, onEmailChange, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = enabled, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, onPasswordChange, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = enabled, modifier = Modifier.fillMaxWidth())
        if (register) Text("Use at least 9 characters.")
        Button(onClick = onSubmit, enabled = enabled) { Text(if (register) "Register" else "Log in") }
        TextButton(onClick = onSwitch, enabled = enabled) { Text(if (register) "Already have an account? Log in" else "Need an account? Register") }
    }
}
