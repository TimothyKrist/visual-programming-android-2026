package com.example.todolistproject.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolistproject.models.ErrorDto
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

abstract class RequestViewModel(private val onSessionExpired: (() -> Unit)? = null) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() { _error.value = null }
    protected fun showError(message: String) { _error.value = message }
    protected open fun clearOnSessionExpired() {}

    protected fun request(action: suspend () -> Unit) {
        if (_loading.value) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 401 && onSessionExpired != null) {
                    clearOnSessionExpired()
                    onSessionExpired.invoke()
                } else {
                    val message = try {
                        Gson().fromJson(e.response()?.errorBody()?.string(), ErrorDto::class.java)?.error
                    } catch (_: Exception) { null }
                    _error.value = message ?: "Request failed (${e.code()})."
                }
            } catch (_: IOException) {
                _error.value = "Could not connect to the server. Check the connection and try again."
            } catch (_: Exception) {
                _error.value = "Something went wrong. Please try again."
            } finally {
                _loading.value = false
            }
        }
    }
}
