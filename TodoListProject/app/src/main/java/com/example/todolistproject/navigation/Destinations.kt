package com.example.todolistproject.navigation

import kotlinx.serialization.Serializable

@Serializable data object LoginDestination
@Serializable data object RegisterDestination
@Serializable data object TodoListDestination
@Serializable data class TodoDetailDestination(val id: String)
@Serializable data object AddTodoDestination
@Serializable data object EditTodoDestination
