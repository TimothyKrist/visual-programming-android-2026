package com.example.todolistproject.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.todolistproject.BuildConfig
import com.example.todolistproject.repositories.AuthRepository
import com.example.todolistproject.repositories.TodoRepository
import com.example.todolistproject.services.AuthService
import com.example.todolistproject.services.TodoService
import com.example.todolistproject.services.createRetrofit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(
    val authRepository: AuthRepository,
    val todoRepository: TodoRepository,
    private val preferences: SharedPreferences
) : ViewModel() {
    private val _token = MutableStateFlow(preferences.getString("access_token", null))
    val token = _token.asStateFlow()
    private val _sessionVersion = MutableStateFlow(preferences.getInt("session_version", 0))
    val sessionVersion = _sessionVersion.asStateFlow()

    fun setSession(accessToken: String) {
        val version = _sessionVersion.value + 1
        preferences.edit().putString("access_token", accessToken).putInt("session_version", version).apply()
        _token.value = accessToken
        _sessionVersion.value = version
    }
    fun clearSession() {
        preferences.edit().remove("access_token").apply()
        _token.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val retrofit = createRetrofit(BuildConfig.API_BASE_URL)
                AppViewModel(
                    AuthRepository(retrofit.create(AuthService::class.java)),
                    TodoRepository(retrofit.create(TodoService::class.java)),
                    application.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
                )
            }
        }
    }
}
