package com.example.todolistproject.views

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.todolistproject.viewmodels.RequestViewModel

@Composable
fun RequestStatus(model: RequestViewModel, message: String? = null) {
    (model.error ?: message)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (model.loading) CircularProgressIndicator()
}
