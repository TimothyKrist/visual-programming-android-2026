package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.todolistproject.BuildConfig
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.repositories.AuthRepository
import com.example.todolistproject.repositories.TodoRepository
import com.example.todolistproject.services.AuthService
import com.example.todolistproject.services.TodoService
import com.example.todolistproject.services.createRetrofit
import retrofit2.Retrofit

class AppViewModel(val authRepository: AuthRepository, val todoRepository: TodoRepository) : ViewModel() {
    constructor() : this(createRetrofit(BuildConfig.API_BASE_URL))
    private constructor(retrofit: Retrofit) : this(
        AuthRepository(retrofit.create(AuthService::class.java)),
        TodoRepository(retrofit.create(TodoService::class.java))
    )

    var token by mutableStateOf<String?>(null)
        private set
    var sessionVersion by mutableStateOf(0)
        private set
    var selected by mutableStateOf<TodoDto?>(null)
        private set
    var loginMessage by mutableStateOf<String?>(null)
        private set

    fun authenticated(accessToken: String) {
        token = accessToken
        sessionVersion++
        loginMessage = null
    }
    fun select(todo: TodoDto) { selected = todo }
    fun logout(expired: Boolean = false) {
        token = null
        selected = null
        loginMessage = if (expired) "Your session expired. Please log in again." else null
    }
    fun clearLoginMessage() { loginMessage = null }
}
