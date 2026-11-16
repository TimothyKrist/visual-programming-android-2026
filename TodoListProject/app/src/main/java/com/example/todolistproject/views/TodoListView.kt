package com.example.todolistproject.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
fun TodoListView(model: TodoViewModel, token: String, sessionVersion: Int, onAdd: () -> Unit, onOpen: (TodoDto) -> Unit, onLogout: () -> Unit) {
    val loading by model.loading.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val todos by model.todos.collectAsStateWithLifecycle()
    val loadedSession by model.loadedSession.collectAsStateWithLifecycle()
    val isCurrentSession = loadedSession == sessionVersion
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        model.load(token, sessionVersion)
    }
    BackHandler(enabled = loading) {}
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isCurrentSession) RequestStatus(model)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd, enabled = !loading && isCurrentSession) { Text("Add todo") }
            OutlinedButton(onClick = { model.load(token, sessionVersion) }, enabled = !loading && isCurrentSession) { Text("Refresh") }
            TextButton(onClick = onLogout, enabled = !loading && isCurrentSession) { Text("Log out") }
        }
        if (isCurrentSession && todos.isEmpty() && !loading && error == null) Text("No todos yet.")
        LazyColumn {
            items(if (isCurrentSession) todos else emptyList(), key = { it.id }) { todo ->
                Column(Modifier.fillMaxWidth().clickable(enabled = !loading && isCurrentSession) { onOpen(todo) }.padding(vertical = 14.dp)) {
                    Text(todo.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (todo.completed) "Completed" else "Not completed")
                }
                HorizontalDivider()
            }
        }
    }
}
