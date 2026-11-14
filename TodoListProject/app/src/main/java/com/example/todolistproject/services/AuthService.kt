package com.example.todolistproject.services

import com.example.todolistproject.models.AuthResponseDto
import com.example.todolistproject.models.CredentialsDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body credentials: CredentialsDto): AuthResponseDto

    @POST("auth/register")
    suspend fun register(@Body credentials: CredentialsDto): AuthResponseDto
}
