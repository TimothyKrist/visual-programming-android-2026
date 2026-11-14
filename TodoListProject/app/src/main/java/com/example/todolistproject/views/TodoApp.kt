package com.example.todolistproject.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.todolistproject.viewmodels.AddTodoViewModel
import com.example.todolistproject.viewmodels.AppViewModel
import com.example.todolistproject.viewmodels.EditTodoViewModel
import com.example.todolistproject.viewmodels.LoginViewModel
import com.example.todolistproject.viewmodels.RegisterViewModel
import com.example.todolistproject.viewmodels.TodoDetailViewModel
import com.example.todolistproject.viewmodels.TodoListViewModel

@Composable
fun TodoApp(app: AppViewModel = viewModel(), navController: NavHostController = rememberNavController()) {

    LaunchedEffect(app.token, navController.currentBackStackEntryAsState().value) {
        if (app.token == null && navController.currentDestination?.let {
                !it.hasRoute<LoginDestination>() && !it.hasRoute<RegisterDestination>()
            } == true) {
            navController.navigate(LoginDestination) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Todo List", style = MaterialTheme.typography.headlineMedium)
            NavHost(navController = navController, startDestination = LoginDestination) {
                composable<LoginDestination> {
                    val viewModel: LoginViewModel = viewModel(factory = viewModelFactory { initializer { LoginViewModel(app.authRepository) } })
                    LoginView(viewModel, app.loginMessage,
                        onLogin = {
                            app.clearLoginMessage()
                            viewModel.login { token ->
                                app.authenticated(token)
                                navController.navigate(TodoListDestination) { popUpTo(navController.graph.id) { inclusive = true } }
                            }
                        },
                        onRegister = { app.clearLoginMessage(); navController.navigate(RegisterDestination) })
                }
                composable<RegisterDestination> {
                    val viewModel: RegisterViewModel = viewModel(factory = viewModelFactory { initializer { RegisterViewModel(app.authRepository) } })
                    RegisterView(viewModel, onRegister = {
                        viewModel.register { token ->
                            app.authenticated(token)
                            navController.navigate(TodoListDestination) { popUpTo(navController.graph.id) { inclusive = true } }
                        }
                    }, onLogin = { navController.popBackStack() })
                }
                composable<TodoListDestination> {
                    app.token?.let { token ->
                        val viewModel: TodoListViewModel = viewModel(factory = viewModelFactory {
                            initializer { TodoListViewModel(app.todoRepository) { app.logout(expired = true) } }
                        })
                        TodoListView(viewModel, token, app.sessionVersion,
                            onAdd = { navController.navigate(AddTodoDestination) },
                            onOpen = { navController.navigate(TodoDetailDestination(it.id)) },
                            onLogout = { app.logout() })
                    }
                }
                composable<TodoDetailDestination> { entry ->
                    app.token?.let { token ->
                        val viewModel: TodoDetailViewModel = viewModel(factory = viewModelFactory {
                            initializer { TodoDetailViewModel(app.todoRepository) { app.logout(expired = true) } }
                        })
                        TodoDetailView(viewModel, token, entry.toRoute<TodoDetailDestination>().id,
                            onBack = { navController.popBackStack() },
                            onEdit = { app.select(it); navController.navigate(EditTodoDestination) },
                            onDeleted = { navController.popBackStack<TodoListDestination>(inclusive = false) })
                    }
                }
                composable<AddTodoDestination> {
                    app.token?.let { token ->
                        val viewModel: AddTodoViewModel = viewModel(factory = viewModelFactory {
                            initializer { AddTodoViewModel(app.todoRepository) { app.logout(expired = true) } }
                        })
                        AddTodoView(viewModel, token, onBack = { navController.popBackStack() })
                    }
                }
                composable<EditTodoDestination> {
                    app.token?.let { token ->
                        app.selected?.let { todo ->
                            val viewModel: EditTodoViewModel = viewModel(factory = viewModelFactory {
                                initializer { EditTodoViewModel(app.todoRepository) { app.logout(expired = true) } }
                            })
                            EditTodoView(viewModel, token, todo, onBack = { navController.popBackStack() },
                                onSaved = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
