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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.viewmodels.TodoViewModel

@Composable
fun TodoDetailView(model: TodoViewModel, token: String, id: String, onBack: () -> Unit, onEdit: (TodoDto) -> Unit, onDeleted: () -> Unit) {
    val loading by model.loading.collectAsStateWithLifecycle()
    val selectedTodo by model.todo.collectAsStateWithLifecycle()
    val confirmDelete by model.confirmDelete.collectAsStateWithLifecycle()
    val currentTodo = selectedTodo?.takeIf { it.id == id }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.load(token, id) }
    BackHandler { if (!loading) onBack() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RequestStatus(model)
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            currentTodo?.let { todo ->
                Text(todo.title, style = MaterialTheme.typography.headlineSmall)
                Text(if (todo.completed) "Completed" else "Not completed")
                Text(todo.description ?: "No description")
                Text("Created: ${todo.createdAt}")
                Text("Updated: ${todo.updatedAt}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack, enabled = !loading) { Text("Back") }
                if (currentTodo == null) {
                    OutlinedButton(onClick = { model.load(token, id) }, enabled = !loading) { Text("Retry") }
                } else {
                    Button(onClick = { currentTodo.let(onEdit) }, enabled = !loading) { Text("Edit") }
                    OutlinedButton(onClick = { model.setConfirmDelete(true) }, enabled = !loading) { Text("Delete") }
                }
            }
        }
        if (confirmDelete) AlertDialog(
            onDismissRequest = { model.setConfirmDelete(false) },
            title = { Text("Delete todo?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton(onClick = { model.setConfirmDelete(false); model.delete(token, id, onDeleted) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { model.setConfirmDelete(false) }) { Text("Cancel") } }
        )
    }
}
