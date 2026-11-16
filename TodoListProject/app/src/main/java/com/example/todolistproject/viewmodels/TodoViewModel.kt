package com.example.todolistproject.viewmodels

import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodoViewModel(private val repository: TodoRepository, onSessionExpired: () -> Unit) : RequestViewModel(onSessionExpired) {
    private val _todos = MutableStateFlow<List<TodoDto>>(emptyList())
    val todos = _todos.asStateFlow()
    private val _loadedSession = MutableStateFlow<Int?>(null)
    val loadedSession = _loadedSession.asStateFlow()
    private val _todo = MutableStateFlow<TodoDto?>(null)
    val todo = _todo.asStateFlow()
    private val _confirmDelete = MutableStateFlow(false)
    val confirmDelete = _confirmDelete.asStateFlow()
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    private val _completed = MutableStateFlow(false)
    val completed = _completed.asStateFlow()
    private var editingId: String? = null
    private var loadedId: String? = null

    fun setTitle(value: String) { _title.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setCompleted(value: Boolean) { _completed.value = value }
    fun setConfirmDelete(value: Boolean) { _confirmDelete.value = value }

    override fun clearOnSessionExpired() {
        _todos.value = emptyList()
        _loadedSession.value = null
        _todo.value = null
        loadedId = null
        _confirmDelete.value = false
        clearFields()
    }

    fun load(token: String, sessionVersion: Int) {
        if (loading.value) return
        if (_loadedSession.value != sessionVersion) clearOnSessionExpired()
        _loadedSession.value = sessionVersion
        request { _todos.value = repository.todos(token) }
    }

    fun load(token: String, id: String) {
        if (loading.value) return
        if (loadedId != id) {
            _confirmDelete.value = false
            _todo.value = null
        }
        loadedId = id
        request { _todo.value = repository.todo(token, id) }
    }

    fun initialize(todo: TodoDto) {
        if (editingId == todo.id) return
        editingId = todo.id
        _title.value = todo.title
        _description.value = todo.description ?: ""
        _completed.value = todo.completed
    }

    fun clearFields() {
        _title.value = ""
        _description.value = ""
        _completed.value = false
        editingId = null
    }

    fun save(token: String, onSuccess: (TodoDto) -> Unit) = save(token, null, onSuccess)

    fun save(token: String, id: String?, onSuccess: (TodoDto) -> Unit) {
        if (_title.value.isBlank()) {
            showError("Enter a title.")
            return
        }
        val input = TodoRequestDto(_title.value.trim(), _description.value.takeUnless { it.isBlank() }, _completed.value)
        request {
            val saved = if (id == null) repository.create(token, input) else repository.update(token, id, input)
            if (id == null) {
                _todos.value = _todos.value + saved
            } else {
                _todos.value = _todos.value.map { if (it.id == saved.id) saved else it }
                _todo.value = saved
            }
            clearFields()
            onSuccess(saved)
        }
    }

    fun delete(token: String, id: String, onSuccess: () -> Unit) = request {
        repository.delete(token, id)
        _todos.value = _todos.value.filterNot { it.id == id }
        if (_todo.value?.id == id) _todo.value = null
        _confirmDelete.value = false
        onSuccess()
    }
}
