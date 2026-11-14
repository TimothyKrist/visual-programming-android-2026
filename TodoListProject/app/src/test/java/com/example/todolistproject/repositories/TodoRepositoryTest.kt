package com.example.todolistproject.repositories

import com.example.todolistproject.models.TodoRequestDto
import com.example.todolistproject.repositories.AuthRepository
import com.example.todolistproject.repositories.TodoRepository
import com.example.todolistproject.services.AuthService
import com.example.todolistproject.services.TodoService
import com.example.todolistproject.services.createRetrofit
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class TodoRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: TodoRepository
    private lateinit var authRepository: AuthRepository
    private val todoJson = """{"id":"123","title":"Task","description":null,"completed":false,"userId":"456","createdAt":"2026-01-01","updatedAt":"2026-01-01"}"""

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = createRetrofit(server.url("/").toString())
        repository = TodoRepository(retrofit.create(TodoService::class.java))
        authRepository = AuthRepository(retrofit.create(AuthService::class.java))
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `authentication uses the backend endpoints and credentials`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"accessToken":"token"}"""))
        assertEquals("token", authRepository.register(" user@example.com ", "password123").accessToken)
        val register = server.takeRequest()
        assertEquals("POST", register.method)
        assertEquals("/auth/register", register.path)
        assertEquals("user@example.com", JsonParser.parseString(register.body.readUtf8()).asJsonObject["email"].asString)

        server.enqueue(MockResponse().setBody("""{"accessToken":"token"}"""))
        authRepository.login("user@example.com", "password123")
        assertEquals("/auth/login", server.takeRequest().path)
    }

    @Test fun `todo operations send bearer token and correct requests`() = runBlocking {
        server.enqueue(MockResponse().setBody("[$todoJson]"))
        assertEquals("Task", repository.todos("token").single().title)
        val list = server.takeRequest()
        assertEquals("GET", list.method)
        assertEquals("/todos", list.path)
        assertEquals("Bearer token", list.getHeader("Authorization"))

        server.enqueue(MockResponse().setBody(todoJson))
        repository.todo("token", "123")
        val detail = server.takeRequest()
        assertEquals("GET", detail.method)
        assertEquals("/todos/123", detail.path)
        assertEquals("Bearer token", detail.getHeader("Authorization"))

        val input = TodoRequestDto("Task", null, true)
        server.enqueue(MockResponse().setResponseCode(201).setBody(todoJson))
        repository.create("token", input)
        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/todos", create.path)
        assertEquals("Bearer token", create.getHeader("Authorization"))
        val body = JsonParser.parseString(create.body.readUtf8()).asJsonObject
        assertTrue(body["description"].isJsonNull)
        assertTrue(body["completed"].asBoolean)

        server.enqueue(MockResponse().setBody(todoJson))
        repository.update("token", "123", input)
        val update = server.takeRequest()
        assertEquals("PATCH", update.method)
        assertEquals("/todos/123", update.path)
        assertEquals("Bearer token", update.getHeader("Authorization"))
        assertTrue(JsonParser.parseString(update.body.readUtf8()).asJsonObject["description"].isJsonNull)

        server.enqueue(MockResponse().setResponseCode(204))
        repository.delete("token", "123")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/todos/123", delete.path)
        assertEquals("Bearer token", delete.getHeader("Authorization"))
    }

    @Test fun `backend errors reach the caller`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Invalid email or password"}"""))
        try {
            authRepository.login("user@example.com", "wrong")
            throw AssertionError("Expected an HTTP error")
        } catch (error: HttpException) {
            assertEquals(401, error.code())
        }
    }
}
