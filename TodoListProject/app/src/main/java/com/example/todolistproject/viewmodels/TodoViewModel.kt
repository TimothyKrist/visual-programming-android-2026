package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.TodoRepository

class TodoViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    var todos by mutableStateOf<List<TodoDto>>(emptyList())
        private set
    var loadedSession by mutableStateOf<Int?>(null)
        private set
    var todo by mutableStateOf<TodoDto?>(null)
        private set
    var confirmDelete by mutableStateOf(false)
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var completed by mutableStateOf(false)
    private var editingId: String? = null
    private var loadedId: String? = null

    fun isCurrentSession(sessionVersion: Int) = loadedSession == sessionVersion
    fun currentTodo(id: String) = todo?.takeIf { it.id == id }

    override fun clearOnSessionExpired() {
        todos = emptyList()
        loadedSession = null
        todo = null
        loadedId = null
        confirmDelete = false
        clearFields()
    }

    fun load(token: String, sessionVersion: Int) {
        if (loading) return
        if (loadedSession != sessionVersion) clearOnSessionExpired()
        loadedSession = sessionVersion
        request { todos = repository.todos(token) }
    }

    fun load(token: String, id: String) {
        if (loading) return
        if (loadedId != id) {
            confirmDelete = false
            todo = null
        }
        loadedId = id
        request { todo = repository.todo(token, id) }
    }

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

    fun save(token: String, onSuccess: (TodoDto) -> Unit) = save(token, null, onSuccess)

    fun save(token: String, id: String?, onSuccess: (TodoDto) -> Unit) {
        if (title.isBlank()) {
            showError("Enter a title.")
            return
        }
        val input = TodoRequestDto(title.trim(), description.takeUnless { it.isBlank() }, completed)
        request {
            val saved = if (id == null) repository.create(token, input) else repository.update(token, id, input)
            if (id == null) {
                todos = todos + saved
            } else {
                todos = todos.map { if (it.id == saved.id) saved else it }
                todo = saved
            }
            clearFields()
            onSuccess(saved)
        }
    }

    fun delete(token: String, id: String, onSuccess: () -> Unit) = request {
        repository.delete(token, id)
        todos = todos.filterNot { it.id == id }
        if (todo?.id == id) todo = null
        confirmDelete = false
        onSuccess()
    }
}
