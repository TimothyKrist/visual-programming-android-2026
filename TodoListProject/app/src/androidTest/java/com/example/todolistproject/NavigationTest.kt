package com.example.todolistproject

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todolistproject.navigation.AddTodoDestination
import com.example.todolistproject.navigation.EditTodoDestination
import com.example.todolistproject.navigation.LoginDestination
import com.example.todolistproject.navigation.RegisterDestination
import com.example.todolistproject.navigation.TodoDetailDestination
import com.example.todolistproject.navigation.TodoListDestination
import com.example.todolistproject.models.AuthResponseDto
import com.example.todolistproject.models.CredentialsDto
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.AuthRepository
import com.example.todolistproject.repositories.TodoRepository
import com.example.todolistproject.services.AuthService
import com.example.todolistproject.services.TodoService
import com.example.todolistproject.viewmodels.AppViewModel
import com.example.todolistproject.views.TodoApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlin.reflect.KClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var nav: NavHostController
    private val api = FakeApi()
    private val app = AppViewModel(AuthRepository(api), TodoRepository(api))

    private fun start() {
        compose.setContent {
            nav = rememberNavController()
            TodoApp(app, nav)
        }
    }

    private fun click(text: String) = compose.onNode(hasText(text) and hasClickAction()).performClick()
    private fun back() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    private fun assertRoute(route: KClass<*>, previous: KClass<*>?) = compose.runOnIdle {
        assertTrue(nav.currentDestination?.hasRoute(route) == true)
        if (previous == null) assertNull(nav.previousBackStackEntry)
        else assertTrue(nav.previousBackStackEntry?.destination?.hasRoute(previous) == true)
    }

    private fun fillAuth() {
        compose.onNodeWithText("Email").performTextInput("a@example.com")
        compose.onNodeWithText("Password").performTextInput("password123")
    }

    @Test fun loginClearsAuthenticationAndTodoScreensKeepBackNavigation() {
        start()
        click("Need an account? Register")
        assertRoute(RegisterDestination::class, LoginDestination::class)
        back()
        assertRoute(LoginDestination::class, null)
        fillAuth()
        click("Log in")
        compose.onNodeWithText("Add todo").assertIsDisplayed()
        assertRoute(TodoListDestination::class, null)

        click("Task")
        compose.onNodeWithText("Edit").assertIsDisplayed()
        assertRoute(TodoDetailDestination::class, TodoListDestination::class)
        compose.runOnIdle { assertEquals(api.id, nav.currentBackStackEntry?.toRoute<TodoDetailDestination>()?.id) }
        click("Edit")
        compose.onNodeWithText("Edit todo").assertIsDisplayed()
        assertRoute(EditTodoDestination::class, TodoDetailDestination::class)
        back()
        assertRoute(TodoDetailDestination::class, TodoListDestination::class)
        click("Edit")
        compose.onNodeWithText("Title").performTextClearance()
        compose.onNodeWithText("Title").performTextInput("Updated")
        click("Save")
        compose.onNodeWithText("Updated").assertIsDisplayed()
        assertRoute(TodoDetailDestination::class, TodoListDestination::class)
        back()
        assertRoute(TodoListDestination::class, null)
        compose.onNodeWithText("Updated").assertIsDisplayed()
        click("Add todo")
        assertRoute(AddTodoDestination::class, TodoListDestination::class)
        back()
        assertRoute(TodoListDestination::class, null)
        click("Updated")
        click("Delete")
        compose.onNodeWithText("Delete todo?").assertIsDisplayed()
        compose.onAllNodes(hasText("Delete") and hasClickAction()).onLast().performClick()
        assertRoute(TodoListDestination::class, null)
        click("Log out")
        compose.onNodeWithText("Need an account? Register").assertIsDisplayed()
        assertRoute(LoginDestination::class, null)
    }

    @Test fun registrationAndSessionExpiryClearTheBackStack() {
        start()
        click("Need an account? Register")
        fillAuth()
        click("Register")
        compose.onNodeWithText("Task").assertIsDisplayed()
        assertRoute(TodoListDestination::class, null)
        compose.runOnIdle { api.expired = true }
        click("Task")
        compose.onNodeWithText("Your session expired. Please log in again.").assertIsDisplayed()
        assertRoute(LoginDestination::class, null)
    }

    @Test fun aProtectedDestinationWithoutASessionReturnsToLogin() {
        start()
        compose.runOnIdle { nav.navigate(TodoDetailDestination("id")) }
        compose.onNodeWithText("Need an account? Register").assertIsDisplayed()
        assertRoute(LoginDestination::class, null)
    }

    private class FakeApi : AuthService, TodoService {
        var expired = false
        val id = "id/with ?#%"
        private var item = TodoDto(id, "Task", null, false, "user", "created", "updated")
        override suspend fun login(credentials: CredentialsDto) = AuthResponseDto("token")
        override suspend fun register(credentials: CredentialsDto) = AuthResponseDto("token")
        override suspend fun todos(authorization: String) = listOf(item)
        override suspend fun todo(authorization: String, id: String): TodoDto {
            assertEquals(this.id, id)
            if (expired) throw HttpException(Response.error<Any>(401, ResponseBody.create(null, "")))
            return item
        }
        override suspend fun create(authorization: String, todo: TodoRequestDto) = item
        override suspend fun update(authorization: String, id: String, todo: TodoRequestDto): TodoDto {
            item = item.copy(title = todo.title, description = todo.description, completed = todo.completed)
            return item
        }
        override suspend fun delete(authorization: String, id: String) {}
    }
}
