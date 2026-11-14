package com.example.todolistproject.models

data class TodoDto(
    val id: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val userId: String,
    val createdAt: String,
    val updatedAt: String
)
