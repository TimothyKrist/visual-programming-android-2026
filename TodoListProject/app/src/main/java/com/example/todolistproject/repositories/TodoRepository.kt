package com.example.todolistproject.repositories

import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.services.TodoService

class TodoRepository(private val api: TodoService) {
    suspend fun todos(token: String) = api.todos("Bearer $token")
    suspend fun todo(token: String, id: String) = api.todo("Bearer $token", id)
    suspend fun create(token: String, todo: TodoRequestDto) = api.create("Bearer $token", todo)
    suspend fun update(token: String, id: String, todo: TodoRequestDto) = api.update("Bearer $token", id, todo)
    suspend fun delete(token: String, id: String) = api.delete("Bearer $token", id)
}
