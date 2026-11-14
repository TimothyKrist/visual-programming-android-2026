package com.example.todolistproject.services

import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TodoService {
    @GET("todos")
    suspend fun todos(@Header("Authorization") authorization: String): List<TodoDto>

    @GET("todos/{id}")
    suspend fun todo(@Header("Authorization") authorization: String, @Path("id") id: String): TodoDto

    @POST("todos")
    suspend fun create(@Header("Authorization") authorization: String, @Body todo: TodoRequestDto): TodoDto

    @PATCH("todos/{id}")
    suspend fun update(@Header("Authorization") authorization: String, @Path("id") id: String, @Body todo: TodoRequestDto): TodoDto

    @DELETE("todos/{id}")
    suspend fun delete(@Header("Authorization") authorization: String, @Path("id") id: String)
}
