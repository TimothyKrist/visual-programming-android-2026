package com.example.todolistproject.viewmodels

import com.example.todolistproject.models.AuthResponseDto
import com.example.todolistproject.models.CredentialsDto
import com.example.todolistproject.models.TodoDto
import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.AuthRepository
import com.example.todolistproject.repositories.TodoRepository
import com.example.todolistproject.services.AuthService
import com.example.todolistproject.services.TodoService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelsTest {
    private val api = FakeApi()
    private val auth = AuthRepository(api)
    private val todos = TodoRepository(api)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `authentication validates input and uses each endpoint`() {
        val login = LoginViewModel(auth)
        val register = RegisterViewModel(auth)
        var token: String? = null
        login.password = "password"
        login.login() { token = it }
        assertEquals("Enter your email and password.", login.error)
        register.email = "a@example.com"
        register.password = "short"
        register.register() { token = it }
        assertEquals("Registration passwords must be at least 9 characters.", register.error)
        assertEquals(0, api.calls)

        login.email = " a@example.com "
        login.login() { token = it }
        assertEquals("login-token", token)
        assertEquals("a@example.com", api.credentials?.email)
        register.password = "password123"
        register.register() { token = it }
        assertEquals("register-token", token)
        assertEquals(2, api.calls)
    }

    @Test fun `todo view models load create update and delete through repository`() {
        val list = TodoListViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        val detail = TodoDetailViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        val add = AddTodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        val edit = EditTodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        var completed = false
        list.load("token", 1)
        assertEquals(listOf(api.todo), list.todos)
        detail.load("token", "id")
        assertEquals(api.todo, detail.todo)
        add.title = "  "
        add.save("token") { completed = true }
        assertEquals("Enter a title.", add.error)
        assertFalse(completed)
        add.title = " Task "
        add.save("token") { completed = true }
        assertTrue(completed)
        assertEquals(TodoRequestDto("Task", null, false), api.request)
        completed = false
        edit.initialize(api.todo)
        edit.title = "Changed"
        edit.description = "Details"
        edit.completed = true
        edit.save("token", "id") { completed = true }
        assertTrue(completed)
        assertEquals(TodoRequestDto("Changed", "Details", true), api.request)
        completed = false
        detail.delete("token", "id") { completed = true }
        assertTrue(completed)
        assertEquals("Bearer token", api.authorization)
    }

    @Test fun `form state survives initialization and clears on success or session expiry`() {
        val edit = EditTodoViewModel(todos) {}
        edit.initialize(api.todo)
        assertEquals("Task", edit.title)
        edit.title = "Draft"
        edit.initialize(api.todo)
        assertEquals("Draft", edit.title)
        edit.initialize(api.todo.copy(id = "other", title = "Other"))
        assertEquals("Other", edit.title)
        edit.clearFields()
        edit.initialize(api.todo)
        assertEquals("Task", edit.title)

        val add = AddTodoViewModel(todos) {}
        add.title = "Draft"
        api.unauthorized = true
        add.save("expired") {}
        assertEquals("", add.title)
        assertEquals("", add.description)
        assertFalse(add.completed)

        api.unauthorized = false
        val login = LoginViewModel(auth)
        login.email = "a@example.com"
        login.password = "password"
        login.login() {}
        assertEquals("", login.email)
        assertEquals("", login.password)
    }

    @Test fun `a pending list request cannot be relabeled as another session`() {
        val gate = CompletableDeferred<Unit>()
        api.listGate = gate
        val list = TodoListViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        list.load("first", 1)
        assertTrue(list.loading)
        list.load("second", 2)
        assertEquals(1, list.loadedSession)
        gate.complete(Unit)
        assertFalse(list.loading)
        list.load("second", 2)
        assertEquals(2, list.loadedSession)
        assertEquals("Bearer second", api.authorization)
    }

    @Test fun `unauthorized todo requests expire session while login errors stay on login`() {
        api.unauthorized = true
        var expired = false
        var authenticated = false
        val login = LoginViewModel(auth)
        login.email = "a@example.com"
        login.password = "wrong"
        login.login() { authenticated = true }
        assertFalse(authenticated)
        assertEquals("Invalid email or password", login.error)
        val list = TodoListViewModel(todos) { expired = true }
        api.unauthorized = false
        list.load("previous-session", 1)
        assertEquals(listOf(api.todo), list.todos)
        api.unauthorized = true
        list.load("expired", 2)
        assertTrue(list.todos.isEmpty())
        assertTrue(expired)
        assertNull(list.error)
        assertFalse(list.loading)
    }

    private class FakeApi : AuthService, TodoService {
        var calls = 0
        var listGate: CompletableDeferred<Unit>? = null
        var unauthorized = false
        var authorization: String? = null
        var credentials: CredentialsDto? = null
        var request: TodoRequestDto? = null
        val todo = TodoDto("id", "Task", null, false, "user", "created", "updated")

        private fun check() {
            calls++
            if (unauthorized) throw HttpException(Response.error<Any>(401,
                """{"error":"Invalid email or password"}""".toResponseBody("application/json".toMediaType())))
        }
        override suspend fun login(credentials: CredentialsDto): AuthResponseDto {
            check(); this.credentials = credentials; return AuthResponseDto("login-token")
        }
        override suspend fun register(credentials: CredentialsDto): AuthResponseDto {
            check(); this.credentials = credentials; return AuthResponseDto("register-token")
        }
        override suspend fun todos(authorization: String): List<TodoDto> {
            check(); this.authorization = authorization; listGate?.await(); return listOf(todo)
        }
        override suspend fun todo(authorization: String, id: String): TodoDto {
            check(); this.authorization = authorization; assertEquals("id", id); return todo
        }
        override suspend fun create(authorization: String, todo: TodoRequestDto): TodoDto {
            check(); this.authorization = authorization; request = todo; return this.todo
        }
        override suspend fun update(authorization: String, id: String, todo: TodoRequestDto): TodoDto {
            check(); this.authorization = authorization; assertEquals("id", id); request = todo; return this.todo
        }
        override suspend fun delete(authorization: String, id: String) {
            check(); this.authorization = authorization; assertEquals("id", id)
        }
    }
}
