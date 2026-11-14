package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.viewmodels.TodoDetailViewModel

@Composable
fun TodoDetailView(model: TodoDetailViewModel, token: String, id: String, onBack: () -> Unit, onEdit: (TodoDto) -> Unit, onDeleted: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.load(token, id) }
    BackHandler { if (!model.loading) onBack() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            model.currentTodo(id)?.let { todo ->
                Text(todo.title, style = MaterialTheme.typography.headlineSmall)
                Text(if (todo.completed) "Completed" else "Not completed")
                Text(todo.description ?: "No description")
                Text("Created: ${todo.createdAt}")
                Text("Updated: ${todo.updatedAt}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack, enabled = !model.loading) { Text("Back") }
                if (model.currentTodo(id) == null) {
                    OutlinedButton(onClick = { model.load(token, id) }, enabled = !model.loading) { Text("Retry") }
                } else {
                    Button(onClick = { model.currentTodo(id)?.let(onEdit) }, enabled = !model.loading) { Text("Edit") }
                    OutlinedButton(onClick = { model.confirmDelete = true }, enabled = !model.loading) { Text("Delete") }
                }
            }
        }
        if (model.confirmDelete) AlertDialog(
            onDismissRequest = { model.confirmDelete = false },
            title = { Text("Delete todo?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { model.confirmDelete = false; model.delete(token, id, onDeleted) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { model.confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
