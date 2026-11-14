package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.repositories.TodoRepository

class TodoDetailViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    var todo by mutableStateOf<TodoDto?>(null)
        private set

    var confirmDelete by mutableStateOf(false)
    private var loadedId: String? = null

    override fun clearOnSessionExpired() {
        todo = null
        loadedId = null
        confirmDelete = false
    }

    fun currentTodo(id: String) = todo?.takeIf { it.id == id }

    fun load(token: String, id: String) {
        if (loading) return
        if (loadedId != id) confirmDelete = false
        loadedId = id
        todo = null
        request { todo = repository.todo(token, id) }
    }
    fun delete(token: String, id: String, onSuccess: () -> Unit) = request {
        repository.delete(token, id)
        onSuccess()
    }
}
