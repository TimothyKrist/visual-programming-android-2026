package com.example.todolistproject.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolistproject.models.ErrorDto
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException

abstract class RequestViewModel(private val onSessionExpired: (() -> Unit)? = null) : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null }
    protected fun showError(message: String) { error = message }
    protected open fun clearOnSessionExpired() {}

    protected fun request(action: suspend () -> Unit) {
        if (loading) return
        loading = true
        error = null
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
                    error = message ?: "Request failed (${e.code()})."
                }
            } catch (_: IOException) {
                error = "Could not connect to the server. Check the connection and try again."
            } catch (_: Exception) {
                error = "Something went wrong. Please try again."
            } finally {
                loading = false
            }
        }
    }
}
