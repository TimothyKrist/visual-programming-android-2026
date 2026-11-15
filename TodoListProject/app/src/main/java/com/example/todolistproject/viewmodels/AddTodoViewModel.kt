package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.TodoRepository

class AddTodoViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var completed by mutableStateOf(false)

    fun clearFields() {
        title = ""
        description = ""
        completed = false
    }

    override fun clearOnSessionExpired() = clearFields()

    fun save(token: String, onSuccess: (TodoDto) -> Unit) {
        if (title.isBlank()) {
            showError("Enter a title.")
            return
        }
        val todo = TodoRequestDto(title.trim(), description.takeUnless { it.isBlank() }, completed)
        request { val created = repository.create(token, todo); clearFields(); onSuccess(created) }
    }
}
