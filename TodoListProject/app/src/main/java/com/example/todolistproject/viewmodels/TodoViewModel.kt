package com.example.todolistproject.viewmodels

import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.TodoRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TodoUiState(
    val todos: List<TodoDto> = emptyList(),
    val loadedSession: Int? = null,
    val todo: TodoDto? = null,
    val confirmDelete: Boolean = false,
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false
)

class TodoViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState = _uiState.asStateFlow()
    private var editingId: String? = null

    fun setTitle(value: String) { _uiState.update { it.copy(title = value) } }
    fun setDescription(value: String) { _uiState.update { it.copy(description = value) } }
    fun setCompleted(value: Boolean) { _uiState.update { it.copy(completed = value) } }
    fun setConfirmDelete(value: Boolean) { _uiState.update { it.copy(confirmDelete = value) } }

    fun clearSession() {
        resetRequest()
        editingId = null
        _uiState.value = TodoUiState()
    }

    override fun clearOnSessionExpired() = clearSession()

    fun load(token: String, sessionVersion: Int) {
        if (_uiState.value.loadedSession != sessionVersion) clearSession()
        if (requestState.value.loading) return
        _uiState.update { it.copy(loadedSession = sessionVersion) }
        request {
            val todos = repository.todos(token)
            currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(todos = todos) }
        }
    }

    fun load(token: String, id: String) {
        if (requestState.value.loading) return
        if (_uiState.value.todo?.id != id) _uiState.update { it.copy(confirmDelete = false, todo = null) }
        request {
            val todo = repository.todo(token, id)
            currentCoroutineContext().ensureActive()
            _uiState.update { it.copy(todo = todo) }
        }
    }

    fun initialize(todo: TodoDto) {
        if (editingId == todo.id) return
        editingId = todo.id
        _uiState.update { it.copy(title = todo.title, description = todo.description ?: "", completed = todo.completed) }
    }

    fun clearFields() {
        editingId = null
        _uiState.update { it.copy(title = "", description = "", completed = false) }
    }

    fun save(token: String, onSuccess: (TodoDto) -> Unit) = save(token, null, onSuccess)

    fun save(token: String, id: String?, onSuccess: (TodoDto) -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) {
            showError("Enter a title.")
            return
        }
        val input = TodoRequestDto(state.title.trim(), state.description.takeUnless { it.isBlank() }, state.completed)
        request {
            val saved = if (id == null) repository.create(token, input) else repository.update(token, id, input)
            currentCoroutineContext().ensureActive()
            _uiState.update { state ->
                if (id == null) state.copy(todos = state.todos + saved)
                else state.copy(todos = state.todos.map { if (it.id == saved.id) saved else it }, todo = saved)
            }
            clearFields()
            onSuccess(saved)
        }
    }

    fun delete(token: String, id: String, onSuccess: () -> Unit) = request {
        repository.delete(token, id)
        currentCoroutineContext().ensureActive()
        _uiState.update { state ->
            state.copy(
                todos = state.todos.filterNot { it.id == id },
                todo = state.todo?.takeUnless { it.id == id },
                confirmDelete = false
            )
        }
        onSuccess()
    }
}
