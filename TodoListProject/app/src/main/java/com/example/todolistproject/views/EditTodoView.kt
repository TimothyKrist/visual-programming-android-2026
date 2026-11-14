package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.viewmodels.EditTodoViewModel

@Composable
fun EditTodoView(model: EditTodoViewModel, token: String, todo: TodoDto, onBack: () -> Unit, onSaved: () -> Unit) {
    LaunchedEffect(todo.id) { model.initialize(todo); model.clearError() }
    BackHandler { if (!model.loading) { model.clearFields(); onBack() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        TodoFields(editing = true, enabled = !model.loading, title = model.title, onTitleChange = { model.title = it },
            description = model.description, onDescriptionChange = { model.description = it },
            completed = model.completed, onCompletedChange = { model.completed = it },
            onSave = { model.save(token, todo.id, onSaved) }, onCancel = { model.clearFields(); onBack() })
    }
}
