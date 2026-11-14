package com.example.todolistproject.repositories

import com.example.todolistproject.models.CredentialsDto
import com.example.todolistproject.services.AuthService

class AuthRepository(private val api: AuthService) {
    suspend fun login(email: String, password: String) = api.login(CredentialsDto(email.trim(), password))
    suspend fun register(email: String, password: String) = api.register(CredentialsDto(email.trim(), password))
}
