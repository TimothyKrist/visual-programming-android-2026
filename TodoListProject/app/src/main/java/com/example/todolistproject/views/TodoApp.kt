package com.example.todolistproject.views

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.todolistproject.navigation.AddTodoDestination
import com.example.todolistproject.navigation.EditTodoDestination
import com.example.todolistproject.navigation.LoginDestination
import com.example.todolistproject.navigation.RegisterDestination
import com.example.todolistproject.navigation.TodoDetailDestination
import com.example.todolistproject.navigation.TodoListDestination
import com.example.todolistproject.viewmodels.AppViewModel
import com.example.todolistproject.viewmodels.AuthViewModel
import com.example.todolistproject.viewmodels.TodoViewModel

@Composable
fun TodoApp(app: AppViewModel = viewModel(factory = AppViewModel.Factory), navController: NavHostController = rememberNavController()) {
    val startDestination = remember(app) {
        if (app.uiState.value.token == null) LoginDestination else TodoListDestination
    }
    val auth: AuthViewModel = viewModel(factory = viewModelFactory { initializer { AuthViewModel(app) } })
    val todos: TodoViewModel = viewModel(factory = viewModelFactory {
        initializer { TodoViewModel(app.todoRepository) { auth.logout(expired = true) } }
    })
    val appState by app.uiState.collectAsStateWithLifecycle()
    val token = appState.token
    LaunchedEffect(token, navController.currentBackStackEntryAsState().value) {
        if (token == null) todos.clearSession()
        if (token == null && navController.currentDestination?.let {
                !it.hasRoute<LoginDestination>() && !it.hasRoute<RegisterDestination>()
            } == true) {
            navController.navigate(LoginDestination) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Todo List", style = MaterialTheme.typography.headlineMedium)
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable<LoginDestination> {
                    LoginView(auth,
                        onLogin = {
                            auth.login {
                                navController.navigate(TodoListDestination) { popUpTo(navController.graph.id) { inclusive = true } }
                            }
                        },
                        onRegister = { auth.clearLoginMessage(); navController.navigate(RegisterDestination) })
                }
                composable<RegisterDestination> {
                    RegisterView(auth, onRegister = {
                        auth.register {
                            navController.navigate(TodoListDestination) { popUpTo(navController.graph.id) { inclusive = true } }
                        }
                    }, onLogin = { navController.popBackStack() })
                }
                composable<TodoListDestination> {
                    token?.let { token ->
                        TodoListView(todos, token, appState.sessionVersion,
                            onAdd = { navController.navigate(AddTodoDestination) },
                            onOpen = { navController.navigate(TodoDetailDestination(it.id)) },
                            onLogout = { todos.clearSession(); auth.logout() })
                    }
                }
                composable<TodoDetailDestination> { entry ->
                    token?.let { token ->
                        val id = entry.toRoute<TodoDetailDestination>().id
                        TodoDetailView(todos, token, id,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate(EditTodoDestination) },
                            onDeleted = { navController.popBackStack<TodoListDestination>(inclusive = false) })
                    }
                }
                composable<AddTodoDestination> {
                    token?.let { token ->
                        AddTodoView(todos, token, onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() })
                    }
                }
                composable<EditTodoDestination> {
                    token?.let { token ->
                        val state by todos.uiState.collectAsStateWithLifecycle()
                        state.todo?.let { todo ->
                            EditTodoView(todos, token, todo, onBack = { navController.popBackStack() },
                                onSaved = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
