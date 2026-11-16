package com.example.todolistproject.viewmodels

import android.content.Context
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ScreenViewModelsTest {
    private val api = FakeApi()
    private val auth = AuthRepository(api)
    private val todos = TodoRepository(api)
    private val preferences = RuntimeEnvironment.getApplication().getSharedPreferences("test_session", Context.MODE_PRIVATE)
    private lateinit var app: AppViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        preferences.edit().clear().commit()
        app = AppViewModel(auth, todos, preferences)
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `authentication validates input and uses each endpoint`() {
        val model = AuthViewModel(app)
        var completed = false
        model.setPassword("password")
        model.login { completed = true }
        assertEquals("Enter your email and password.", model.requestState.value.error)
        model.setEmail("a@example.com")
        model.setPassword("short")
        model.register { completed = true }
        assertEquals("Registration passwords must be at least 9 characters.", model.requestState.value.error)
        assertEquals(0, api.calls)
        assertFalse(completed)
        assertNull(app.uiState.value.token)

        model.setEmail(" a@example.com ")
        model.setPassword("password")
        model.login { assertEquals("login-token", app.uiState.value.token); completed = true }
        assertTrue(completed)
        assertEquals(1, app.uiState.value.sessionVersion)
        assertEquals("a@example.com", api.credentials?.email)
        model.setEmail("a@example.com")
        model.setPassword("password123")
        model.register { assertEquals("register-token", app.uiState.value.token) }
        assertEquals(2, app.uiState.value.sessionVersion)
        assertEquals(2, api.calls)
    }

    @Test fun `logout clears the session and reports expiry until the next login attempt`() {
        val model = AuthViewModel(app)
        app.setSession("token")
        model.setEmail("draft@example.com")
        model.setPassword("draft")
        model.logout(expired = true)
        assertNull(app.uiState.value.token)
        assertEquals("Your session expired. Please log in again.", model.uiState.value.loginMessage)
        assertEquals("", model.uiState.value.email)
        assertEquals("", model.uiState.value.password)

        model.login {}
        assertNull(model.uiState.value.loginMessage)
        assertEquals("Enter your email and password.", model.requestState.value.error)

        app.setSession("new-token")
        model.logout()
        assertNull(app.uiState.value.token)
        assertNull(model.uiState.value.loginMessage)
        assertNull(model.requestState.value.error)
        assertEquals(2, app.uiState.value.sessionVersion)
    }
    @Test fun `new app instance restores the latest token and session version`() {
        app.setSession("first-token")
        val restored = AppViewModel(auth, todos, preferences)
        assertEquals("first-token", restored.uiState.value.token)
        assertEquals(1, restored.uiState.value.sessionVersion)

        restored.setSession("second-token")
        val reopened = AppViewModel(auth, todos, preferences)
        assertEquals("second-token", reopened.uiState.value.token)
        assertEquals(2, reopened.uiState.value.sessionVersion)
    }

    @Test fun `logout and expiry remove persisted credentials`() {
        for (expired in listOf(false, true)) {
            app.setSession("token")
            AuthViewModel(app).logout(expired)
            val restored = AppViewModel(auth, todos, preferences)
            assertNull(restored.uiState.value.token)
            assertEquals(app.uiState.value.sessionVersion, restored.uiState.value.sessionVersion)
        }
    }


    @Test fun `todo mutations update shared list and detail before success callbacks`() {
        val model = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        var completed = false
        model.load("token", 1)
        assertEquals(listOf(api.todo), model.uiState.value.todos)
        model.load("token", "id")
        assertEquals(api.todo, model.uiState.value.todo)
        model.setTitle("  ")
        model.save("token") { completed = true }
        assertEquals("Enter a title.", model.requestState.value.error)
        assertFalse(completed)
        model.setTitle(" Task ")
        model.save("token") { created ->
            assertEquals(listOf(api.todo, created), model.uiState.value.todos)
            completed = true
        }
        assertTrue(completed)
        assertEquals(TodoRequestDto("Task", null, false), api.request)
        assertEquals("", model.uiState.value.title)
        completed = false
        model.initialize(api.todo)
        model.setTitle("Changed")
        model.setDescription("Details")
        model.setCompleted(true)
        model.save("token", "id") { updated ->
            assertEquals(updated, model.uiState.value.todo)
            assertEquals(updated, model.uiState.value.todos.first())
            assertEquals("Changed", updated.title)
            completed = true
        }
        assertTrue(completed)
        assertEquals(TodoRequestDto("Changed", "Details", true), api.request)
        completed = false
        model.delete("token", "id") {
            assertNull(model.uiState.value.todo)
            assertFalse(model.uiState.value.todos.any { it.id == "id" })
            completed = true
        }
        assertTrue(completed)
        assertEquals("Bearer token", api.authorization)
    }

    @Test fun `form state survives initialization and clears on success or session expiry`() {
        val edit = TodoViewModel(todos) {}
        edit.initialize(api.todo)
        assertEquals("Task", edit.uiState.value.title)
        edit.setTitle("Draft")
        edit.initialize(api.todo)
        assertEquals("Draft", edit.uiState.value.title)
        edit.initialize(api.todo.copy(id = "other", title = "Other"))
        assertEquals("Other", edit.uiState.value.title)
        edit.clearFields()
        edit.initialize(api.todo)
        assertEquals("Task", edit.uiState.value.title)

        val add = edit
        add.load("token", 1)
        add.load("token", "id")
        add.setTitle("Draft")
        api.unauthorized = true
        add.save("expired") {}
        assertEquals("", add.uiState.value.title)
        assertEquals("", add.uiState.value.description)
        assertFalse(add.uiState.value.completed)
        assertTrue(add.uiState.value.todos.isEmpty())
        assertNull(add.uiState.value.todo)
        assertNull(add.uiState.value.loadedSession)
        assertFalse(add.uiState.value.confirmDelete)
        api.unauthorized = false
        val login = AuthViewModel(app)
        login.setEmail("a@example.com")
        login.setPassword("password")
        login.login() {}
        assertEquals("", login.uiState.value.email)
        assertEquals("", login.uiState.value.password)
    }

    @Test fun `new session replaces a pending list request without exposing stale data`() {
        val first = FakeApi.ListResponse(listOf(api.todo))
        val secondTodo = api.todo.copy(id = "second", title = "Second user's task", userId = "second-user")
        val second = FakeApi.ListResponse(listOf(secondTodo))
        api.listResponses["Bearer first"] = first
        api.listResponses["Bearer second"] = second
        val model = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }

        model.load("first", 1)
        assertTrue(model.requestState.value.loading)
        model.load("second", 2)
        assertTrue(first.cancelled)
        assertEquals(2, model.uiState.value.loadedSession)
        assertTrue(model.uiState.value.todos.isEmpty())
        assertEquals("Bearer second", api.authorization)
        assertTrue(model.requestState.value.loading)

        first.gate.complete(Unit)
        assertTrue(model.uiState.value.todos.isEmpty())
        assertTrue(model.requestState.value.loading)
        second.gate.complete(Unit)
        assertEquals(listOf(secondTodo), model.uiState.value.todos)
        assertFalse(model.requestState.value.loading)
        assertNull(model.requestState.value.error)
    }

    @Test fun `clearSession resets all state and cancels pending data without repopulating it`() {
        val model = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        fun populateSession() {
            model.load("first", 1)
            model.load("first", "id")
            model.initialize(api.todo)
            model.setTitle("")
            model.save("first") { throw AssertionError("Invalid form must not save") }
            model.setTitle("Private draft")
            model.setDescription("Private details")
            model.setCompleted(true)
            model.setConfirmDelete(true)
        }

        populateSession()
        assertEquals(listOf(api.todo), model.uiState.value.todos)
        assertEquals(api.todo, model.uiState.value.todo)
        assertEquals("Enter a title.", model.requestState.value.error)
        model.clearSession()
        assertEquals(TodoUiState(), model.uiState.value)
        assertEquals(RequestUiState(), model.requestState.value)

        populateSession()
        val pending = FakeApi.ListResponse(listOf(api.todo))
        api.listResponses["Bearer first"] = pending
        model.load("first", 1)
        assertTrue(model.requestState.value.loading)
        assertEquals("Private draft", model.uiState.value.title)
        assertEquals("Private details", model.uiState.value.description)
        assertTrue(model.uiState.value.completed)
        assertTrue(model.uiState.value.confirmDelete)
        model.clearSession()
        assertTrue(pending.cancelled)
        assertEquals(TodoUiState(), model.uiState.value)
        assertEquals(RequestUiState(), model.requestState.value)

        pending.gate.complete(Unit)
        assertEquals(TodoUiState(), model.uiState.value)
        assertEquals(RequestUiState(), model.requestState.value)

        model.initialize(api.todo)
        assertEquals("Task", model.uiState.value.title)
    }

    @Test fun `collectors observe pending list request and completion`() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.listGate = gate
        val model = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        val loadingStates = mutableListOf<Boolean>()
        val listStates = mutableListOf<List<TodoDto>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.requestState.map { it.loading }.distinctUntilChanged().collect { loadingStates.add(it) }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.uiState.map { it.todos }.distinctUntilChanged().collect { listStates.add(it) }
        }

        model.load("token", 1)
        assertEquals(listOf(false, true), loadingStates)
        assertEquals(listOf(emptyList<TodoDto>()), listStates)

        gate.complete(Unit)
        assertEquals(listOf(false, true, false), loadingStates)
        assertEquals(listOf(emptyList<TodoDto>(), listOf(api.todo)), listStates)
    }

    @Test fun `unauthorized todo requests expire session while login errors stay on login`() {
        api.unauthorized = true
        var authenticated = false
        val login = AuthViewModel(app)
        login.setEmail("a@example.com")
        login.setPassword("wrong")
        login.login() { authenticated = true }
        assertFalse(authenticated)
        assertEquals("Invalid email or password", login.requestState.value.error)
        app.setSession("previous-session")
        val list = TodoViewModel(todos) { login.logout(expired = true) }
        api.unauthorized = false
        list.load("previous-session", 1)
        assertEquals(listOf(api.todo), list.uiState.value.todos)
        api.unauthorized = true
        list.load("expired", 2)
        assertTrue(list.uiState.value.todos.isEmpty())
        assertNull(app.uiState.value.token)
        assertEquals("Your session expired. Please log in again.", login.uiState.value.loginMessage)
        assertNull(list.requestState.value.error)
        assertFalse(list.requestState.value.loading)
    }

    private class FakeApi : AuthService, TodoService {
        var calls = 0
        var listGate: CompletableDeferred<Unit>? = null
        val listResponses = mutableMapOf<String, ListResponse>()
        class ListResponse(val todos: List<TodoDto>) {
            val gate = CompletableDeferred<Unit>()
            var cancelled = false
        }
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
            check()
            this.authorization = authorization
            val response = listResponses[authorization]
            if (response != null) {
                try {
                    response.gate.await()
                } catch (cancelled: CancellationException) {
                    response.cancelled = true
                    throw cancelled
                }
                return response.todos
            }
            listGate?.await()
            return listOf(todo)
        }
        override suspend fun todo(authorization: String, id: String): TodoDto {
            check(); this.authorization = authorization; assertEquals("id", id); return todo
        }
        override suspend fun create(authorization: String, todo: TodoRequestDto): TodoDto {
            check(); this.authorization = authorization; request = todo
            return this.todo.copy(id = "created", title = todo.title, description = todo.description, completed = todo.completed)
        }
        override suspend fun update(authorization: String, id: String, todo: TodoRequestDto): TodoDto {
            check(); this.authorization = authorization; assertEquals("id", id); request = todo
            return this.todo.copy(title = todo.title, description = todo.description, completed = todo.completed)
        }
        override suspend fun delete(authorization: String, id: String) {
            check(); this.authorization = authorization; assertEquals("id", id)
        }
    }
}
