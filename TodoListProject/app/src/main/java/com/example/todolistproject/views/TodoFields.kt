package com.example.todolistproject.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun TodoFields(editing: Boolean, enabled: Boolean, title: String, onTitleChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit, completed: Boolean,
    onCompletedChange: (Boolean) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (editing) "Edit todo" else "New todo", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(title, onTitleChange, label = { Text("Title") }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, onDescriptionChange, label = { Text("Description (optional)") }, enabled = enabled, modifier = Modifier.fillMaxWidth())
        Row(Modifier.toggleable(value = completed, enabled = enabled, role = Role.Checkbox, onValueChange = onCompletedChange),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = completed, onCheckedChange = null, enabled = enabled)
            Text("Completed")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = enabled) { Text("Save") }
            OutlinedButton(onClick = onCancel, enabled = enabled) { Text("Cancel") }
        }
    }
}
