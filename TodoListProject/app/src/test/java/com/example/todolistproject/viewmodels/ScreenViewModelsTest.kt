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
        assertEquals("Enter your email and password.", model.error.value)
        model.setEmail("a@example.com")
        model.setPassword("short")
        model.register { completed = true }
        assertEquals("Registration passwords must be at least 9 characters.", model.error.value)
        assertEquals(0, api.calls)
        assertFalse(completed)
        assertNull(app.token.value)

        model.setEmail(" a@example.com ")
        model.setPassword("password")
        model.login { assertEquals("login-token", app.token.value); completed = true }
        assertTrue(completed)
        assertEquals(1, app.sessionVersion.value)
        assertEquals("a@example.com", api.credentials?.email)
        model.setEmail("a@example.com")
        model.setPassword("password123")
        model.register { assertEquals("register-token", app.token.value) }
        assertEquals(2, app.sessionVersion.value)
        assertEquals(2, api.calls)
    }

    @Test fun `logout clears the session and reports expiry until the next login attempt`() {
        val model = AuthViewModel(app)
        app.setSession("token")
        model.setEmail("draft@example.com")
        model.setPassword("draft")
        model.logout(expired = true)
        assertNull(app.token.value)
        assertEquals("Your session expired. Please log in again.", model.loginMessage.value)
        assertEquals("", model.email.value)
        assertEquals("", model.password.value)

        model.login {}
        assertNull(model.loginMessage.value)
        assertEquals("Enter your email and password.", model.error.value)

        app.setSession("new-token")
        model.logout()
        assertNull(app.token.value)
        assertNull(model.loginMessage.value)
        assertNull(model.error.value)
        assertEquals(2, app.sessionVersion.value)
    }
    @Test fun `new app instance restores the latest token and session version`() {
        app.setSession("first-token")
        val restored = AppViewModel(auth, todos, preferences)
        assertEquals("first-token", restored.token.value)
        assertEquals(1, restored.sessionVersion.value)

        restored.setSession("second-token")
        val reopened = AppViewModel(auth, todos, preferences)
        assertEquals("second-token", reopened.token.value)
        assertEquals(2, reopened.sessionVersion.value)
    }

    @Test fun `logout and expiry remove persisted credentials`() {
        for (expired in listOf(false, true)) {
            app.setSession("token")
            AuthViewModel(app).logout(expired)
            val restored = AppViewModel(auth, todos, preferences)
            assertNull(restored.token.value)
            assertEquals(app.sessionVersion.value, restored.sessionVersion.value)
        }
    }


    @Test fun `todo mutations update shared list and detail before success callbacks`() {
        val model = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        var completed = false
        model.load("token", 1)
        assertEquals(listOf(api.todo), model.todos.value)
        model.load("token", "id")
        assertEquals(api.todo, model.todo.value)
        model.setTitle("  ")
        model.save("token") { completed = true }
        assertEquals("Enter a title.", model.error.value)
        assertFalse(completed)
        model.setTitle(" Task ")
        model.save("token") { created ->
            assertEquals(listOf(api.todo, created), model.todos.value)
            completed = true
        }
        assertTrue(completed)
        assertEquals(TodoRequestDto("Task", null, false), api.request)
        assertEquals("", model.title.value)
        completed = false
        model.initialize(api.todo)
        model.setTitle("Changed")
        model.setDescription("Details")
        model.setCompleted(true)
        model.save("token", "id") { updated ->
            assertEquals(updated, model.todo.value)
            assertEquals(updated, model.todos.value.first())
            assertEquals("Changed", updated.title)
            completed = true
        }
        assertTrue(completed)
        assertEquals(TodoRequestDto("Changed", "Details", true), api.request)
        completed = false
        model.delete("token", "id") {
            assertNull(model.todo.value)
            assertFalse(model.todos.value.any { it.id == "id" })
            completed = true
        }
        assertTrue(completed)
        assertEquals("Bearer token", api.authorization)
    }

    @Test fun `form state survives initialization and clears on success or session expiry`() {
        val edit = TodoViewModel(todos) {}
        edit.initialize(api.todo)
        assertEquals("Task", edit.title.value)
        edit.setTitle("Draft")
        edit.initialize(api.todo)
        assertEquals("Draft", edit.title.value)
        edit.initialize(api.todo.copy(id = "other", title = "Other"))
        assertEquals("Other", edit.title.value)
        edit.clearFields()
        edit.initialize(api.todo)
        assertEquals("Task", edit.title.value)

        val add = edit
        add.load("token", 1)
        add.load("token", "id")
        add.setTitle("Draft")
        api.unauthorized = true
        add.save("expired") {}
        assertEquals("", add.title.value)
        assertEquals("", add.description.value)
        assertFalse(add.completed.value)
        assertTrue(add.todos.value.isEmpty())
        assertNull(add.todo.value)
        assertNull(add.loadedSession.value)
        assertFalse(add.confirmDelete.value)
        api.unauthorized = false
        val login = AuthViewModel(app)
        login.setEmail("a@example.com")
        login.setPassword("password")
        login.login() {}
        assertEquals("", login.email.value)
        assertEquals("", login.password.value)
    }

    @Test fun `a pending list request cannot be relabeled as another session`() {
        val gate = CompletableDeferred<Unit>()
        api.listGate = gate
        val list = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        list.load("first", 1)
        assertTrue(list.loading.value)
        list.load("second", 2)
        assertEquals(1, list.loadedSession.value)
        gate.complete(Unit)
        assertFalse(list.loading.value)
        list.load("second", 2)
        assertEquals(2, list.loadedSession.value)
        assertEquals("Bearer second", api.authorization)
    }

    @Test fun `collectors observe pending list request and completion`() = runTest {
        val gate = CompletableDeferred<Unit>()
        api.listGate = gate
        val model = TodoViewModel(todos) { throw AssertionError("Unexpected session expiry") }
        val loadingStates = mutableListOf<Boolean>()
        val listStates = mutableListOf<List<TodoDto>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.loading.collect { loadingStates.add(it) }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            model.todos.collect { listStates.add(it) }
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
        assertEquals("Invalid email or password", login.error.value)
        app.setSession("previous-session")
        val list = TodoViewModel(todos) { login.logout(expired = true) }
        api.unauthorized = false
        list.load("previous-session", 1)
        assertEquals(listOf(api.todo), list.todos.value)
        api.unauthorized = true
        list.load("expired", 2)
        assertTrue(list.todos.value.isEmpty())
        assertNull(app.token.value)
        assertEquals("Your session expired. Please log in again.", login.loginMessage.value)
        assertNull(list.error.value)
        assertFalse(list.loading.value)
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
