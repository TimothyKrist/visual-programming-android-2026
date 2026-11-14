package com.example.todolistproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.todolistproject.ui.theme.TodoListProjectTheme
import com.example.todolistproject.views.TodoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TodoListProjectTheme { TodoApp() } }
    }
}
