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
    fun add(todo: TodoDto) { todos = todos + todo }
    fun update(todo: TodoDto) { todos = todos.map { if (it.id == todo.id) todo else it } }
    fun remove(id: String) { todos = todos.filterNot { it.id == id } }
}
