package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.TodoRepository

class EditTodoViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var completed by mutableStateOf(false)
    private var editingId: String? = null

    fun initialize(todo: TodoDto) {
        if (editingId == todo.id) return
        editingId = todo.id
        title = todo.title
        description = todo.description ?: ""
        completed = todo.completed
    }

    fun clearFields() {
        title = ""
        description = ""
        completed = false
        editingId = null
    }

    override fun clearOnSessionExpired() = clearFields()

    fun save(token: String, id: String, onSuccess: (TodoDto) -> Unit) {
        if (title.isBlank()) {
            showError("Enter a title.")
            return
        }
        val todo = TodoRequestDto(title.trim(), description.takeUnless { it.isBlank() }, completed)
        request { val updated = repository.update(token, id, todo); clearFields(); onSuccess(updated) }
    }
}
