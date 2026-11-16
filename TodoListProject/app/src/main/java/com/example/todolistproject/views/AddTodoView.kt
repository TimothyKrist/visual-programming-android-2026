package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.viewmodels.TodoViewModel

@Composable
fun AddTodoView(model: TodoViewModel, token: String, onBack: () -> Unit, onSaved: (TodoDto) -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val request by model.requestState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler { if (!request.loading) { model.clearFields(); onBack() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        TodoFields(editing = false, enabled = !request.loading, title = state.title, onTitleChange = model::setTitle,
            description = state.description, onDescriptionChange = model::setDescription,
            completed = state.completed, onCompletedChange = model::setCompleted,
            onSave = { model.save(token, onSaved) }, onCancel = { model.clearFields(); onBack() })
    }
}
