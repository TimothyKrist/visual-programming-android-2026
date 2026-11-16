package com.example.todolistproject.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolistproject.models.ErrorDto
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class RequestUiState(
    val loading: Boolean = false,
    val error: String? = null
)

abstract class RequestViewModel(private val onSessionExpired: (() -> Unit)? = null) : ViewModel() {
    private val _requestState = MutableStateFlow(RequestUiState())
    val requestState = _requestState.asStateFlow()
    private var requestJob: Job? = null
    private var requestVersion = 0

    fun clearError() { _requestState.update { it.copy(error = null) } }
    protected fun showError(message: String) { _requestState.update { it.copy(error = message) } }
    protected open fun clearOnSessionExpired() {}

    protected fun resetRequest() {
        requestVersion++
        requestJob?.cancel()
        requestJob = null
        _requestState.value = RequestUiState()
    }

    protected fun request(action: suspend () -> Unit) {
        if (_requestState.value.loading) return
        val version = ++requestVersion
        _requestState.value = RequestUiState(loading = true)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version != requestVersion) return@launch
                if (e is HttpException && e.code() == 401 && onSessionExpired != null) {
                    clearOnSessionExpired()
                    onSessionExpired.invoke()
                } else {
                    val message = when (e) {
                        is HttpException -> {
                            val serverMessage = try {
                                Gson().fromJson(e.response()?.errorBody()?.string(), ErrorDto::class.java)?.error
                            } catch (_: Exception) { null }
                            serverMessage ?: "Request failed (${e.code()})."
                        }
                        is IOException -> "Could not connect to the server. Check the connection and try again."
                        else -> "Something went wrong. Please try again."
                    }
                    _requestState.update { it.copy(error = message) }
                }
            } finally {
                if (version == requestVersion) {
                    requestJob = null
                    _requestState.update { it.copy(loading = false) }
                }
            }
        }
        requestJob = job
        job.start()
    }
}
