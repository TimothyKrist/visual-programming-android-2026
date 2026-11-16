package com.example.todolistproject.views

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.todolistproject.viewmodels.RequestViewModel

@Composable
fun RequestStatus(model: RequestViewModel, message: String? = null) {
    val error by model.error.collectAsStateWithLifecycle()
    val loading by model.loading.collectAsStateWithLifecycle()
    (error ?: message)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) CircularProgressIndicator()
}
