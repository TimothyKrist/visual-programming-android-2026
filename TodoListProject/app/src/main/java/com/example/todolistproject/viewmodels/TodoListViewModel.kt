package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.repositories.TodoRepository

class TodoListViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    var todos by mutableStateOf<List<TodoDto>>(emptyList())
        private set

    var loadedSession by mutableStateOf<Int?>(null)
        private set

    fun isCurrentSession(sessionVersion: Int) = loadedSession == sessionVersion

    fun load(token: String, sessionVersion: Int) {
        if (loading) return
        if (loadedSession != sessionVersion) todos = emptyList()
        loadedSession = sessionVersion
        request { todos = repository.todos(token) }
    }
    fun clear() { todos = emptyList(); loadedSession = null; clearError() }
}
