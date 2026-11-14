package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.example.todolistproject.viewmodels.AddTodoViewModel

@Composable
fun AddTodoView(model: AddTodoViewModel, token: String, onBack: () -> Unit) {
    LaunchedEffect(Unit) { model.clearError() }
    BackHandler { if (!model.loading) { model.clearFields(); onBack() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        TodoFields(editing = false, enabled = !model.loading, title = model.title, onTitleChange = { model.title = it },
            description = model.description, onDescriptionChange = { model.description = it },
            completed = model.completed, onCompletedChange = { model.completed = it },
            onSave = { model.save(token, onBack) }, onCancel = { model.clearFields(); onBack() })
    }
}
